package com.scaliaos.app.http.endpoints

import sttp.tapir.ztapir._
import sttp.tapir.generic.auto._
import sttp.tapir.generic.auto.schemaForCaseClass
import sttp.tapir.json.zio._
import sttp.tapir.server.ServerEndpoint
import sttp.model.StatusCode
import com.scaliaos.app.models.AgentModels._
import com.scaliaos.app.models.{AgentType, AgentConfig}
import com.scaliaos.app.services.{BlockchainService, AgentRegistry, AgentExecutorFactory}
import zio._

/**
 * HTTP endpoint definitions for agent operations in the ScaliaOS v0.2 API.
 * 
 * This trait defines all agent-related HTTP endpoints using Tapir, a type-safe
 * endpoint description library that provides:
 * - Compile-time checked endpoint definitions
 * - Automatic OpenAPI/Swagger documentation generation
 * - Type-safe request/response handling
 * - Separation of endpoint description from implementation
 * 
 * Architecture:
 * The endpoints follow a registry-based routing pattern where:
 * 1. Client sends request with agent ID
 * 2. AgentRegistry looks up agent configuration
 * 3. AgentExecutorFactory routes to appropriate executor based on agent type
 * 4. Executor validates and executes the request
 * 5. Results are returned with comprehensive metadata
 * 
 * All endpoints use ZIO for effect management, providing:
 * - Composable error handling
 * - Structured logging
 * - Timeout protection
 * - Resource safety
 * 
 * Available Endpoints:
 * - POST /agent/run - Execute an agent with automatic type-based routing
 * - GET /agent/list - List all registered agents and their capabilities
 * - GET /agent/:agentId/status - Get configuration for a specific agent
 */
trait AgentExecutionEndpoint {

  // ==================== POST /agent/run ====================
  
  /**
   * Endpoint for executing an agent with automatic routing based on agent type.
   * 
   * This is the primary endpoint for agent execution in v0.2. It provides:
   * - Automatic executor selection based on agent type (LLM, Blockchain, Hybrid)
   * - Input validation before execution
   * - Timeout protection based on agent configuration
   * - Comprehensive error handling with appropriate HTTP status codes
   * - Detailed execution logging
   * 
   * Request Flow:
   * 1. Client POSTs AgentExecutionRequest with agentId and input
   * 2. System looks up agent in registry to determine type
   * 3. AgentExecutorFactory provides appropriate executor
   * 4. Executor validates the request
   * 5. Executor executes with timeout protection
   * 6. Results returned with execution metadata
   * 
   * HTTP Details:
   * - Method: POST
   * - Path: /agent/run
   * - Content-Type: application/json
   * - Request Body: AgentExecutionRequest (agentId, input, optional params)
   * - Success Response (200): AgentExecutionResponse with output and metadata
   * 
   * Error Responses:
   * - 404 Not Found: Agent ID doesn't exist in registry
   * - 400 Bad Request: Invalid input or validation failure
   * - 500 Internal Server Error: Execution failure, timeout, or system error
   * 
   * Timeout Behavior:
   * Each agent has a configured maxExecutionTime (default 30s, LLM agents 120s).
   * If execution exceeds this time, the request is cancelled and returns 500.
   * 
   * Example Request:
   * ```json
   * POST /agent/run
   * {
   *   "agentId": "llm-chat-gpt4",
   *   "input": {
   *     "message": "What is the capital of France?"
   *   }
   * }
   * ```
   * 
   * Example Success Response:
   * ```json
   * {
   *   "agentId": "llm-chat-gpt4",
   *   "output": {
   *     "reply": "The capital of France is Paris."
   *   },
   *   "blockchainRequests": [],
   *   "submittedTransactions": [],
   *   "confidence": 0.95,
   *   "reasoning": "Direct factual answer from knowledge base",
   *   "executionTime": 1250,
   *   "warnings": []
   * }
   * ```
   * 
   * @param registry AgentRegistry for looking up agent configurations
   * @param factory AgentExecutorFactory for getting type-specific executors
   * @return A ServerEndpoint that handles agent execution requests
   */
  def agentRunEndpoint(
    registry: AgentRegistry,
    factory: AgentExecutorFactory
  ): ServerEndpoint[Any, Task] = 
    endpoint
      .tag("agent")
      .name("run-agent")
      .description("Execute an agent with automatic routing based on agent type")
      .post
      .in("agent" / "run")
      .in(jsonBody[AgentExecutionRequest])
      .out(jsonBody[AgentExecutionResponse])
      .errorOut(
        oneOf[String](
          oneOfVariant(statusCode(StatusCode.NotFound).and(stringBody)),
          oneOfVariant(statusCode(StatusCode.BadRequest).and(stringBody)),
          oneOfVariant(statusCode(StatusCode.InternalServerError).and(stringBody))
        )
      )
      .zServerLogic { request =>
        val execution = for {
          // Log the start of execution for monitoring and debugging
          _ <- ZIO.logInfo(s"[Agent Execution] Starting: ${request.agentId}")
          
          // Step 1: Look up agent configuration to determine type and constraints
          // This will fail with AgentError.NotFound if agent doesn't exist
          agentConfig <- registry.getAgent(request.agentId)
          _ <- ZIO.logInfo(s"[Agent Execution] Found agent type: ${agentConfig.agentType}")
          
          // Step 2: Get the appropriate executor for this agent's type
          // Factory routes to LLMExecutor, BlockchainExecutor, or HybridExecutor
          executor <- factory.getExecutor(agentConfig.agentType)
          
          // Step 3: Validate the request before executing
          // Each executor has type-specific validation rules
          // Fails fast with AgentError.InvalidInput if validation fails
          _ <- executor.validate(request)
          _ <- ZIO.logInfo(s"[Agent Execution] Validation passed")
          
          // Step 4: Execute the agent with timeout protection
          // Uses the maxExecutionTime from agent config (e.g., 30s, 120s)
          // If execution exceeds timeout, cancels and returns error
          result <- executor.execute(request)
            .timeout(agentConfig.maxExecutionTime)
            .someOrFail(new Exception(s"Execution timed out after ${agentConfig.maxExecutionTime}"))
          
          // Log successful completion with execution metrics
          _ <- ZIO.logInfo(
            s"[Agent Execution] Completed: ${request.agentId} " +
            s"(${agentConfig.agentType}) in ${result.executionTime}ms, " +
            s"${result.submittedTransactions.length} txs submitted"
          )
          
        } yield result
        
        // Convert ZIO errors to HTTP error responses with appropriate status codes
        execution.mapError {
          case AgentError.NotFound(agentId) => 
            s"Agent not found: $agentId"
          case AgentError.ExecutionFailed(agentId, reason) => 
            s"Agent execution failed: $reason"
          case AgentError.InvalidInput(msg) => 
            s"Invalid input: $msg"
          case e: Throwable => 
            s"Internal error: ${e.getMessage}"
        }
      }

  // ==================== GET /agent/list ====================
  
  /**
   * Endpoint for listing all registered agents with their capabilities.
   * 
   * This endpoint provides agent discovery, allowing clients to:
   * - See all available agents
   * - Understand each agent's capabilities
   * - Determine which agent to use for their task
   * - Check agent requirements (blockchain access, timeouts)
   * 
   * The list includes both active and inactive agents, with full configuration
   * details for each. Clients can filter or select agents based on:
   * - Agent type (LLM, Blockchain, Hybrid, DataProcessing)
   * - Capabilities (chat, trading, analysis, etc.)
   * - Requirements (requiresBlockchain flag)
   * - Execution constraints (maxExecutionTime)
   * 
   * HTTP Details:
   * - Method: GET
   * - Path: /agent/list
   * - Success Response (200): Array of AgentConfig objects
   * - Error Response (500): String error message
   * 
   * Example Response:
   * ```json
   * [
   *   {
   *     "id": "llm-chat-gpt4",
   *     "name": "GPT-4 Chat Agent",
   *     "agentType": "llm",
   *     "description": "General purpose LLM chat",
   *     "capabilities": ["chat", "reasoning", "code-generation"],
   *     "requiresBlockchain": false,
   *     "maxExecutionTime": 120000
   *   },
   *   {
   *     "id": "solana-trading",
   *     "name": "Solana Trading Agent",
   *     "agentType": "blockchain",
   *     "description": "Executes trades on Solana",
   *     "capabilities": ["swap", "trade", "liquidity"],
   *     "requiresBlockchain": true,
   *     "maxExecutionTime": 30000
   *   }
   * ]
   * ```
   * 
   * Use Cases:
   * - Building agent selection UIs
   * - Dynamic agent discovery in client applications
   * - Monitoring available agents
   * - Generating documentation
   * 
   * @param registry AgentRegistry containing all registered agents
   * @return A ServerEndpoint that returns the list of all agents
   */
  def agentListEndpoint(registry: AgentRegistry): ServerEndpoint[Any, Task] = 
    endpoint
      .tag("agent")
      .name("list-agents")
      .description("List all registered agents with their capabilities")
      .get
      .in("agent" / "list")
      .out(jsonBody[List[AgentConfig]])
      .errorOut(stringBody)
      .zServerLogic { _ =>
        registry.listAgents()
          .tap(agents => ZIO.logInfo(s"[Agent Registry] Listed ${agents.length} agents"))
          .mapError(e => s"Failed to list agents: ${e.getMessage}")
      }

  // ==================== GET /agent/:agentId/status ====================
  
  /**
   * Endpoint for retrieving configuration and status for a specific agent.
   * 
   * This endpoint provides detailed information about a single agent, useful for:
   * - Validating that an agent exists before attempting execution
   * - Checking agent capabilities before sending requests
   * - Verifying agent requirements and constraints
   * - Building agent-specific UIs or documentation
   * 
   * Returns the complete AgentConfig including:
   * - Agent type and capabilities
   * - Execution constraints (timeout, blockchain requirements)
   * - Human-readable name and description
   * 
   * HTTP Details:
   * - Method: GET
   * - Path: /agent/:agentId/status
   * - Path Parameter: agentId (string) - The unique agent identifier
   * - Success Response (200): AgentConfig object
   * - Error Response (404/500): String error message
   * 
   * Example Request:
   * ```
   * GET /agent/llm-chat-gpt4/status
   * ```
   * 
   * Example Success Response:
   * ```json
   * {
   *   "id": "llm-chat-gpt4",
   *   "name": "GPT-4 Chat Agent",
   *   "agentType": "llm",
   *   "description": "General purpose LLM chat",
   *   "capabilities": ["chat", "reasoning", "code-generation"],
   *   "requiresBlockchain": false,
   *   "maxExecutionTime": 120000
   * }
   * ```
   * 
   * Example Error Response (404):
   * ```
   * Agent not found: invalid-agent-id
   * ```
   * 
   * Use Cases:
   * - Pre-flight checks before execution
   * - Agent capability verification
   * - Building agent-specific configuration UIs
   * - Health checks and monitoring
   * 
   * @param registry AgentRegistry for looking up agent configurations
   * @return A ServerEndpoint that returns the agent's configuration
   */
  def agentStatusEndpoint(registry: AgentRegistry): ServerEndpoint[Any, Task] = 
    endpoint
      .tag("agent")
      .name("agent-status")
      .description("Get agent configuration and status")
      .get
      .in("agent" / path[String]("agentId") / "status")
      .out(jsonBody[AgentConfig])
      .errorOut(stringBody)
      .zServerLogic { agentId =>
        registry.getAgent(agentId)
          .tap(config => ZIO.logInfo(s"[Agent Registry] Retrieved config for: $agentId"))
          .mapError {
            case AgentError.NotFound(id) => s"Agent not found: $id"
            case e: Throwable => e.getMessage
          }
      }

  // ==================== All Endpoints ====================
  
  /**
   * Collects all agent-related endpoints for registration with the HTTP server.
   * 
   * This method aggregates all endpoint definitions and wires them with their
   * required dependencies (BlockchainService, AgentRegistry, AgentExecutorFactory).
   * 
   * The returned list is consumed by the ZIO HTTP server in Main.scala, where
   * Tapir interprets the endpoint definitions and generates actual HTTP routes.
   * 
   * Dependency Injection:
   * All endpoints receive their dependencies as parameters, following the
   * ZIO pattern of explicit dependency management. This enables:
   * - Easy testing with mock services
   * - Clear dependency visualization
   * - Type-safe service composition
   * 
   * Note: blockchainService parameter is currently unused but included for
   * future endpoints that may need direct blockchain access (e.g., transaction
   * status queries, blockchain health checks).
   * 
   * @param blockchainService Service for blockchain operations (reserved for future use)
   * @param registry AgentRegistry for agent lookup and listing
   * @param factory AgentExecutorFactory for creating type-specific executors
   * @return List of all agent endpoints ready for server registration
   */
  def allEndpoints(
    blockchainService: BlockchainService,
    registry: AgentRegistry,
    factory: AgentExecutorFactory
  ): List[ServerEndpoint[Any, Task]] = 
    List(
      agentRunEndpoint(registry, factory),
      agentListEndpoint(registry),
      agentStatusEndpoint(registry)
    )
}

/**
 * Companion object providing a concrete implementation of AgentExecutionEndpoint.
 * 
 * This singleton instance can be imported and used directly throughout the application.
 * The trait pattern allows for easy testing by creating alternative implementations.
 */
object AgentExecutionEndpoint extends AgentExecutionEndpoint
