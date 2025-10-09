// AgentExecutor.scala
package com.scaliaos.app.services.executors

import com.scaliaos.app.models.AgentModels._
import com.scaliaos.app.models.AgentType
import zio._

/**
 * Base trait defining the contract for all agent executors in the ScaliaOS platform.
 * 
 * An AgentExecutor is responsible for:
 * - Validating execution requests before processing
 * - Executing agent logic based on the request parameters
 * - Returning structured responses with results or errors
 * 
 * Different executor implementations handle different agent types:
 * - LLMAgentExecutor: Handles language model interactions (chat, generation, reasoning)
 * - BlockchainAgentExecutor: Handles on-chain operations (transactions, queries, trading)
 * - HybridAgentExecutor: Combines LLM and blockchain capabilities
 * - DataProcessingAgentExecutor: Handles data analysis and transformation (future)
 * 
 * Each executor implementation must be stateless to allow safe concurrent usage
 * across multiple requests.
 */
trait AgentExecutor {
    /**
     * Executes an agent task based on the provided request.
     * 
     * This is the main entry point for agent execution. The executor should:
     * 1. Parse the request parameters
     * 2. Perform any necessary preprocessing
     * 3. Execute the core agent logic
     * 4. Format and return the results
     * 
     * The execution may involve:
     * - API calls to external services (LLM providers, blockchain RPCs)
     * - Complex computations or data transformations
     * - Multi-step workflows combining different capabilities
     * 
     * @param request The execution request containing agent ID, input parameters,
     *                configuration overrides, and execution context
     * @return A Task containing the execution response with results, status,
     *         execution metadata, and any generated outputs
     * @throws AgentError if execution fails due to validation, processing, or
     *         external service errors
     */
    def execute(request: AgentExecutionRequest): Task[AgentExecutionResponse]
    
    /**
     * Validates an execution request before processing.
     * 
     * Performs pre-execution validation to ensure:
     * - All required parameters are present and properly formatted
     * - Parameter values are within acceptable ranges
     * - The agent has necessary permissions/capabilities for the requested operation
     * - Any prerequisites (API keys, blockchain connections) are satisfied
     * 
     * This method should be fast and fail quickly if the request is invalid,
     * preventing unnecessary resource usage on requests that will fail anyway.
     * 
     * Validation checks may include:
     * - Required fields presence
     * - Data type and format validation
     * - Business rule validation (e.g., transaction amounts, rate limits)
     * - Capability verification (e.g., agent supports the requested action)
     * 
     * @param request The execution request to validate
     * @return A Task that succeeds if validation passes, or fails with a
     *         descriptive error explaining what validation failed
     * @throws AgentError.ValidationError if the request is invalid
     */
    def validate(request: AgentExecutionRequest): Task[Unit]
    
    /**
     * Returns the type of agent this executor handles.
     * 
     * Used by the AgentExecutorFactory to route requests to the appropriate
     * executor implementation based on the agent's type.
     * 
     * @return The AgentType enum value (LLM, Blockchain, Hybrid, or DataProcessing)
     */
    def getAgentType: AgentType
}
