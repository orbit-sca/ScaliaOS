package com.scaliaos.app.services.executors

import com.scaliaos.app.models.AgentModels._
import com.scaliaos.app.models.AgentType
import com.scaliaos.app.bridge.JuliaBridge
import zio._
import zio.json._

/**
 * Executor implementation for Large Language Model (LLM) agents.
 * 
 * This executor handles agents that primarily interact with language models for:
 * - Natural language chat and conversation
 * - Text generation and completion
 * - Reasoning and analysis tasks
 * - Code generation and explanation
 * - Question answering
 * 
 * Architecture:
 * The executor acts as a bridge between the HTTP API and Julia-based LLM agents,
 * handling:
 * - Input validation and parsing
 * - Julia FFI communication via JuliaBridge
 * - Response formatting and error handling
 * - Execution timing and timeout management
 * 
 * The actual LLM logic (API calls to OpenAI/Anthropic/etc.) is implemented
 * in Julia for performance and easier integration with ML libraries.
 * 
 * Version: v0.2 - Uses executeAgent method with structured JSON I/O
 */
class LLMAgentExecutor extends AgentExecutor {
  
  /**
   * Identifies this executor as handling LLM-type agents.
   * 
   * @return AgentType.LLM
   */
  override def getAgentType: AgentType = AgentType.LLM
  
  /**
   * Validates that the request contains a properly formatted message for the LLM.
   * 
   * Requirements:
   * - Input must be a Map[String, String]
   * - The map should contain a "message" key with the text to send to the LLM
   * 
   * @param request The execution request to validate
   * @return Success if validation passes
   * @throws Exception if input is not in the expected Map format or missing required fields
   */
  override def validate(request: AgentExecutionRequest): Task[Unit] = {
    ZIO.attempt {
      val inputMap = request.input.as[Map[String, String]]
      require(inputMap.isRight, "LLM agent requires message in input")
    }
  }
  
  /**
   * Executes an LLM agent task by calling into Julia-based agent logic.
   * 
   * Execution flow:
   * 1. Records start time for performance metrics
   * 2. Parses and validates input to extract the message
   * 3. Calls Julia agent via JuliaBridge with the message and agent ID
   * 4. Waits for response with 90-second timeout
   * 5. Returns structured response with LLM output and metadata
   * 
   * The Julia agent handles:
   * - API calls to LLM providers (OpenAI, Anthropic, etc.)
   * - Prompt engineering and context management
   * - Response parsing and formatting
   * - Error handling for API failures
   * 
   * Timeout protection:
   * The 90-second timeout prevents hanging on slow LLM API calls or
   * network issues. This is appropriate for complex reasoning tasks but
   * may need adjustment for simpler queries.
   * 
   * @param request The execution request containing agent ID, message, and configuration
   * @return An AgentExecutionResponse containing:
   *         - output: The LLM's response (typically includes "reply" key)
   *         - executionTime: Total duration including Julia call and LLM API time
   *         - confidence: LLM's confidence score (if available)
   *         - reasoning: Explanation of the LLM's reasoning process
   *         - warnings: Any issues encountered (rate limits, token limits, etc.)
   * @throws Exception if input parsing fails, Julia call times out, or LLM API errors occur
   */
  override def execute(request: AgentExecutionRequest): Task[AgentExecutionResponse] = {
    for {
      // Capture start time for execution metrics
      startTime <- Clock.currentTime(java.util.concurrent.TimeUnit.MILLISECONDS)
      
      // Parse input into expected Map format
      inputData <- ZIO.fromEither(
        request.input.as[Map[String, String]]
      ).mapError(e => new Exception(s"Invalid input: $e"))
      
      // Extract the message to send to the LLM
      // Falls back to empty string if "message" key is missing
      message = inputData.getOrElse("message", "")
      
      // Call Julia agent to process the message with the LLM
      // Uses v0.2 executeAgent method which handles JSON encoding/decoding
      response <- callJuliaLLMAgent(message, request.agentId)
      
      // Capture end time to calculate total execution duration
      endTime <- Clock.currentTime(java.util.concurrent.TimeUnit.MILLISECONDS)
      
    } yield AgentExecutionResponse(
      agentId = request.agentId,
      output = response.output,  // LLM response data (typically contains "reply" key)
      blockchainRequests = response.blockchainRequests,  // Usually empty for pure LLM agents
      submittedTransactions = List.empty,  // LLM agents don't submit transactions
      executionTime = endTime - startTime,
      confidence = response.confidence,
      reasoning = response.reasoning,
      warnings = response.warnings
    )
  }
  
  /**
   * Invokes the Julia-based LLM agent to process a message.
   * 
   * This method bridges between Scala and Julia using JuliaBridge, which handles:
   * - JSON serialization of input parameters
   * - Julia function invocation via JNI or process communication
   * - JSON deserialization of Julia's response
   * - Error propagation from Julia to Scala
   * 
   * Implementation details (v0.2):
   * - Uses "basic_agent" as the Julia agent identifier
   * - Passes structured input with "ability", "prompt", and "message" fields
   * - "ability" -> "llm_chat" indicates this is a chat/generation task
   * - Both "prompt" and "message" are provided for backward compatibility
   * 
   * Timeout: 90 seconds
   * - Accounts for LLM API latency (typically 1-30 seconds)
   * - Includes time for Julia processing and response formatting
   * - Prevents indefinite hangs on network issues or API outages
   * 
   * Logging:
   * - Logs before Julia call with input details (for debugging)
   * - Logs after successful response (for monitoring)
   * - Allows tracking of slow requests and failures
   * 
   * @param message The user's message/prompt to send to the LLM
   * @param agentId The agent ID (currently unused in Julia, but logged for tracing)
   * @return A JuliaAgentResponse containing the LLM's output and metadata
   * @throws Exception if Julia call fails, times out, or returns an error
   */
  private def callJuliaLLMAgent(message: String, agentId: String): Task[JuliaAgentResponse] = {
    // Prepare input as Map[String, Any] for v0.2 executeAgent method
    // The Julia agent expects this specific structure
    val input = Map[String, Any](
      "ability" -> "llm_chat",  // Specifies the LLM capability to use
      "prompt" -> message,       // The actual message/prompt
      "message" -> message       // Duplicate for backward compatibility with older Julia code
    )
    
    for {
      // Log the invocation for debugging and monitoring
      _ <- ZIO.logInfo(s"[LLM] Calling Julia agent: basic_agent")
      _ <- ZIO.logInfo(s"[LLM] Input: $input")
      
      // Call Julia via JuliaBridge with structured input
      // The bridge handles JSON encoding/decoding internally (v0.2 improvement)
      response <- JuliaBridge.executeAgent("basic_agent", input)
        .tap(r => ZIO.logInfo(s"[LLM] Julia response received: ${r.output}"))
        .timeout(Duration.fromSeconds(90))  // Prevent indefinite hangs
        .someOrFail(new Exception("Julia agent execution timed out after 90 seconds"))
      
      _ <- ZIO.logInfo(s"[LLM] Successfully executed Julia agent")
      
    } yield response
  }
}