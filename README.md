# JSONRPC Server

JSON-RPC server using reflection based on Servlet.

# Run

Server

    gradle appRunWar

Client

Method: rpc/service/Calculator.multiply

    curl -H "Content-Type:application/json" \
    -d '{"id":"1","method":"Calculator.multiply","params":[1,2]}' \
    http://localhost:8080/json-rpc-server-by-reflection/rpc

OutPut

    {"result":2,"id":"1","jsonrpc":"2.0"}

Method: rpc/service/Misc.now

    curl -H "Content-Type:application/json" \
    -d '{"id":"1","method":"Misc.now"}' \
    http://localhost:8080/json-rpc-server-by-reflection/rpc

OutPut

    {"result":"2020-01-06T08:04:01.955Z","id":"1","jsonrpc":"2.0"}

# Ref

* https://gist.github.com/jcubic/7887543
* https://guides.gradle.org/building-java-web-applications/
* https://github.com/briandilley/jsonrpc4j/blob/master/README.md
* https://solarcitynotes.blogspot.com/p/json-rpc.html
* https://www.jsonrpc.org/specification