# Add src to load path
push!(LOAD_PATH, joinpath(@__DIR__, "..", "src"))

using JSON3

# Import our new modules
include("../src/framework/BlockchainRequests.jl")
include("../src/bridge/ResponseModels.jl")

using .BlockchainRequests
using .ResponseModels

"""
Simple v0.2 trading agent
- Takes market data as input
- Makes buy/hold decision
- Returns blockchain requests (doesn't execute)
"""
function run_trading_agent(input::Dict)
    # Extract input parameters
    token = get(input, "token", "ETH")
    action = get(input, "action", "analyze")
    price = get(input, "price", 2000.0)
    
    if action == "analyze"
        # Simple decision logic
        should_buy = price < 2000.0
        
        if should_buy
            # Create blockchain swap request
            blockchain_req = swap_request(
                from_token = "USDC",
                to_token = token,
                amount = "1000",
                chain = "ethereum",
                slippage = "0.5",
                priority = "medium"
            )
            
            # Return result with blockchain request
            result = AgentResult(
                Dict(
                    "decision" => "buy",
                    "token" => string(token),
                    "price" => string(price),  # Convert to string
                    "reason" => "Price below threshold (\$$(price) < \$2000)",
                    "amount" => "1000 USDC"
                ),
                [blockchain_req],  # Blockchain requests to execute
                0.75,              # Confidence level
                "Market conditions favorable for entry",  # Reasoning
                String[]           # No warnings
            )
        else
            # Hold decision - no blockchain requests
            result = AgentResult(
                Dict(
                    "decision" => "hold",
                    "token" => string(token),
                    "price" => string(price),  # Convert to string
                    "reason" => "Price above threshold (\$$(price) >= \$2000)"
                ),
                [],    # No blockchain requests
                0.90,  # High confidence
                "Waiting for better entry point",
                String[]
            )
        end
        
        return result
        
    elseif action == "transfer"
        # Example transfer action
        to_address = get(input, "to_address", "")
        amount = get(input, "amount", "100")
        
        if isempty(to_address)
            return AgentResult(
                Dict("error" => "Missing to_address"),
                [],
                0.0,
                nothing,
                ["to_address is required for transfer"]
            )
        end
        
        blockchain_req = transfer_request(
            to_address = to_address,
            token = token,
            amount = amount,
            chain = "ethereum"
        )
        
        result = AgentResult(
            Dict(
                "decision" => "transfer",
                "to_address" => string(to_address),
                "token" => string(token),
                "amount" => string(amount)
            ),
            [blockchain_req],
            1.0,
            "Transfer request validated",
            String[]
        )
        
        return result
    else
        # Unknown action
        result = AgentResult(
            Dict("error" => "Unknown action: $action"),
            [],
            0.0,
            nothing,
            ["Supported actions: analyze, transfer"]
        )
        return result
    end
end

"""
Main entry point - called by Scala via subprocess
"""
function main(args::Vector{String})
    try
        # Check for input
        if length(args) == 0
            error("No input provided. Usage: julia trading_agent.jl '{\"token\":\"ETH\",\"action\":\"analyze\"}'")
        end
        
        # Parse JSON input
        input = JSON3.read(args[1], Dict{String,Any})
        
        # Execute agent logic
        result = run_trading_agent(input)
        
        # Convert to dict and output as JSON
        output = to_dict(result)
        println(JSON3.write(output))
        
        exit(0)
    catch e
        # Output error as JSON
        error_result = Dict(
            "error" => true,
            "message" => string(e),
            "stacktrace" => sprint(showerror, e, catch_backtrace())
        )
        println(JSON3.write(error_result))
        exit(1)
    end
end

# Run if executed as script
if abspath(PROGRAM_FILE) == @__FILE__
    main(ARGS)
end