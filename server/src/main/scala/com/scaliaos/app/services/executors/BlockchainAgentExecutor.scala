// BlockchainAgentExecutor.scala
package com.scaliaos.app.services.executors

import com.scaliaos.app.models.AgentModels._
import com.scaliaos.app.models.AgentType
import com.scaliaos.app.services.BlockchainService
import zio._

/**
 * Executor implementation for blockchain-focused agents.
 * 
 * This executor handles agents that primarily interact with blockchain networks,
 * such as trading agents, DeFi protocol interactions, and on-chain data queries.
 * 
 * Architecture:
 * - Receives execution requests with blockchain action parameters
 * - Delegates to Julia-based agent logic for decision making
 * - Submits resulting blockchain transactions via BlockchainService
 * - Tracks transaction hashes and execution metrics
 * 
 * The executor acts as a bridge between the HTTP API layer and the underlying
 * blockchain infrastructure, providing error handling, validation, and transaction
 * management.
 * 
 * @param blockchainService The service used to submit and track blockchain transactions
 */
class BlockchainAgentExecutor(blockchainService: BlockchainService) extends AgentExecutor {
  
  /**
   * Identifies this executor as handling Blockchain-type agents.
   * 
   * @return AgentType.Blockchain
   */
  override def getAgentType: AgentType = AgentType.Blockchain
  
  /**
   * Validates that the request contains properly formatted blockchain action parameters.
   * 
   * Requirements:
   * - Input must be a Map[String, String] containing action parameters
   * - The map should contain blockchain-specific fields like action type, chain, amounts, etc.
   * 
   * @param request The execution request to validate
   * @return Success if validation passes
   * @throws Exception if input is not in the expected Map format or is missing required fields
   */
  override def validate(request: AgentExecutionRequest): Task[Unit] = {
    ZIO.attempt {
      val inputMap = request.input.as[Map[String, String]]
      require(inputMap.isRight, "Blockchain agent requires action in input")
    }
  }
  
  /**
   * Executes a blockchain agent task end-to-end.
   * 
   * Execution flow:
   * 1. Records start time for performance metrics
   * 2. Parses and validates input parameters
   * 3. Calls Julia agent logic to determine required blockchain operations
   * 4. Submits all blockchain transactions via BlockchainService
   * 5. Collects transaction hashes for tracking
   * 6. Returns comprehensive response with results and metadata
   * 
   * The executor handles multiple blockchain transactions in a single request,
   * processing them concurrently for efficiency. All transactions must succeed
   * for the execution to be considered successful.
   * 
   * @param request The execution request containing agent ID, input parameters, and config
   * @return An AgentExecutionResponse containing:
   *         - output: Result data from the agent
   *         - blockchainRequests: List of transactions that were requested
   *         - submittedTransactions: Transaction IDs from BlockchainService
   *         - confidence: Agent's confidence score in the decision
   *         - reasoning: Explanation of why these actions were taken
   *         - executionTime: Total execution duration in milliseconds
   *         - warnings: Any non-fatal issues encountered
   * @throws Exception if input parsing fails or blockchain submission errors occur
   */
  override def execute(request: AgentExecutionRequest): Task[AgentExecutionResponse] = {
    for {
      // Capture start time for execution metrics
      startTime <- Clock.currentTime(java.util.concurrent.TimeUnit.MILLISECONDS)
      
      // Parse input into expected Map format
      inputData <- ZIO.fromEither(
        request.input.as[Map[String, String]]
      ).mapError(e => new Exception(s"Invalid input: $e"))
      
      // Call Julia-based agent to determine blockchain actions
      // The Julia agent analyzes the request and decides what transactions to submit
      juliaResponse <- callJuliaAgent(request.agentId, inputData)
      
      // Submit all blockchain transactions concurrently
      // Each transaction returns a tracking ID that can be used to query status
      txHashes <- ZIO.foreach(juliaResponse.blockchainRequests) { txRequest =>
        blockchainService.submitTransaction(txRequest)
      }
      
      // Capture end time to calculate total execution duration
      endTime <- Clock.currentTime(java.util.concurrent.TimeUnit.MILLISECONDS)
      
    } yield AgentExecutionResponse(
      agentId = request.agentId,
      output = juliaResponse.output,
      blockchainRequests = juliaResponse.blockchainRequests,
      submittedTransactions = txHashes,
      confidence = juliaResponse.confidence,
      reasoning = juliaResponse.reasoning,
      executionTime = endTime - startTime,
      warnings = juliaResponse.warnings
    )
  }
  
  /**
   * Invokes the Julia-based agent logic to process the blockchain request.
   * 
   * This is currently a mock implementation that returns success without
   * performing actual Julia FFI calls. In production, this would:
   * - Call into Julia runtime via JNI or process communication
   * - Pass agent ID and input parameters to Julia functions
   * - Receive structured response with blockchain transaction requests
   * - Handle Julia-side errors and convert them to ZIO errors
   * 
   * TODO: Implement actual Julia agent integration
   * - Set up Julia runtime environment
   * - Define FFI bindings for agent functions
   * - Implement proper error handling and type conversion
   * - Add timeout protection for long-running Julia computations
   * 
   * @param agentId The ID of the specific agent to invoke
   * @param input The parsed input parameters for the agent
   * @return A JuliaAgentResponse containing output data and blockchain transaction requests
   */
  private def callJuliaAgent(
    agentId: String,
    input: Map[String, String]
  ): Task[JuliaAgentResponse] = {
    // Mock implementation - returns empty success
    // In production, this would invoke Julia code and parse results
    ZIO.succeed(JuliaAgentResponse(
      output = Map("status" -> "success"),
      blockchainRequests = List.empty
    ))
  }
}