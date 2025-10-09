package com.scaliaos.app.models

import zio.json._
import sttp.tapir.json.zio._
import zio.json.ast.Json
import BlockchainModels._

/**
 * Domain models for agent execution in the ScaliaOS platform.
 * 
 * This object contains all data models related to agent operations, including:
 * - Execution requests and responses (v0.2 API)
 * - Julia FFI communication models
 * - Agent metadata and configuration
 * - Error types for agent operations
 * 
 * All models include ZIO JSON encoders/decoders for serialization and
 * are designed to work with Tapir for HTTP endpoint definitions.
 * 
 * Version: v0.2 - Uses structured JSON input/output instead of string-based I/O
 */
object AgentModels { 

  /* ==================== Legacy LLM Julia Response Models (v0.1) ====================
   * 
   * These models are kept for reference but are no longer actively used.
   * The v0.2 architecture uses JuliaAgentResponse instead.
   * 
   * Previous structure:
   * - JuliaOuterResponse: Top-level wrapper with agent_id, task, status
   * - JuliaInnerResult: Contained the actual answer and success flag
   * - LLMResult: Simplified result format
   * 
   * Deprecated in favor of more flexible v0.2 models below.
  
  case class JuliaOuterResponse(
    agent_id: String,
    task: String,
    status: String,
    result: JuliaInnerResult
  )

  case class JuliaInnerResult(
    answer: String,
    success: Boolean
  )

  object JuliaOuterResponse {
    implicit val innerDecoder: JsonDecoder[JuliaInnerResult] = DeriveJsonDecoder.gen[JuliaInnerResult]
    implicit val innerEncoder: JsonEncoder[JuliaInnerResult] = DeriveJsonEncoder.gen[JuliaInnerResult]
    implicit val decoder: JsonDecoder[JuliaOuterResponse] = DeriveJsonDecoder.gen[JuliaOuterResponse]
    implicit val encoder: JsonEncoder[JuliaOuterResponse] = DeriveJsonEncoder.gen[JuliaOuterResponse]
  }

  case class LLMResult(
    answer: String,
    status: String,
    success: Boolean
  )

   ==================== v0.2 Models ====================  */

  /**
   * Request to execute an agent (v0.2 API).
   * 
   * This is the primary request model for agent execution, providing a flexible
   * structure that supports various agent types (LLM, Blockchain, Hybrid).
   * 
   * @param agentId The unique identifier of the agent to execute (e.g., "llm-chat-gpt4")
   * @param input Flexible JSON input containing agent-specific parameters.
   *              - For LLM agents: typically contains "message" field
   *              - For Blockchain agents: contains "action", "chain", "amount", etc.
   *              - For Hybrid agents: combines both LLM and blockchain parameters
   * @param sessionId Optional session identifier for maintaining conversation context
   *                  across multiple requests (useful for chat agents)
   * @param context Optional metadata providing additional context about the request
   *                (e.g., user preferences, environment info, request source)
   * @param options Optional execution parameters to customize agent behavior
   *                (streaming, token limits, temperature for LLMs)
   */
  case class AgentExecutionRequest(
    agentId: String,
    input: Json,
    sessionId: Option[String] = None,
    context: Option[Map[String, String]] = None,
    options: Option[AgentOptions] = None
  )

  /**
   * Optional parameters for customizing agent execution behavior.
   * 
   * @param stream If true, enable streaming responses (for LLM agents that support it).
   *               Streaming allows progressive output as the agent generates results.
   * @param maxTokens Maximum number of tokens to generate (LLM agents only).
   *                  Helps control costs and response length.
   * @param temperature Controls randomness in LLM responses (0.0 to 2.0).
   *                    - Lower values (0.0-0.3): More deterministic, focused
   *                    - Medium values (0.5-0.8): Balanced creativity
   *                    - Higher values (1.0-2.0): More creative, diverse
   */
  case class AgentOptions(
    stream: Boolean = false,
    maxTokens: Option[Int] = None,
    temperature: Option[Double] = None
  )

  object AgentExecutionRequest {
    implicit val optionsDecoder: JsonDecoder[AgentOptions] =
      DeriveJsonDecoder.gen[AgentOptions]
    implicit val optionsEncoder: JsonEncoder[AgentOptions] =
      DeriveJsonEncoder.gen[AgentOptions]
    implicit val decoder: JsonDecoder[AgentExecutionRequest] =
      DeriveJsonDecoder.gen[AgentExecutionRequest]
    implicit val encoder: JsonEncoder[AgentExecutionRequest] =
      DeriveJsonEncoder.gen[AgentExecutionRequest]
  }

  /**
   * Response from agent execution (v0.2 API).
   * 
   * Provides comprehensive information about the execution result, including
   * the agent's output, any blockchain operations performed, and metadata
   * about the execution process.
   * 
   * @param agentId The ID of the agent that was executed
   * @param output The agent's primary output as a key-value map.
   *               - For LLM agents: typically contains "reply" or "answer"
   *               - For Blockchain agents: contains transaction details and status
   *               - For Hybrid agents: contains both LLM reasoning and blockchain results
   * @param blockchainRequests List of blockchain transaction requests generated by the agent.
   *                           Populated for Blockchain and Hybrid agents.
   * @param submittedTransactions List of transaction IDs for blockchain operations that were
   *                              successfully submitted. Can be used to track transaction status.
   * @param confidence Optional confidence score (0.0 to 1.0) indicating how confident
   *                   the agent is in its output. Useful for decision-making and filtering.
   * @param reasoning Optional explanation of the agent's decision-making process.
   *                  Provides transparency and helps users understand why certain
   *                  actions were taken or recommendations made.
   * @param executionTime Total execution time in milliseconds, from request receipt
   *                      to response generation. Useful for performance monitoring.
   * @param warnings List of non-fatal issues encountered during execution
   *                 (e.g., API rate limits, degraded performance, fallback usage).
   */
  case class AgentExecutionResponse(
    agentId: String,
    output: Map[String, String],
    blockchainRequests: List[BlockchainTransactionRequest] = List.empty,
    submittedTransactions: List[String] = List.empty,
    confidence: Option[Double] = None,
    reasoning: Option[String] = None,
    executionTime: Long,
    warnings: List[String] = List.empty
  )

  object AgentExecutionResponse {
    implicit val decoder: JsonDecoder[AgentExecutionResponse] =
      DeriveJsonDecoder.gen[AgentExecutionResponse]
    implicit val encoder: JsonEncoder[AgentExecutionResponse] =
      DeriveJsonEncoder.gen[AgentExecutionResponse]
  }

  /**
   * Raw response from Julia agent execution (v0.2 Julia FFI interface).
   * 
   * This model represents the direct output from Julia-based agents, before
   * being wrapped into the final AgentExecutionResponse. It's used internally
   * by executors when communicating with Julia via JuliaBridge.
   * 
   * Structure matches Julia's JSON output format for consistency across the
   * Scala-Julia boundary.
   * 
   * @param output The agent's primary output data as a key-value map
   * @param blockchainRequests Blockchain operations requested by the agent
   * @param confidence Agent's confidence score in the result (0.0 to 1.0)
   * @param reasoning Explanation of the agent's decision-making process
   * @param warnings Non-fatal issues encountered during execution
   * @param error Flag indicating if an error occurred in Julia
   * @param message Error message or additional information from Julia
   */
  case class JuliaAgentResponse(
    output: Map[String, String],
    blockchainRequests: List[BlockchainTransactionRequest] = List.empty,
    confidence: Option[Double] = None,
    reasoning: Option[String] = None,
    warnings: List[String] = List.empty,
    error: Option[Boolean] = None,
    message: Option[String] = None
  )

  object JuliaAgentResponse {
    implicit val decoder: JsonDecoder[JuliaAgentResponse] =
      DeriveJsonDecoder.gen[JuliaAgentResponse]
    implicit val encoder: JsonEncoder[JuliaAgentResponse] =
      DeriveJsonEncoder.gen[JuliaAgentResponse]
  }

  // ==================== Agent Metadata ====================

  /**
   * Metadata describing an agent's capabilities and constraints.
   * 
   * Provides detailed information about what an agent can do, what it has
   * access to, and what limitations apply. Used for agent discovery,
   * validation, and authorization.
   * 
   * @param id Unique agent identifier
   * @param name Human-readable agent name
   * @param description Brief description of the agent's purpose and capabilities
   * @param capabilities List of capabilities/actions this agent can perform
   *                     (e.g., "chat", "trading", "analysis", "code-generation")
   * @param supportedChains List of blockchain networks this agent can interact with
   *                        (e.g., "ethereum", "solana", "polygon")
   * @param permissions Security constraints and limits for this agent
   */
  case class AgentMetadata(
    id: String,
    name: String,
    description: String,
    capabilities: List[String],
    supportedChains: List[String],
    permissions: AgentPermissions
  )

  /**
   * Security permissions and constraints for an agent.
   * 
   * Defines limits on what an agent is allowed to do, particularly important
   * for blockchain agents that can perform financial transactions.
   * 
   * @param maxTransactionValue Maximum USD value for a single transaction.
   *                            Prevents agents from executing unauthorized large transfers.
   * @param allowedActions Whitelist of actions this agent is permitted to perform.
   *                       Any requested action not in this list will be rejected.
   *                       Examples: "swap", "transfer", "stake", "query"
   */
  case class AgentPermissions(
    maxTransactionValue: Double,
    allowedActions: List[String]
  )

  object AgentMetadata {
    implicit val permissionsDecoder: JsonDecoder[AgentPermissions] =
      DeriveJsonDecoder.gen[AgentPermissions]
    implicit val permissionsEncoder: JsonEncoder[AgentPermissions] =
      DeriveJsonEncoder.gen[AgentPermissions]
    implicit val decoder: JsonDecoder[AgentMetadata] =
      DeriveJsonDecoder.gen[AgentMetadata]
    implicit val encoder: JsonEncoder[AgentMetadata] =
      DeriveJsonEncoder.gen[AgentMetadata]
  }

  // ==================== Agent Errors ====================

  /**
   * Base trait for all agent-related errors.
   * 
   * Provides typed error handling throughout the agent execution pipeline,
   * allowing different error types to be handled appropriately at each layer.
   * 
   * All agent errors extend Throwable so they can be used with ZIO's error
   * channel and standard exception handling mechanisms.
   */
  sealed trait AgentError extends Throwable
  
  object AgentError {
    /**
     * Error indicating the requested agent doesn't exist in the registry.
     * 
     * Typically occurs when:
     * - Agent ID is misspelled
     * - Agent hasn't been registered yet
     * - Agent was removed from the registry
     * 
     * @param agentId The ID of the agent that was not found
     */
    case class NotFound(agentId: String) extends AgentError {
      override def getMessage: String = s"Agent not found: $agentId"
    }
    
    /**
     * Error indicating the agent execution failed.
     * 
     * Can occur due to:
     * - Julia runtime errors
     * - LLM API failures
     * - Blockchain network issues
     * - Internal processing errors
     * - Timeout exceeded
     * 
     * @param agentId The ID of the agent that failed
     * @param reason Detailed explanation of why execution failed
     */
    case class ExecutionFailed(agentId: String, reason: String) extends AgentError {
      override def getMessage: String = s"Agent execution failed: $reason"
    }
    
    /**
     * Error indicating the input provided to the agent was invalid.
     * 
     * Occurs when:
     * - Required parameters are missing
     * - Parameter types don't match expected format
     * - Values are out of acceptable ranges
     * - Input doesn't match agent's capability requirements
     * 
     * @param message Detailed explanation of what's wrong with the input
     */
    case class InvalidInput(message: String) extends AgentError {
      override def getMessage: String = message
    }
  }
}