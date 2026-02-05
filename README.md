A lightweight Redis-compatible server written in Java.

Features
- RESP protocol parsing and serialization
- Core commands: PING, ECHO, SET, GET, KEYS, TYPE, CONFIG GET, INFO
- Optional key expiry with PX
- Basic master/replica handshake support

Testing
1. Install Maven (Java 8 compatible)
2. Run `mvn test`

Running
- Build and run with `./spawn_redis_server.sh`
- Optional flags: `--port`, `--dir`, `--dbfilename`, `--replicaof <host> <port>`
