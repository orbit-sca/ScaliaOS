module BlockchainAgent

using ..bridge.ResponseModels
using ..bridge.ScalaClient
using ..framework.BlockchainRequests
using ..runtime.AgentRuntime

export BlockchainAgent

# ------------------------------
# Example agent type
# ------------------------------
struct BlockchainAgent <: AgentRuntime.Agent
    id::String
    chain::String
    maxConcurrentTasks::Int
end

# Each agent implements its own run function
function AgentRuntime.run(agent::BlockchainAgent, input::Dict{String,Any})::ResponseModels.AgentResult
    # Example logic: produce output and blockchain requests
    output = Dict("message" => "Simulated transfer by $(agent.id)")
    
    blockchain_req = [BlockchainRequests.transfer_request(
        to_address="0xABC123",
        token="ETH",
        amount="0.1",
        chain=agent.chain,
        priority="medium"
    )]
    
    return ResponseModels.AgentResult(
        output,
        blockchain_requests=blockchain_req,
        confidence=0.95,
        reasoning="Example transfer executed by $(agent.id)",
        warnings=[]
    )
end

end # module
