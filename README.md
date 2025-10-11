![scalia](https://github.com/user-attachments/assets/eacb766f-a772-4ab4-a1fd-7d56228f22e7)
# ScaliaOS

ScaliaOS provides enterprise-grade JVM infrastructure for JuliaOS's AI agent framework through a type-safe execution platform built with Scala, ZIO, and Tapir. It implements a registry-factory-executor pattern to manage and route requests across multiple agent types—including LLM conversational agents, blockchain trading agents, and hybrid workflows. The platform bridges Scala with JuliaOS for high-performance compute while exposing agent capabilities through a RESTful API for execution, discovery, and status checking.

# What It Does
**ScaliaOS v0.2 – Hybrid Scala + Julia Stack OS AI Agent Framework**

ScaliaOS is a reactive, type-safe framework that bridges Scala's powerful JVM ecosystem with Julia's high-performance computing capabilities.
Built on ZIO for functional effects and Tapir for HTTP APIs, it provides a robust platform for deploying AI agents that leverage Julia's numerical computing strengths alongside Scala's enterprise-grade reliability.

Powered by the JuliaOS stack, ScaliaOS integrates Julia’s modular agent system, dataflow runtime, and scientific libraries—enabling seamless interoperability between Julia-based analytical modules and Scala’s distributed backend. This hybrid architecture allows developers to build intelligent, high-performance agent systems that unify data science, AI, and systems engineering in one cohesive environment.

## Current Status

**Early Development (v0.2.0-alpha)**

Currently Implements:

- ✅ Registry-based architecture - Centralized agent configuration and discovery
- ✅ Type-safe routing - Factory-executor pattern for agent orchestration
- ✅ Live LLM agents - Real conversational AI via Scala-Julia bridge
- ✅ Multi-agent support - LLM, Blockchain, and Hybrid agent types
- ✅ RESTful API - Execute, list, and query agent status
- ✅ Production-ready - ZIO effects, error handling, and timeout management


 **What's New in v0.2**

- Refined Architecture: Cleaner separation between core framework, agent runtime, and HTTP layer
- Enhanced Type Safety: Improved models and error handling across the Scala-Julia boundary
- Modular Design: Better organized codebase with clear domain boundaries
- Agent Registry System: New agent discovery and management capabilities
- RESTful API: Standardized endpoints for agent execution and monitoring
- Production Ready: Improved error handling, logging, and process management
- Multi-Agent Support: Built-in support for LLM, blockchain, and hybrid agents


**Coming soon:**
- More JuliaOS agent abilities
- Swarm coordination
- TestNet Blockchain Integration
- Live Blockchain Integration
- Enhanced monitoring and metrics
- Frontend connection built in scala

## Quick Start

### 🛠 Prerequisites

- JDK 17+ installed
- Scala 3 and sbt installed
- Julia 1.10+ installed
- HTTPie or curl (for testing endpoints)
- Valid LLM API key (OpenAI, Groq, or compatible provider)

### ⚙️ Setup

1. Clone the Repository
   
bash
```
git clone https://github.com/your-username/scaliaOS.git
cd scaliaOS
```

3. Initialize Julia Environment
   
bash
```
cd julia
julia --project=@. -e 'using Pkg; Pkg.instantiate()'
cd ..
```

5. Configure Environment Variables
Create a .env file in the julia/ directory:

bash
```
# For OpenAI
OPENAI_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxx
OPENAI_MODEL=gpt-4o-mini

For Groq (alternative)
OPENAI_API_KEY=gsk_xxxxxxxxxxxxxxxxxxxxxxxx
OPENAI_BASE_URL=https://api.groq.com/openai/v1
OPENAI_MODEL=llama3-8b-8192
```
Note: Use the .env.example file as a template. The gpt-4o-mini model is recommended for OpenAI testing.

### 🚀 Running the Server
Start the ZIO HTTP server:

bash
```
sbt "server/run"
```
Expected output:
```
Starting ScaliaOS server on http://localhost:8000

Host: localhost
Port: 8000

Endpoints:
  POST http://localhost:8000/agent/run        - Execute an agent
  GET  http://localhost:8000/agent/list       - List all registered agents
  GET  http://localhost:8000/agent/:id/status - Get agent configuration

Registered Agents:
  - llm-chat-gpt4   (LLM)(Live)        - General purpose chat
  - solana-trading  (Blockchain)(Mock) - Trading on Solana
  - ai-trader       (Hybrid)(Mock)     - AI-powered trading

Server ready to accept connections
```
The server will listen on port 8000 and be ready to process agent requests.

## 🧪 Quick Testing with HTTPie
**Prerequisites**

-Server running on localhost:8000
-HTTPie installed (pip install httpie or brew install httpie)
-For LLM agent: Valid OpenAI API key in julia/.env


### Test 1: List All Agents

bash
```
http GET localhost:8000/agent/list
```
What this does: Shows all registered agents and their configurations.
Expected output: JSON array with agent details (id, type, capabilities, etc.)

### Test 2: Get Agent Status

bash
```
http GET localhost:8000/agent/llm-chat-gpt4/status
```
What this does: Retrieves configuration details for a specific agent.
Expected output: Agent configuration including type, capabilities, and timeout settings.

### Test 3: LLM Chat Agent (Live - Requires API Key)

bash
```
echo '{"agentId":"llm-chat-gpt4","input":{"message":"What is ZIO?"}}' | http POST localhost:8000/agent/run
```
What this does: Sends a chat message to GPT-4 and returns the AI response.
Status: ✅ LIVE - Makes real API calls to OpenAI (requires valid API key in julia/.env)
Expected output:
```
json{
  "agentId": "llm-chat-gpt4",
  "output": {
    "reply": "ZIO is a type-safe, composable library for asynchronous and concurrent programming in Scala..."
  },
  "executionTime": 1250,
  ...
}
```
Note: This will fail with "Julia process failed with exit code 1" if:

julia/.env file is missing
OpenAI API key is invalid or missing
Julia dependencies aren't installed


### Test 4: Solana Trading Agent (Mock)

bash
```
echo '{"agentId":"solana-trading","input":{"action":"analyze-market","symbol":"SOL/USDC"}}' | http POST localhost:8000/agent/run
```
What this does: Simulates market analysis for a Solana trading pair.
Status: 🔶 MOCK - Returns success without real blockchain interaction
Expected output:
```
json{
  "agentId": "solana-trading",
  "output": {
    "status": "success"
  },
  "executionTime": 1,
  "blockchainRequests": [],
  "submittedTransactions": []
}
```
Other trading commands:

bash
```
# Check balance (mock)
echo '{"agentId":"solana-trading","input":{"action":"check-balance","wallet":"mock-wallet-address"}}' | http POST localhost:8000/agent/run

# Get price (mock)
echo '{"agentId":"solana-trading","input":{"action":"get-price","token":"SOL"}}' | http POST localhost:8000/agent/run

# Execute trade (mock)
echo '{"agentId":"solana-trading","input":{"action":"execute-trade","token":"SOL","amount":1.0,"side":"buy"}}' | http POST localhost:8000/agent/run
Note: Mock mode returns minimal success responses. To enable real blockchain functionality:
```
Note: Mock mode returns minimal success responses. To enable real blockchain functionality:
- Configure Solana RPC endpoint in julia/config/config.toml
- Update agent status from "Mock" to "Live" in agent registry
- Implement full blockchain logic in Julia agent script


### Test 5: AI Trader Agent (Mock)

bash
```
echo '{"agentId":"ai-trader","input":{"operation":"analyze-market","symbol":"SOL/USDC","strategy":"momentum"}}' | http POST localhost:8000/agent/run
```
What this does: Simulates AI-powered market analysis.
Status: 🔶 MOCK - Returns success without real analysis
Expected output:
```
json{
  "agentId": "ai-trader",
  "output": {
    "status": "success"
  },
  "executionTime": 2,
  "blockchainRequests": [],
  "submittedTransactions": []
}
```
Other AI trader commands:

bash
```
# Auto-trade (mock)
echo '{"agentId":"ai-trader","input":{"operation":"auto-trade","symbol":"SOL/USDC","maxAmount":10.0,"strategy":"mean-reversion"}}' | http POST localhost:8000/agent/run

# Backtest (mock)
echo '{"agentId":"ai-trader","input":{"operation":"backtest","symbol":"SOL/USDC","strategy":"momentum","startDate":"2025-01-01","endDate":"2025-10-01"}}' | http POST localhost:8000/agent/run
```

### 🔧 Troubleshooting
LLM Agent Fails:
`Internal error: Julia process failed with exit code 1`
Solutions:

Create `julia/.env` with `OPENAI_API_KEY=sk-...`
Run `cd julia && julia --project=@. -e 'using Pkg; Pkg.instantiate()'`
Check server logs for detailed Julia error

Mock Agents Return Minimal Data:
This is expected behavior. Mock agents validate the request format but don't perform real operations. To enable full functionality, implement the business logic in the corresponding Julia agent scripts.

### 🎯 Quick Test Script
Create `test.sh`:

bash
```
#!/bin/bash
echo "Testing ScaliaOS v0.2..."
echo ""
echo "1. List agents:"
http GET localhost:8000/agent/list
echo ""
echo "2. LLM Chat (requires API key):"
echo '{"agentId":"llm-chat-gpt4","input":{"message":"Hello!"}}' | http POST localhost:8000/agent/run
echo ""
echo "3. Trading agent (mock):"
echo '{"agentId":"solana-trading","input":{"action":"analyze-market","symbol":"SOL/USDC"}}' | http POST localhost:8000/agent/run
```
Run with: `chmod +x test.sh && ./test.sh`


## 👩🛠️ Project Structure 
```
ScaliaOS/
│
├── 📜 api/                                         # API specifications and documentation
│   └── spec/
│       └── openapi.yaml                           # OpenAPI 3.1 spec for REST endpoints
│
├── 🔬 julia/                                      # Julia agent runtime (high-performance compute)
│   ├── config/                                    # Configuration management
│   │   ├── agents.toml                          # Agent registry and capabilities
│   │   ├── config.example.toml                  # Template configuration with examples
│   │   ├── config.jl                            # Julia config loader and validator  
│   │   └── config.toml                          # Main configuration (API keys, endpoints)
│   │
│   ├── examples/                                  # Working agent implementations
│   │   ├── basic_agent.jl                       # Simple LLM chat agent with streaming
│   │   ├── plan_execute_agent.jl                # Multi-step planning and execution
│   │   ├── run_agents.jl                        # Agent orchestration demo
│   │   ├── streaming_chat_agent.jl              # Real-time streaming responses  
│   │   └── trading_agent.jl                     # Blockchain trading strategy agent
│   │
│   ├── src/                                       # Core Julia modules
│   │   ├── agents/                              # Agent lifecycle and orchestration
│   │   │   ├── AgentCore.jl                    # Base agent abstractions and interfaces
│   │   │   ├── AgentMetrics.jl                 # Performance monitoring and analytics
│   │   │   ├── AgentMonitor.jl                 # Real-time agent health monitoring
│   │   │   ├── Agents.jl                       # Main agent module and exports
│   │   │   ├── Config.jl                       # Agent-specific configuration
│   │   │   ├── LLMIntegration.jl               # LLM provider integration (OpenAI, Groq)
│   │   │   ├── Persistence.jl                  # Agent state persistence layer
│   │   │   └── PlanAndExecute.jl               # Plan-and-execute agent pattern
│   │   │
│   │   ├── bridge/                              # Scala-Julia communication layer
│   │   │   ├── ResponseModels.jl               # Structured response formats
│   │   │   └── ScalaClient.jl                  # Client for calling Scala services
│   │   │
│   │   ├── framework/                           # Core framework utilities
│   │   │   ├── BlockchainRequests.jl           # Blockchain transaction builders
│   │   │   ├── JuliaOSFramework.jl             # Main framework initialization
│   │   │   ├── LlmHandlers.jl                  # LLM request/response handlers
│   │   │   ├── SwarmHandlers.jl                # Swarm intelligence handlers
│   │   │   └── Utils.jl                        # Common utility functions
│   │   │
│   │   ├── risk/                                # Risk management module
│   │   │   └── RiskManager.jl                  # Position sizing, risk limits
│   │   │
│   │   ├── runtime/                             # Agent execution runtime
│   │   │   ├── AgentRuntime.jl                 # Agent execution environment
│   │   │   └── BlockchainAgent.jl              # Blockchain-specific agent runtime
│   │   │
│   │   ├── storage/                             # Data persistence layer
│   │   │   ├── local_storage.jl                # File-based storage backend
│   │   │   ├── storage_interface.jl            # Abstract storage interface
│   │   │   └── Storage.jl                      # Main storage module
│   │   │
│   │   ├── strategies/                          # Trading and decision strategies
│   │   │   ├── MeanReversionImpl.jl            # Mean reversion trading strategy
│   │   │   ├── MovingAverageStrategy.jl        # MA crossover strategies
│   │   │   ├── RiskManagement.jl               # Portfolio risk management
│   │   │   ├── TradeLogger.jl                  # Trade execution logging
│   │   │   └── TradingStrategy.jl              # Base strategy interface
│   │   │
│   │   ├── swarm/                               # Swarm intelligence algorithms
│   │   │   ├── algorithms/                     # Optimization algorithms
│   │   │   │   ├── de.jl                      # Differential Evolution
│   │   │   │   ├── ga.jl                      # Genetic Algorithm
│   │   │   │   └── pso.jl                     # Particle Swarm Optimization
│   │   │   ├── SwarmBase.jl                   # Base swarm abstractions
│   │   │   └── Swarms.jl                      # Swarm orchestration
│   │   │
│   │   ├── entrypoint.jl                        # Main Julia entry point
│   │   ├── juliaos_server.jl                    # Julia HTTP server (optional)
│   │   ├── JuliaOS.jl                           # JuliaOS compatibility layer
│   │   └── server.jl                            # Server utilities
│   │
│   ├── test/                                     # Julia test suites
│   │   └── [test directories]                   # Comprehensive test coverage
│   │
│   ├── .env.example                              # Environment variable template
│   ├── .gitignore                                # Julia-specific ignores
│   ├── DEVELOPMENT.md                            # Julia development guide
│   ├── package.json                              # Optional Node.js dependencies
│   └── Project.toml                              # Julia package manifest
│
├── 🏛️ project/                                    # SBT build configuration
│   ├── project/                                  # Nested SBT plugins
│   │   └── [nested project files]              # Build plugin configurations
│   ├── target/                                   # SBT build artifacts (gitignored)
│   │   └── [build artifacts]                   
│   ├── build.properties                         # SBT version specification
│   └── plugins.sbt                              # SBT plugin dependencies
│
├── ⚡ server/                                      # Scala backend (ZIO + Tapir)
│   ├── src/
│   │   └── main/
│   │       └── scala/
│   │           └── com/
│   │               └── scaliaos/
│   │                   └── app/
│   │                       ├── bridge/          # Julia-Scala integration
│   │                       │   └── JuliaBridge.scala     # Process management, JSON encoding
│   │                       │
│   │                       ├── http/            # HTTP API layer
│   │                       │   └── endpoints/
│   │                       │       └── AgentExecutionEndpoint.scala  # REST endpoints
│   │                       │
│   │                       ├── models/          # Domain models and types
│   │                       │   ├── AgentModels.scala      # Agent request/response models
│   │                       │   ├── AgentTypes.scala       # Agent type definitions
│   │                       │   └── BlockchainModels.scala # Blockchain transaction models
│   │                       │
│   │                       ├── services/        # Business logic layer
│   │                       │   ├── executors/  # Agent execution strategies
│   │                       │   │   └── [LLM, Blockchain, Hybrid executors]
│   │                       │   ├── AgentExecutorFactory.scala  # Factory pattern router
│   │                       │   ├── AgentRegistry.scala         # Agent discovery service
│   │                       │   └── BlockchainService.scala     # Blockchain operations
│   │                       │
│   │                       ├── test/           # Integration tests
│   │                       │   └── AgentTask.scala        # Agent test scenarios
│   │                       │
│   │                       └── Main.scala      # Server entry point (ZIO HTTP)
│   │
│   └── target/                                   # Scala build artifacts (gitignored)
│       └── [build artifacts]
│
├── target/                                        # Root build artifacts (gitignored)
│   └── [root build artifacts]
│
├── .gitignore                                     # Git ignore patterns
├── build.sbt                                      # Root Scala build definition
├── LICENSE                                        # Project license
└── README.md                                      # Project documentation
```
