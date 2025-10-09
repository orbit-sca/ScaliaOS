module ResponseModels

"""
    module ResponseModels

Defines the standardized data structures and serialization helpers for returning
results from Julia agents to the Scala or backend layer (e.g., ScaliaOS server).

This module is responsible for packaging all agent outputs — including model results,
blockchain requests, reasoning text, and confidence scores — into a uniform,
JSON-serializable format that can be consumed by ZIO/Scala services.

Exports:
- `AgentResult` — the primary response type.
- `to_dict` — converts an `AgentResult` or `BlockchainRequest` into a Dict for JSON encoding.
"""

export AgentResult, to_dict


# =========================
# = STRUCT DEFINITIONS =
# =========================

"""
    struct AgentResult

Represents a unified response object produced by an AI agent.

Fields
------
- `output::Dict{String,Any}`:
    The main content of the agent’s output (e.g., text generation, analysis result).

- `blockchain_requests::Vector`:
    A list of blockchain operations the agent suggests executing.
    Each entry is expected to be a `BlockchainRequest` struct or equivalent.

- `confidence::Float64`:
    The confidence score (0.0–1.0) indicating the agent’s certainty in its response.

- `reasoning::Union{String,Nothing}`:
    Optional explanation or rationale behind the agent’s decision or output.

- `warnings::Vector{String}`:
    A list of warning messages produced during agent execution.

Validation
----------
- Throws an `ArgumentError` if `confidence` is not within `[0.0, 1.0]`.

Example
-------
    using .ResponseModels
    result = AgentResult(
        Dict("ability" => "llm_chat", "message" => "Hello, world!"),
        [],
        0.95,
        "Based on prompt understanding",
        []
    )
"""
struct AgentResult
    output::Dict{String,Any}
    blockchain_requests::Vector
    confidence::Float64
    reasoning::Union{String,Nothing}
    warnings::Vector{String}

    function AgentResult(output, blockchain_requests=[], confidence=1.0, reasoning=nothing, warnings=String[])
        if confidence < 0.0 || confidence > 1.0
            throw(ArgumentError("Confidence must be between 0.0 and 1.0"))
        end
        new(output, blockchain_requests, confidence, reasoning, warnings)
    end
end


# =========================
# = CONVERSION HELPERS =
# =========================

"""
    to_dict(result::AgentResult) -> Dict{String,Any}

Converts an `AgentResult` into a dictionary suitable for JSON serialization.

Includes the `output`, serialized `blockchainRequests`, `confidence`, `warnings`,
and optionally `reasoning` if it is not `nothing`.

Example
-------
    d = to_dict(result)
    println(JSON.json(d))
"""
function to_dict(result::AgentResult)
    d = Dict{String,Any}(
        "output" => result.output,
        "blockchainRequests" => [to_dict(req) for req in result.blockchain_requests],
        "confidence" => result.confidence,
        "warnings" => result.warnings
    )

    if !isnothing(result.reasoning)
        d["reasoning"] = result.reasoning
    end

    return d
end


"""
    to_dict(req) -> Dict{String,Any}

Converts a `BlockchainRequest` (or compatible object) into a serializable dictionary.

This is used internally by `to_dict(::AgentResult)` to represent blockchain operations.

Expected Fields
---------------
- `req.action::String`
- `req.chain::String`
- `req.params::Dict`
- `req.priority::String`

Example
-------
    using .BlockchainRequests
    req = swap_request(from_token="ETH", to_token="USDC", amount="1.0")
    d = to_dict(req)
    println(JSON.json(d))
"""
function to_dict(req)
    return Dict{String,Any}(
        "action" => req.action,
        "chain" => req.chain,
        "params" => req.params,
        "priority" => req.priority
    )
end

end # module
