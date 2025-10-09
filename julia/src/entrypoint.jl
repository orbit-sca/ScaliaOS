#!/usr/bin/env julia

using JSON

try
    input = read(stdin, String)
    data = isempty(input) ? Dict("info" => "No input received") : JSON.parse(input)
    response = Dict("from" => "JuliaOS", "received" => data, "status" => "ok")
    println(JSON.json(response))
catch e
    println(JSON.json(Dict("error" => string(e))))
end