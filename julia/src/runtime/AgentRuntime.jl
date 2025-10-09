module AgentRuntime

using Base.Threads: @spawn
using .ResponseModels
using .ScaliaClient
using .BlockchainRequests

export Agent, AgentRegistry, start_agent_runtime, submit_task, AgentResult

# ------------------------------
# Agent definition
# ------------------------------
abstract type Agent end

"An agent that can process a task and return AgentResult"
struct BlockchainAgent <: Agent
    id::String
    chain::String
    maxConcurrentTasks::Int
end

# Function each agent implements
function run(agent::BlockchainAgent, input::Dict{String,Any})::AgentResult
    # Example logic: transfer some tokens
    output = Dict("message" => "Simulated transfer")
    
    blockchain_req = [transfer_request(
        to_address="0xABC123",
        token="ETH",
        amount="0.1",
        chain=agent.chain,
        priority="medium"
    )]
    
    AgentResult(
        output,
        blockchain_requests=blockchain_req,
        confidence=0.95,
        reasoning="Example transfer by $(agent.id)",
        warnings=[]
    )
end

# ------------------------------
# Registry & Supervisor
# ------------------------------
mutable struct AgentRegistry
    agents::Dict{String, Agent}
end

function AgentRegistry()
    AgentRegistry(Dict{String,Agent}())
end

"Register an agent in the runtime"
function register_agent!(registry::AgentRegistry, agent::Agent)
    registry.agents[agent.id] = agent
end

# ------------------------------
# Task Submission
# ------------------------------
"""
Submit a task to an agent and process blockchain requests
"""
function submit_task(registry::AgentRegistry, agentId::String, input::Dict{String,Any})
    agent = get(registry.agents, agentId, nothing)
    if agent === nothing
        error("Agent not found: $agentId")
    end

    # Run agent asynchronously
    fut = @spawn begin
        result = run(agent, input)
        
        # Submit blockchain requests to Scala
        requestIds = String[]
        for req in result.blockchain_requests
            push!(requestIds, ScaliaClient.submit_blockchain_request(
                action=req.action,
                chain=req.chain,
                params=req.params,
                agent_id=agentId,
                priority=req.priority
            ))
        end
        
        # Return enriched AgentResult with request IDs
        result_dict = ResponseModels.to_dict(result)
        result_dict["submittedTransactionIds"] = requestIds
        return result_dict
    end
    return fut
end

# ------------------------------
# Runtime Start
# ------------------------------
"""
Start multiple agents and wait for results
"""
function start_agent_runtime(registry::AgentRegistry, tasks::Vector{Tuple{String,Dict{String,Any}}})
    futures = Vector{Task}(undef, length(tasks))
    for (i, (agentId, input)) in enumerate(tasks)
        futures[i] = submit_task(registry, agentId, input)
    end
    results = [fetch(fut) for fut in futures]
    return results
end

end # module
