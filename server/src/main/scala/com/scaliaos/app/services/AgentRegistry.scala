// AgentRegistry.scala
package com.scaliaos.app.services

import com.scaliaos.app.models._
import com.scaliaos.app.models.AgentModels._
import com.scaliaos.app.models.{AgentType, AgentConfig}
import zio._

/**
 * Registry service for managing available agents in the ScaliaOS platform.
 * 
 * The AgentRegistry acts as a centralized catalog of all registered agents,
 * providing lookup, listing, and registration capabilities. It maintains
 * agent metadata including capabilities, configuration, and requirements.
 * 
 * This registry pattern allows for:
 * - Dynamic agent discovery
 * - Runtime agent registration
 * - Validation of agent existence before execution
 * - Centralized agent metadata management
 */
trait AgentRegistry {
  /**
   * Retrieves the configuration for a specific agent by ID.
   * 
   * @param agentId The unique identifier of the agent to retrieve
   * @return A Task containing the agent configuration
   * @throws AgentError.NotFound if no agent exists with the given ID
   */
  def getAgent(agentId: String): Task[AgentConfig]
  
  /**
   * Lists all currently registered agents in the system.
   * 
   * @return A Task containing a list of all agent configurations
   */
  def listAgents(): Task[List[AgentConfig]]
  
  /**
   * Registers a new agent or updates an existing agent's configuration.
   * 
   * If an agent with the same ID already exists, its configuration will be
   * replaced with the new configuration.
   * 
   * @param config The agent configuration to register
   * @return A Task that succeeds when registration is complete
   */
  def registerAgent(config: AgentConfig): Task[Unit]
}

/**
 * Live implementation of AgentRegistry using ZIO Ref for thread-safe state management.
 * 
 * This implementation stores agent configurations in memory using a Ref-wrapped Map,
 * providing safe concurrent access without explicit locking. The Ref ensures that
 * all updates are atomic and consistent.
 * 
 * @param agentsRef A ZIO Ref containing the map of agent ID to configuration
 */
class AgentRegistryLive(agentsRef: Ref[Map[String, AgentConfig]]) extends AgentRegistry {
  
  /**
   * Looks up an agent by ID in the registry.
   * 
   * Performs a safe lookup in the concurrent map and returns an error
   * if the agent doesn't exist.
   * 
   * @param agentId The agent identifier to look up
   * @return The agent's configuration, or AgentError.NotFound if not present
   */
  override def getAgent(agentId: String): Task[AgentConfig] = {
    agentsRef.get.flatMap { agentMap =>
      ZIO.fromOption(agentMap.get(agentId))
        .orElseFail(AgentError.NotFound(agentId))
    }
  }
  
  /**
   * Returns all registered agents as a list.
   * 
   * Extracts all values from the internal map without any filtering.
   * The order of agents in the returned list is not guaranteed.
   * 
   * @return List of all agent configurations in the registry
   */
  override def listAgents(): Task[List[AgentConfig]] = {
    agentsRef.get.map(_.values.toList)
  }
  
  /**
   * Adds or updates an agent in the registry.
   * 
   * Uses the agent's ID as the key. If an agent with the same ID already
   * exists, it will be replaced with the new configuration.
   * 
   * @param config The agent configuration to add/update
   */
  override def registerAgent(config: AgentConfig): Task[Unit] = {
    agentsRef.update(_ + (config.id -> config))
  }
}

/**
 * Companion object providing ZIO Layer construction and default agent configurations.
 */
object AgentRegistry {
  /**
   * Creates a ZIO layer with a live AgentRegistry instance pre-populated with default agents.
   * 
   * Default agents included:
   * 
   * 1. **llm-chat-gpt4**: General purpose LLM chat agent
   *    - Type: LLM
   *    - Capabilities: chat, reasoning, code generation
   *    - Max execution: 2 minutes
   *    - No blockchain required
   * 
   * 2. **solana-trading**: Blockchain trading agent for Solana
   *    - Type: Blockchain
   *    - Capabilities: swap, trade, liquidity management
   *    - Requires blockchain connection
   * 
   * 3. **ai-trader**: Hybrid AI-powered trading assistant
   *    - Type: Hybrid (combines LLM + Blockchain)
   *    - Capabilities: market analysis, trading, risk assessment
   *    - Requires blockchain connection
   * 
   * The layer has no dependencies and never fails, making it safe to compose
   * with other layers in the application.
   */
  val live: ZLayer[Any, Nothing, AgentRegistry] = 
    ZLayer.fromZIO {
      // Create a Ref with pre-populated default agents
      Ref.make(Map(
        // LLM Agent: General purpose chat and reasoning
        "llm-chat-gpt4" -> AgentConfig(
          id = "llm-chat-gpt4",
          name = "GPT-4 Chat Agent",
          agentType = AgentType.LLM,
          description = "General purpose LLM chat",
          capabilities = List("chat", "reasoning", "code-generation"),
          requiresBlockchain = false,
          maxExecutionTime = Duration.fromSeconds(120)  // 2 minutes for complex LLM tasks
        ),
        
        // Blockchain Agent: Pure on-chain operations
        "solana-trading" -> AgentConfig(
          id = "solana-trading",
          name = "Solana Trading Agent",
          agentType = AgentType.Blockchain,
          description = "Executes trades on Solana",
          capabilities = List("swap", "trade", "liquidity"),
          requiresBlockchain = true
        ),
        
        // Hybrid Agent: Combines AI decision-making with blockchain execution
        "ai-trader" -> AgentConfig(
          id = "ai-trader",
          name = "AI Trading Assistant",
          agentType = AgentType.Hybrid,
          description = "LLM-powered trading decisions with blockchain execution",
          capabilities = List("analysis", "trading", "risk-assessment"),
          requiresBlockchain = true
        )
      )).map(new AgentRegistryLive(_))  // Construct the registry after Ref is initialized
    }
}
