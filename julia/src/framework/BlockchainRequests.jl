module BlockchainRequests

"""
    module BlockchainRequests

Provides a unified, typed interface for representing blockchain-related actions
(swap, transfer, approve) as structured data that can be serialized and passed
to other components — such as AI agents, Scala bridges, or smart contract layers.

Each blockchain request is modeled as a `BlockchainRequest` struct that includes:
- The `action` (e.g., "swap", "transfer", "approve")
- The target `chain` (e.g., "ethereum", "lukso", "polygon")
- A dictionary of request-specific `params`
- A `priority` level that determines urgency (low, medium, high, urgent)

Intended usage:
    using .BlockchainRequests
    req = swap_request(
        from_token = "ETH",
        to_token   = "USDC",
        amount     = "1.0"
    )

This design allows flexible serialization into JSON for cross-language agents
(e.g. Julia → Scala → ZIO backend).
"""

export BlockchainRequest, swap_request, transfer_request, approve_request

# =========================
# = STRUCT DEFINITIONS =
# =========================

"""
    struct BlockchainRequest

Represents a standardized blockchain action request.

Fields
------
- `action::String`: The action type (`"swap"`, `"transfer"`, `"approve"`, etc.)
- `chain::String`: The blockchain network name (`"ethereum"`, `"lukso"`, etc.)
- `params::Dict{String,Any}`: A map of key/value parameters required for the action.
- `priority::String`: The urgency level of the request. Must be one of:
  `"low"`, `"medium"`, `"high"`, `"urgent"`.

Example
-------
    req = BlockchainRequest(
        "transfer",
        "lukso",
        Dict("toAddress" => "0x123...", "token" => "LYX", "amount" => "100"),
        "high"
    )
"""
struct BlockchainRequest
    action::String
    chain::String
    params::Dict{String,Any}
    priority::String

    function BlockchainRequest(action, chain, params, priority="medium")
        valid_priorities = ["low", "medium", "high", "urgent"]
        if !(priority in valid_priorities)
            throw(ArgumentError("Priority must be one of: low, medium, high, urgent"))
        end
        new(action, chain, params, priority)
    end
end


# =========================
# = FACTORY METHODS =
# =========================

"""
    swap_request(; from_token, to_token, amount, chain="ethereum", slippage="0.5", priority="medium")

Creates a standardized blockchain swap request — e.g., swapping tokens on a DEX.

Keyword Arguments
-----------------
- `from_token::String`: Symbol or address of the token being swapped from.
- `to_token::String`: Symbol or address of the token being swapped to.
- `amount::String`: The token amount (as a string to avoid precision issues).
- `chain::String`: The blockchain network to execute on. Default: `"ethereum"`.
- `slippage::String`: Allowed slippage percentage. Default: `"0.5"`.
- `priority::String`: Request urgency level.

Returns
-------
A `BlockchainRequest` with `"swap"` as its action type.

Example
-------
    req = swap_request(
        from_token = "ETH",
        to_token   = "USDC",
        amount     = "1.5",
        chain      = "lukso",
        priority   = "high"
    )
"""
function swap_request(;
    from_token::String,
    to_token::String,
    amount::String,
    chain::String="ethereum",
    slippage::String="0.5",
    priority::String="medium"
)
    params = Dict{String,Any}(
        "fromToken" => from_token,
        "toToken"   => to_token,
        "amount"    => amount,
        "slippage"  => slippage
    )

    return BlockchainRequest("swap", chain, params, priority)
end


"""
    transfer_request(; to_address, token, amount, chain="ethereum", priority="medium")

Creates a standardized blockchain transfer request — e.g., sending tokens
between addresses.

Keyword Arguments
-----------------
- `to_address::String`: Recipient wallet address.
- `token::String`: Symbol or contract address of the token.
- `amount::String`: Token amount to transfer.
- `chain::String`: Target blockchain network. Default: `"ethereum"`.
- `priority::String`: Request urgency.

Example
-------
    req = transfer_request(
        to_address = "0xabc123...",
        token      = "LYX",
        amount     = "42",
        chain      = "lukso"
    )
"""
function transfer_request(;
    to_address::String,
    token::String,
    amount::String,
    chain::String="ethereum",
    priority::String="medium"
)
    params = Dict{String,Any}(
        "toAddress" => to_address,
        "token"     => token,
        "amount"    => amount
    )

    return BlockchainRequest("transfer", chain, params, priority)
end


"""
    approve_request(; token, spender, amount, chain="ethereum", priority="medium")

Creates a standardized blockchain approval request — typically used
before swaps to grant smart contracts permission to spend a token.

Keyword Arguments
-----------------
- `token::String`: Address or symbol of the token to approve.
- `spender::String`: Address of the contract or user to approve.
- `amount::String`: Token amount to approve.
- `chain::String`: Blockchain network to execute on. Default: `"ethereum"`.
- `priority::String`: Request urgency.

Example
-------
    req = approve_request(
        token   = "USDC",
        spender = "0xrouter...",
        amount  = "1000"
    )
"""
function approve_request(;
    token::String,
    spender::String,
    amount::String,
    chain::String="ethereum",
    priority::String="medium"
)
    params = Dict{String,Any}(
        "token"   => token,
        "spender" => spender,
        "amount"  => amount
    )

    return BlockchainRequest("approve", chain, params, priority)
end

end # module
