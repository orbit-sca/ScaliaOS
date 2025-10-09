# ScaliaOS

A Scala/ZIO SDK for [JuliaOS](https://github.com/Juliaoscode/JuliaOS), providing type-safe, enterprise-grade access to JuliaOS's AI agent framework through JVM infrastructure.

## What It Does

ScaliaOS enables JVM applications to orchestrate JuliaOS agents without Python or Node.js dependencies:

- Scala 3 + ZIO HTTP server exposing REST endpoints
- Type-safe process bridge to JuliaOS's Julia backend
- Compile-time verified JSON codecs with ZIO JSON
- Fiber-based concurrency for handling concurrent agent requests

## Current Status

**Early Development (v0.1.0-alpha)**

Currently implements:
- ✅ Basic agent orchestration (create, start, execute, stop)
- ✅ LLM integration (chat ability)
- ✅ Process management and lifecycle handling
- ✅ Type-safe error handling

Coming soon:
- More JuliaOS agent abilities
- Swarm coordination
- Enhanced monitoring and metrics

## Quick Start

### Prerequisites

- Julia 1.11+ ([install](https://julialang.org/downloads/))
- Scala 3.3+ and sbt
- JDK 11+

### Setup

1. Clone and install Julia dependencies:
```bash
cd julia
julia --project=. -e 'import Pkg; Pkg.instantiate()'
cd ..

Configure environment:

bashcp julia/.env.example julia/.env
# Edit julia/.env with your API keys

Run the server:

bashsbt "server/run"
Example Usage
bash# Ping an agent
curl -X POST http://localhost:8000/run-agent \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": "test",
    "name": "TestAgent",
    "ability": "ping",
    "prompt": "",
    "task": "test"
  }'

# Chat with LLM
curl -X POST http://localhost:8000/run-agent \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": "test",
    "name": "ChatAgent",
    "ability": "llm_chat",
    "prompt": "Tell me a joke",
    "task": "chat"
  }'
Architecture

Scala Layer: ZIO HTTP server, Tapir endpoints, type-safe JSON
Julia Layer: JuliaOS agent framework (agents, swarms, DEX, blockchain)
Bridge: Process spawning with stdin/stdout IPC

ScaliaOS is a client SDK - all agent logic, swarm algorithms, and blockchain operations are provided by JuliaOS.
Why ScaliaOS?
Use ScaliaOS if you:

Need JuliaOS agents in JVM infrastructure
Want compile-time type safety for agent configs
Prefer Scala's effect system (ZIO) over Python/TypeScript
Deploy to enterprise environments with JVM requirements

Contributing
Early stage - API may change. Contributions welcome but expect breaking changes.
License
MIT License - see LICENSE
Related

JuliaOS - The underlying AI agent framework
Built with: ZIO, Tapir, Julia


Note: ScaliaOS provides JVM access to JuliaOS. All AI/agent/swarm functionality is implemented by JuliaOS.
# ScaliaOS-private-v.0.2
