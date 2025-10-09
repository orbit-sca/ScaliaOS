module ScaliaClient

# =============================
# = ScaliaClient Module =
# =============================
# This module provides utility functions for Julia agents to interact
# with the Scala backend (ScaliaOS server) for blockchain-related operations.
# It allows agents to submit blockchain requests and query their status.
# The module uses HTTP requests and JSON serialization/deserialization.

using HTTP    # HTTP.jl for making REST API calls
using JSON3   # JSON3.jl for encoding/decoding JSON payloads

# Exported functions for external use
export submit_blockchain_request, check_transaction_status


# =============================
# = Submit Blockchain Request =
# =============================
"""
    submit_blockchain_request(; action, chain, params, agent_id="unknown", priority="medium", scala_url=get(ENV, "SCALA_URL", "http://localhost:8080")) -> String

Submits a blockchain request (e.g., swap, transfer, approve) to the Scala backend.

# Keyword Arguments
- `action::String`: The blockchain operation type (swap, transfer, approve, etc.)
- `chain::String`: The blockchain network (e.g., "ethereum", "lukso")
- `params::Dict`: A dictionary of action-specific parameters (token, amount, address, etc.)
- `agent_id::String`: ID of the agent submitting the request (default: "unknown")
- `priority::String`: Request priority ("low", "medium", "high", "urgent"; default: "medium")
- `scala_url::String`: Base URL of the Scala backend (default from ENV `SCALA_URL` or localhost)

# Returns
- `requestId::String`: Unique ID assigned by the backend to track this request
"""
function submit_blockchain_request(;
    action::String,
    chain::String,
    params::Dict,
    agent_id::String="unknown",
    priority::String="medium",
    scala_url::String=get(ENV, "SCALA_URL", "http://localhost:8080")
)
    # Construct the request payload as a dictionary
    request_body = Dict(
        "agentId" => agent_id,
        "action" => action,
        "chain" => chain,
        "params" => params,
        "priority" => priority
    )
    
    # Send POST request to the Scala backend
    response = HTTP.post(
        "$scala_url/blockchain/submit",        # Endpoint for submitting requests
        ["Content-Type" => "application/json"], # Set header to JSON
        JSON3.write(request_body)               # Serialize payload to JSON string
    )
    
    # Parse JSON response from backend
    result = JSON3.read(String(response.body))
    
    # Return the request ID assigned by the backend
    return result.requestId
end


# =============================
# = Check Transaction Status =
# =============================
"""
    check_transaction_status(request_id::String; scala_url=get(ENV, "SCALA_URL", "http://localhost:8080")) -> Dict

Checks the current status of a blockchain transaction previously submitted.

# Arguments
- `request_id::String`: The unique ID of the blockchain request to query
- `scala_url::String`: Base URL of the Scala backend (default from ENV `SCALA_URL` or localhost)

# Returns
- `Dict`: JSON-decoded dictionary containing the current transaction status
"""
function check_transaction_status(request_id::String;
    scala_url::String=get(ENV, "SCALA_URL", "http://localhost:8080"))
    
    # Send GET request to the Scala backend status endpoint
    response = HTTP.get("$scala_url/blockchain/status/$request_id")
    
    # Parse and return JSON response
    return JSON3.read(String(response.body))
end

end # module




