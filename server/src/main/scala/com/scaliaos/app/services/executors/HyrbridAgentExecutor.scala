// HybridAgentExecutor.scala
package com.scaliaos.app.services.executors

import com.scaliaos.app.models.AgentModels._
import com.scaliaos.app.models.AgentType
import zio._
import zio.json._
import zio.json.ast.Json

/**
 * Executor implementation for hybrid agents that combine LLM and blockchain capabilities.
 * 
 * Hybrid agents represent the most powerful agent type in ScaliaOS, combining:
 * - **LLM capabilities**: Natural language understanding, reasoning, and decision-making
 * - **Blockchain capabilities**: On-chain transaction execution and data queries
 * 
 * Architecture:
 * This executor implements a two-phase execution pattern:
 * 
 * Phase 1 (LLM Analysis):
 * - Receives user request in natural language or structured format
 * - Uses LLM to understand intent, context, and requirements
 * - Generates reasoning and determines appropriate blockchain actions
 * 
 * Phase 2 (Blockchain Execution):
 * - Takes LLM output and translates it to blockchain operations
 * - Submits transactions via the blockchain executor
 * - Combines LLM reasoning with blockchain results
 * 
 * Use cases:
 * - AI-powered trading: LLM analyzes market conditions, blockchain executes trades
 * - Smart contract interactions: LLM interprets complex contract logic, blockchain calls methods
 * - Risk assessment: LLM evaluates risks, blockchain performs hedging operations
 * - Portfolio management: LLM suggests rebalancing, blockchain executes swaps
 * 
 * @param llmExecutor The LLM executor for natural language processing and reasoning
 * @param blockchainExecutor The blockchain executor for on-chain operations
 */
class HybridAgentExecutor(
  llmExecutor: LLMAgentExecutor,
  blockchainExecutor: BlockchainAgentExecutor
) extends AgentExecutor {
  
  /**
   * Identifies this executor as handling Hybrid-type agents.
   * 
   * @return AgentType.Hybrid
   */
  override def getAgentType: AgentType = AgentType.Hybrid
  
  /**
   * Validates the request against both LLM and blockchain requirements.
   * 
   * Performs dual validation:
   * 1. LLM validation: Ensures input can be processed by language model
   * 2. Blockchain validation: Ensures necessary blockchain parameters are present
   * 
   * Both validations must pass for the hybrid agent to execute. This ensures
   * that the request contains everything needed for both phases of execution.
   * 
   * The `*>` operator sequences the validations, short-circuiting if the first fails.
   * 
   * @param request The execution request to validate
   * @return Success if both validations pass
   * @throws Exception if either LLM or blockchain validation fails
   */
  override def validate(request: AgentExecutionRequest): Task[Unit] = 
    llmExecutor.validate(request) *> blockchainExecutor.validate(request)
  
  /**
   * Executes a hybrid agent task using a two-phase LLM-then-blockchain approach.
   * 
   * Execution workflow:
   * 
   * **Phase 1: LLM Analysis**
   * - Passes the original request to the LLM executor
   * - LLM processes natural language input and generates:
   *   - Understanding of user intent
   *   - Reasoning about appropriate actions
   *   - Structured blockchain operation parameters
   * 
   * **Phase 2: Blockchain Execution**
   * - Takes LLM output and reformats it for blockchain execution
   * - Converts LLM's structured response into blockchain transaction requests
   * - Submits transactions via blockchain executor
   * - Collects transaction hashes and results
   * 
   * **Response Synthesis**
   * - Combines blockchain execution results with LLM reasoning
   * - Returns comprehensive response showing both "why" (LLM) and "what" (blockchain)
   * 
   * This pipeline allows for intelligent, context-aware blockchain operations
   * where the LLM acts as a decision engine and the blockchain executor as
   * the execution engine.
   * 
   * Example flow for AI trading agent:
   * 1. User: "Buy SOL if the price is favorable"
   * 2. LLM: Analyzes market conditions, determines price is good, outputs trade parameters
   * 3. Blockchain: Executes swap transaction on Solana DEX
   * 4. Response: Transaction hash + LLM explanation of why trade was executed
   * 
   * @param request The execution request with user input and agent configuration
   * @return An AgentExecutionResponse containing:
   *         - Blockchain transaction results (output, tx hashes, execution time)
   *         - LLM reasoning explaining the decisions made
   *         - Combined metadata from both execution phases
   * @throws Exception if either LLM analysis or blockchain execution fails
   */
  override def execute(request: AgentExecutionRequest): Task[AgentExecutionResponse] = {
    for {
      // Phase 1: Use LLM to analyze the request and determine intent
      // The LLM interprets natural language, considers context, and generates
      // a structured plan for blockchain operations
      llmResult <- llmExecutor.execute(request)
      
      // Phase 2: Execute blockchain actions based on LLM analysis
      // Convert LLM output back to JSON and pass as input to blockchain executor
      // This allows the LLM's decisions to drive on-chain operations
      blockchainResult <- blockchainExecutor.execute(
        request.copy(
          input = llmResult.output.toJson.fromJson[Json].toOption.get
        )
      )
      
    } yield blockchainResult.copy(
      // Preserve the LLM's reasoning in the final response
      // This provides transparency about why certain blockchain actions were taken
      reasoning = Some(llmResult.output.getOrElse("reply", ""))
    )
  }
}
