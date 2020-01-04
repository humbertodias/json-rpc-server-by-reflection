# JSONRPC Server

JSON-RPC server using reflection based on Servlet.

# Run

Server

    gradle appRunWar

Client

    curl -H "Content-Type:application/json" \
    -d '{"id":"1","jsonrpc":"2.0","method":"Calculator.multiplier", "params": [1,2]}' \
    http://localhost:8080/reflection-json-rpc-server/rpc

# Ref

* https://gist.github.com/jcubic/7887543
* https://guides.gradle.org/building-java-web-applications/
* https://github.com/briandilley/jsonrpc4j/blob/master/README.md
* https://solarcitynotes.blogspot.com/p/json-rpc.html
* https://www.jsonrpc.org/specification