package com.scaliaos.app.bridge

import zio._
import zio.json._
import com.scaliaos.app.models.AgentModels._
import java.io.{BufferedReader, InputStreamReader}
import java.util.Base64

/**
 * JuliaBridge provides a functional bridge between Scala (ZIO backend)
 * and Julia-based agents running within the JuliaOS framework.
 *
 * Communication uses Base64-encoded JSON passed via command-line arguments
 * to ensure consistent cross-platform behavior, especially on Windows.
 *
 * The bridge manages:
 *  - Agent execution (calling Julia scripts)
 *  - Argument encoding/decoding
 *  - Output parsing into structured responses
 *  - Error propagation into ZIO Task layers
 *
 * Designed for Julia agents that read input from `ARGS`
 * using the `input=<base64>` convention.
 */
object JuliaBridge {

  // ======================================================================
  //  AGENT EXECUTION (v0.2)
  // ======================================================================

  /**
   * Executes a Julia agent by launching its corresponding script
   * and passing Base64-encoded JSON input as a command-line argument.
   *
   * This is the primary entry point for agent interaction in ScaliaOS.
   *
   * @param agentId   Identifier of the Julia agent (maps to a script file)
   * @param input     Input parameters as a Scala Map[String, Any]
   * @return          Parsed JuliaAgentResponse wrapped in a ZIO Task
   */
  def executeAgent(agentId: String, input: Map[String, Any]): Task[JuliaAgentResponse] = {
    for {
      // Resolve paths and configuration
      juliaPath   <- getJuliaPath
      agentScript <- getAgentScript(agentId)

      // Serialize input map to JSON
      inputJson = buildJsonString(input)

      // Encode input as Base64 to avoid quote escaping issues (esp. Windows)
      encodedInput = Base64.getEncoder.encodeToString(inputJson.getBytes("UTF-8"))

      // Log execution metadata
      _ <- ZIO.logInfo(s"Executing Julia agent: $agentId")
      _ <- ZIO.logDebug(s"Base64 input: $encodedInput")

      // Execute the Julia process
      output   <- runJuliaProcessV2(juliaPath, agentScript, encodedInput)

      // Parse and validate response
      response <- parseJuliaResponse(output)

      // Check for agent-side errors
      _ <- ZIO.when(response.error.getOrElse(false)) {
        ZIO.fail(
          AgentError.ExecutionFailed(
            agentId,
            response.message.getOrElse("Unknown error in Julia agent")
          )
        )
      }

      _ <- ZIO.logInfo(s"Agent $agentId executed successfully")
    } yield response
  }

  // ======================================================================
  //  INTERNAL HELPERS
  // ======================================================================

  /**
   * Converts a nested Scala Map[String, Any] into a valid JSON string.
   * Handles strings, numbers, booleans, sequences, and nested maps.
   *
   * Used to serialize agent input for Base64 encoding.
   */
  private def buildJsonString(map: Map[String, Any]): String = {

    def valueToJson(value: Any): String = value match {
      case s: String => s""""${escapeString(s)}""""
      case n: Number => n.toString
      case b: Boolean => b.toString
      case m: Map[_, _] => buildJsonString(m.asInstanceOf[Map[String, Any]])
      case seq: Seq[_] => seq.map(valueToJson).mkString("[", ",", "]")
      case null => "null"
      case other => s""""${escapeString(other.toString)}""""
    }

    def escapeString(str: String): String =
      str.replace("\\", "\\\\")
         .replace("\"", "\\\"")
         .replace("\n", "\\n")
         .replace("\r", "")

    map.map { case (k, v) => s""""$k":${valueToJson(v)}""" }
       .mkString("{", ",", "}")
  }

  /**
   * Launches a Julia process using command-line arguments.
   * Passes encoded JSON as `input=<base64>` to the target agent script.
   *
   * @param juliaPath  Path to the Julia executable
   * @param scriptPath Path to the Julia script (.jl)
   * @param inputJson  Base64-encoded JSON input
   * @return           Raw stdout output as a String
   */
  private def runJuliaProcessV2(
    juliaPath: String,
    scriptPath: String,
    inputJson: String
  ): Task[String] = {
    ZIO.attemptBlocking {

      println(s"[DEBUG] Launching Julia process:")
      println(s"[DEBUG] juliaPath  = $juliaPath")
      println(s"[DEBUG] scriptPath = $scriptPath")
      println(s"[DEBUG] inputJson  = $inputJson")

      val processBuilder = new ProcessBuilder()
      processBuilder.command(
        juliaPath,
        "--project=julia",
        scriptPath,
        s"input=$inputJson"
      )

      println(s"[DEBUG] Command: ${processBuilder.command()}")
      processBuilder.directory(new java.io.File("."))

      val process = processBuilder.start()

      // Concurrently read stdout and stderr streams
      import scala.concurrent.{Future, ExecutionContext}
      import scala.concurrent.ExecutionContext.Implicits.global

      val stdoutFuture = Future {
        val reader = new BufferedReader(new InputStreamReader(process.getInputStream))
        val lines = scala.collection.mutable.ArrayBuffer[String]()
        var line: String = null
        while ({ line = reader.readLine(); line != null }) {
          println(s"[JULIA STDOUT] $line")
          lines += line
        }
        lines.mkString("\n")
      }

      val stderrFuture = Future {
        val reader = new BufferedReader(new InputStreamReader(process.getErrorStream))
        var line: String = null
        while ({ line = reader.readLine(); line != null }) {
          println(s"[JULIA STDERR] $line")
        }
      }

      // Wait for process completion
      val exitCode = process.waitFor()

      // Await outputs
      import scala.concurrent.Await
      import scala.concurrent.duration._
      val output = Await.result(stdoutFuture, 120.seconds)
      Await.result(stderrFuture, 5.seconds)

      if (exitCode != 0) {
        throw new RuntimeException(s"Julia process failed with exit code $exitCode")
      }

      // Extract final JSON line from Julia output
      val jsonLine = output.split("\n").reverse.find(line =>
        line.trim.startsWith("{") && line.contains("output")
      ).getOrElse(output)

      jsonLine
    }
  }

  /**
   * Parses a JSON string produced by a Julia agent
   * into a strongly typed `JuliaAgentResponse`.
   *
   * @param output Raw JSON output from the Julia process
   * @return       Parsed JuliaAgentResponse as a ZIO Task
   */
  private def parseJuliaResponse(output: String): Task[JuliaAgentResponse] =
    ZIO.fromEither(output.fromJson[JuliaAgentResponse])
      .mapError(error =>
        AgentError.ExecutionFailed(
          "unknown",
          s"Failed to parse Julia response: $error\nOutput: $output"
        )
      )

  /**
   * Retrieves the Julia executable path from environment variables,
   * or defaults to "julia" (assuming it is on the system PATH).
   */
  private def getJuliaPath: Task[String] =
    ZIO.succeed(sys.env.getOrElse("JULIA_PATH", "julia"))

  /**
   * Maps a given agent ID to its corresponding Julia script file.
   * Returns an error if the expected file does not exist.
   */
  def getAgentScript(agentId: String): Task[String] = {
    val scriptPath = agentId match {
      case "trading_agent"  => "julia/examples/trading_agent.jl"
      case "basic_agent"    => "julia/examples/basic_agent.jl"
      case "llm-chat-gpt4"  => "julia/examples/basic_agent.jl"
      case "plan_execute"   => "julia/examples/plan_execute_agent.jl"
      case "streaming_chat" => "julia/examples/streaming_chat_agent.jl"
      case _                => s"julia/examples/${agentId}.jl"
    }

    val file = new java.io.File(scriptPath)
    if (file.exists()) ZIO.succeed(scriptPath)
    else ZIO.fail(AgentError.NotFound(agentId))
  }

  // ======================================================================
  //  TESTING & DEBUG UTILITIES
  // ======================================================================

  /**
   * Test utility to verify Julia bridge connectivity.
   * Executes a trading agent with mock data and prints the response.
   */
  val testAgentExecution: ZIO[Any, Throwable, Unit] = {
    val testInput = Map(
      "token"  -> "ETH",
      "action" -> "analyze",
      "price"  -> "1800"
    )

    executeAgent("trading_agent", testInput)
      .flatMap { response =>
        Console.printLine("✅ Agent test successful!") *>
        Console.printLine(s"Output: ${response.output}") *>
        Console.printLine(s"Blockchain requests: ${response.blockchainRequests.length}")
      }
  }
}
