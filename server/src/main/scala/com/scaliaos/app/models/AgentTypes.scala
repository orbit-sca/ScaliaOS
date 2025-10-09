// server/src/main/scala/com/scaliaos/app/models/AgentTypes.scala
package com.scaliaos.app.models

import zio.Duration
import zio.json._

// ==================== Agent Types ====================

/**
 * Sealed trait representing the fundamental types of agents in ScaliaOS.
 * 
 * Agent types determine:
 * - Which executor implementation handles the agent
 * - What capabilities and resources the agent has access to
 * - How the agent's execution is validated and monitored
 * 
 * Using a sealed trait ensures exhaustive pattern matching at compile time,
 * preventing runtime errors from unhandled agent types.
 */
sealed trait AgentType

object AgentType {
  /**
   * Large Language Model agents that interact with AI models for natural language tasks.
   * 
   * Capabilities:
   * - Natural language understanding and generation
   * - Conversational interfaces (chat, Q&A)
   * - Text analysis and summarization
   * - Code generation and explanation
   * - Reasoning and problem-solving
   * 
   * Examples: GPT-4 chat agent, Claude assistant, code generation agent
   * 
   * Resources required: LLM API keys (OpenAI, Anthropic, etc.)
   */
  case object LLM extends AgentType
  
  /**
   * Blockchain-focused agents that interact directly with blockchain networks.
   * 
   * Capabilities:
   * - Transaction submission and monitoring
   * - Smart contract interactions
   * - On-chain data queries
   * - DeFi protocol operations (swaps, staking, liquidity)
   * - Wallet management
   * 
   * Examples: Solana trading agent, Ethereum DeFi agent, NFT minting agent
   * 
   * Resources required: Blockchain RPC endpoints, wallet credentials
   */
  case object Blockchain extends AgentType
  
  /**
   * Hybrid agents combining LLM intelligence with blockchain execution capabilities.
   * 
   * These represent the most sophisticated agent type, using LLMs for decision-making
   * and blockchain networks for execution. The LLM acts as the "brain" analyzing
   * context and determining actions, while blockchain serves as the "hands" executing
   * those actions on-chain.
   * 
   * Capabilities:
   * - AI-powered trading decisions with on-chain execution
   * - Natural language interfaces to blockchain operations
   * - Intelligent risk assessment and portfolio management
   * - Context-aware DeFi interactions
   * 
   * Examples: AI trading assistant, conversational DeFi agent, smart portfolio manager
   * 
   * Resources required: Both LLM API keys and blockchain access
   */
  case object Hybrid extends AgentType
  
  /**
   * Data processing agents for analysis, transformation, and computation tasks.
   * 
   * Capabilities:
   * - Data analysis and statistical computations
   * - File processing (CSV, JSON, Excel)
   * - Data transformation and ETL operations
   * - Report generation and visualization
   * 
   * Note: This agent type is planned but not yet implemented in v0.2
   * 
   * Examples: CSV analyzer, report generator, data pipeline agent
   * 
   * Resources required: File storage access, computation resources
   */
  case object DataProcessing extends AgentType
  
  /**
   * Converts a string representation to an AgentType.
   * 
   * Useful for parsing configuration files, API requests, or user input.
   * Case-insensitive for user convenience.
   * 
   * @param s The string to parse (e.g., "llm", "blockchain", "hybrid", "data")
   * @return Some(AgentType) if the string matches a valid type, None otherwise
   */
  def fromString(s: String): Option[AgentType] = s.toLowerCase match {
    case "llm" => Some(LLM)
    case "blockchain" => Some(Blockchain)
    case "hybrid" => Some(Hybrid)
    case "data" => Some(DataProcessing)
    case _ => None
  }
  
  /**
   * JSON encoder for AgentType.
   * 
   * Converts AgentType to lowercase string representation for API responses.
   * Uses snake_case for DataProcessing to follow JSON naming conventions.
   */
  implicit val encoder: JsonEncoder[AgentType] = JsonEncoder[String].contramap {
    case LLM => "llm"
    case Blockchain => "blockchain"
    case Hybrid => "hybrid"
    case DataProcessing => "data_processing"
  }
  
  /**
   * JSON decoder for AgentType.
   * 
   * Parses string values from API requests into AgentType instances.
   * Returns a Left with error message for invalid types, enabling proper
   * error handling in the HTTP layer.
   */
  implicit val decoder: JsonDecoder[AgentType] = JsonDecoder[String].mapOrFail {
    case "llm" => Right(LLM)
    case "blockchain" => Right(Blockchain)
    case "hybrid" => Right(Hybrid)
    case "data_processing" => Right(DataProcessing)
    case other => Left(s"Unknown agent type: $other")
  }
}

/**
 * Configuration model for an agent in the ScaliaOS registry.
 * 
 * AgentConfig defines the static properties and capabilities of an agent,
 * distinguishing it from execution-time parameters. This configuration is
 * stored in the AgentRegistry and used for:
 * - Agent discovery and listing
 * - Validation before execution
 * - Routing to appropriate executors
 * - Enforcing resource and time constraints
 * 
 * @param id Unique identifier for the agent (e.g., "llm-chat-gpt4", "solana-trading").
 *           Used as the primary key in the registry and for API requests.
 * @param name Human-readable display name for the agent (e.g., "GPT-4 Chat Agent").
 *             Used in UIs and documentation.
 * @param agentType The type of agent (LLM, Blockchain, Hybrid, DataProcessing).
 *                  Determines which executor handles this agent.
 * @param description Brief explanation of the agent's purpose and use cases.
 *                    Helps users understand when to use this agent.
 * @param capabilities List of specific capabilities this agent provides.
 *                     Examples: ["chat", "reasoning"], ["swap", "trade"], ["analysis", "trading"]
 * @param requiresBlockchain Flag indicating if this agent needs blockchain connectivity.
 *                           If true, validates blockchain service availability before execution.
 *                           Default: false (only true for Blockchain and Hybrid agents)
 * @param maxExecutionTime Maximum time allowed for agent execution before timeout.
 *                         Prevents runaway executions and ensures responsiveness.
 *                         Default: 30 seconds (can be overridden per agent)
 *                         Typical values:
 *                         - LLM agents: 60-120 seconds (LLM API calls can be slow)
 *                         - Blockchain agents: 30-60 seconds (transaction submission)
 *                         - Hybrid agents: 90-180 seconds (combined LLM + blockchain)
 */
case class AgentConfig(
  id: String,
  name: String,
  agentType: AgentType,
  description: String,
  capabilities: List[String],
  requiresBlockchain: Boolean = false,
  maxExecutionTime: Duration = Duration.fromSeconds(30)
)

object AgentConfig {
  /**
   * Custom JSON encoder for Duration.
   * 
   * Converts ZIO Duration to milliseconds for JSON serialization.
   * Milliseconds provide sufficient precision for execution timeouts
   * while keeping the JSON representation simple.
   */
  implicit val durationEncoder: JsonEncoder[Duration] = 
    JsonEncoder[Long].contramap(_.toMillis)
  
  /**
   * Custom JSON decoder for Duration.
   * 
   * Parses milliseconds from JSON and reconstructs ZIO Duration.
   * Handles timeout values from API requests and configuration files.
   */
  implicit val durationDecoder: JsonDecoder[Duration] = 
    JsonDecoder[Long].map(millis => Duration.fromMillis(millis))
  
  /**
   * Automatic JSON encoder for AgentConfig.
   * 
   * Derives the encoder from the case class structure, using the custom
   * Duration and AgentType encoders defined above.
   */
  implicit val encoder: JsonEncoder[AgentConfig] = DeriveJsonEncoder.gen[AgentConfig]
  
  /**
   * Automatic JSON decoder for AgentConfig.
   * 
   * Derives the decoder from the case class structure, using the custom
   * Duration and AgentType decoders defined above. Validates all fields
   * during deserialization.
   */
  implicit val decoder: JsonDecoder[AgentConfig] = DeriveJsonDecoder.gen[AgentConfig]
}