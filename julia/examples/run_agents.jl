using .runtime.AgentRuntime
using .runtime.BlockchainAgent
using .bridge.ResponseModels

# ------------------------------
# Setup
# ------------------------------
registry = AgentRuntime.AgentRegistry()

# Create agents
agent1 = BlockchainAgent.BlockchainAgent("trading_agent", "ethereum", 5)
agent2 = BlockchainAgent.BlockchainAgent("monitor_agent", "arbitrum", 3)

# Register agents
AgentRuntime.register_agent!(registry, agent1)
AgentRuntime.register_agent!(registry, agent2)

# ------------------------------
# Submit tasks
# ------------------------------
tasks = [
    ("trading_agent", Dict("signal" => "buy")),
    ("monitor_agent", Dict("check" => "balance"))
]

results = AgentRuntime.start_agent_runtime(registry, tasks)

# ------------------------------
# Print results
# ------------------------------
for r in results
    println("AgentResult:")
    println(r)
end
