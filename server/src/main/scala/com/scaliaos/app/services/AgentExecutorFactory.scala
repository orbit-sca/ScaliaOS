// server/src/main/scala/com/scaliaos/app/services/AgentExecutorFactory.scala
package com.scaliaos.app.services

import com.scaliaos.app.models.AgentType
import com.scaliaos.app.services.executors._
import zio._

/**
 * Factory service for creating and managing agent executors.
 * 
 * This factory implements a singleton pattern for executors, maintaining a single
 * instance of each executor type to optimize resource usage. Each executor is
 * responsible for running a specific type of agent (LLM, Blockchain, Hybrid, etc.).
 */
trait AgentExecutorFactory {
  /**
   * Retrieves an executor instance for the specified agent type.
   * 
   * @param agentType The type of agent executor to retrieve
   * @return A ZIO Task that succeeds with the appropriate executor, or fails if
   *         the executor type is not yet implemented
   */
  def getExecutor(agentType: AgentType): Task[AgentExecutor]
}

/**
 * Live implementation of the AgentExecutorFactory.
 * 
 * Creates and maintains singleton instances of each executor type:
 * - LLMAgentExecutor: Handles language model-based agents
 * - BlockchainAgentExecutor: Handles blockchain interaction agents
 * - HybridAgentExecutor: Combines LLM and blockchain capabilities
 * 
 * @param blockchainService The blockchain service used by blockchain and hybrid executors
 */
class AgentExecutorFactoryLive(
  blockchainService: BlockchainService
) extends AgentExecutorFactory {
  
  // Singleton executor instances - created once and reused for all requests
  private val llmExecutor = new LLMAgentExecutor()
  private val blockchainExecutor = new BlockchainAgentExecutor(blockchainService)
  private val hybridExecutor = new HybridAgentExecutor(llmExecutor, blockchainExecutor)
  
  /**
   * Returns the appropriate executor based on agent type.
   * 
   * Currently supported types:
   * - LLM: For language model agents (e.g., chat, text generation)
   * - Blockchain: For blockchain agents (e.g., trading, smart contracts)
   * - Hybrid: For agents combining LLM and blockchain capabilities
   * 
   * @param agentType The type of agent executor needed
   * @return A ZIO Task containing the executor, or a failure for unimplemented types
   */
  override def getExecutor(agentType: AgentType): Task[AgentExecutor] = {
    agentType match {
      case AgentType.LLM => ZIO.succeed(llmExecutor)
      case AgentType.Blockchain => ZIO.succeed(blockchainExecutor)
      case AgentType.Hybrid => ZIO.succeed(hybridExecutor)
      case AgentType.DataProcessing => 
        // TODO: Implement data processing executor
        ZIO.fail(new Exception("Data processing executor not implemented yet"))
    }
  }
}

/**
 * Companion object providing ZIO Layer construction for dependency injection.
 */
object AgentExecutorFactory {
  /**
   * ZIO Layer that constructs a live AgentExecutorFactory instance.
   * 
   * Requires BlockchainService as a dependency and provides AgentExecutorFactory
   * to the ZIO environment. This layer can be composed with other layers in the
   * application's dependency graph.
   */
  val live: ZLayer[BlockchainService, Nothing, AgentExecutorFactory] =
    ZLayer.fromFunction(new AgentExecutorFactoryLive(_))
} 
