using Pkg
Pkg.activate(joinpath(@__DIR__, ".."))

using Base64
using Logging
global_logger(SimpleLogger(stderr, Logging.Info))
using DotEnv

# -------------------------------
# 5️⃣ Load JuliaOS framework and JSON
# -------------------------------
# Add the `src` folder to LOAD_PATH
push!(LOAD_PATH, joinpath(@__DIR__, "..", "src"))
include("../src/JuliaOS.jl")
using .JuliaOS
using JSON
using JuliaOS.JuliaOSFramework

# ----------------- Load .env -----------------
env_file = abspath(joinpath(@__DIR__, "..", ".env"))
@info "Loading .env from: $env_file"
if isfile(env_file)
    for line in eachline(env_file)
        line = strip(line)
        if isempty(line) || startswith(line, "#")
            continue
        end
        if contains(line, "=")
            parts = split(line, "=", limit=2)
            if length(parts) == 2
                key = strip(parts[1])
                value = strip(parts[2])
                ENV[key] = value
            end
        end
    end
    @info "Parsed .env file"
else
    @error ".env file not found"
end

# -------------------------------
# Verify essential environment variables
# -------------------------------
@info "OPENAI_API_KEY loaded: $(haskey(ENV, "OPENAI_API_KEY") && !isempty(get(ENV, "OPENAI_API_KEY", "")))"

# ----------------- Helper: recursive convert to Dict{String, Any} -----------------
function convert_any_recursive(x)
    if isa(x, Dict)
        return Dict{String, Any}(k => convert_any_recursive(v) for (k,v) in x)
    elseif isa(x, Vector)
        return [convert_any_recursive(v) for v in x]
    else
        return x
    end
end

# ----------------- Helper: extract Base64 JSON from ARGS -----------------
function extract_input_json(args::Vector{String})
    for arg in args
        if startswith(arg, "input=")
            s = replace(arg, "input=" => "")
            # Base64 decode
            try
                decoded = String(base64decode(s))
                @info "Decoded Base64 input successfully"
                return decoded
            catch e
                @warn "Base64 decode failed, trying as plain JSON" exception=e
                # Fallback to old behavior for backwards compatibility
                if startswith(s, "'") && endswith(s, "'")
                    s = s[2:end-1]
                elseif startswith(s, "\"") && endswith(s, "\"")
                    s = s[2:end-1]
                end
                return s
            end
        end
    end
    return "{}"
end

# ----------------- Main function -----------------
function main(args::Vector{String})
    @info "ARGS received: $args"

    input_json = extract_input_json(args)
    @info "Extracted JSON: $input_json"
    input_data = try
        JSON.parse(input_json)
    catch e
        @error "Failed to parse JSON: $e"
        Dict{String, Any}()
    end

    @info "Parsed input_data: $input_data"

    # ----------------- LLM configuration -----------------
    llm_config = Dict{String, Any}(
        "provider" => "openai",
        "api_key"  => get(ENV, "OPENAI_API_KEY", ""),
        "api_base" => get(ENV, "OPENAI_BASE_URL", "https://api.groq.com/openai/v1"),
        "model"    => get(ENV, "OPENAI_MODEL", "llama3-8b-8192"),
        "temperature" => 0.7,
        "max_tokens"  => 8092,
        "stream"      => false
    )

    # ----------------- Agent configuration -----------------
    cfg = JuliaOS.JuliaOSFramework.AgentCore.AgentConfig(
        "BasicAgent",
        JuliaOS.JuliaOSFramework.AgentCore.CUSTOM;
        abilities=["ping", "llm_chat"],
        chains=String[],
        parameters=Dict{String, Any}("demo" => true),
        llm_config=llm_config,
        memory_config=Dict{String, Any}("type" => "ordered_dict", "max_size" => 1000), # Example memory config ^
        queue_config=Dict{String, Any}("type" => "priority_queue") # Example queue config ^
    )

    # ----------------- Create and start agent -----------------
    agent = JuliaOS.JuliaOSFramework.Agents.createAgent(cfg)
    JuliaOS.JuliaOSFramework.Agents.startAgent(agent.id)

    waited = 0.0
    while agent.status != JuliaOS.JuliaOSFramework.AgentCore.RUNNING && waited < 10
        sleep(0.1)
        waited += 0.1
        agent = JuliaOS.JuliaOSFramework.AgentCore.AGENTS[agent.id]
    end
    if agent.status != JuliaOS.JuliaOSFramework.AgentCore.RUNNING
        @error "Agent failed to start. Status: $(agent.status)"
    end

    # ----------------- Execute ability -----------------
    ability = get(input_data, "ability", "ping")
    prompt  = get(input_data, "prompt", "")

    # Force Dict{String, Any} at top level, then recursively convert nested dicts
    task_dict = Dict{String, Any}("ability" => ability)
    if ability == "llm_chat"
        task_dict["prompt"] = prompt
    end

    result = JuliaOS.JuliaOSFramework.Agents.executeAgentTask(agent.id, convert_any_recursive(task_dict))

    # ----------------- Simplify output -----------------
    simple_result = Dict(
        "ability" => ability,
        "message" => begin
            if ability == "ping"
                "pong"
            elseif ability == "llm_chat"
                if isa(result, Dict) && haskey(result, "response") && haskey(result["response"], "content")
                    result["response"]["content"]
                else
                    string(result)
                end
            else
                "unknown ability"
            end
        end,
        "status" => "success"
    )

    println(JSON.json(Dict(
        "output" => simple_result,
        "blockchainRequests" => [],
        "warnings" => [],
        "confidence" => 1.0,
        "reasoning" => "Agent task completed: $ability"
    )))

    JuliaOS.JuliaOSFramework.Agents.stopAgent(agent.id)
end

if abspath(PROGRAM_FILE) == @__FILE__
    main(ARGS)
end

