# redis-java

A lightweight Redis-compatible server written in Java.

## Requirements

- JDK 21 or newer (Java 21 is the baseline)
- Apache Maven 3.9+

The source release has no maintained Java 8 compatibility. Earlier Java releases should use an older release of this project; Java 21 is preferred for builds and deployments.

## Build and run

```bash
mvn clean test
./spawn_redis_server.sh
```

Optional server flags:

- `--port <port>`
- `--dir <directory>`
- `--dbfilename <filename>`
- `--replicaof <host> <port>`

## Supported commands

- `PING [message]`
- `ECHO <message>`
- `SET <key> <value> [PX <milliseconds>]`
- `GET <key>`
- `KEYS <pattern>` (only `*` is supported)
- `TYPE <key>`
- `CONFIG GET <dir|dbfilename>`
- `INFO [replication]`
- `REPLCONF ...`
- `PSYNC ? -1`

Inline RESP commands and RESP arrays/bulk strings are accepted. RESP stream input is bounded to 64 KiB per line, 16 MiB per bulk string, and 1024 array elements.

## Replication and limitations

- Master/replica handshake and propagation of `SET` are implemented.
- The master sends an empty RDB payload during `PSYNC` and tracks replica connections in memory.
- Persistence/loading, transactions, pub/sub, ACLs, cluster mode, authentication, and the complete Redis command set are not implemented.
- `KEYS` supports only `*`; patterns and full Redis glob matching are not implemented.
- Replication is best-effort; it does not implement replication backlog, partial resynchronization, failover, or durable offset recovery.
- This project is intended for learning and small protocol exercises, not production use.

## Security and resource limits

Client sockets have idle timeouts, client handling is served by a bounded thread pool, and RESP parser inputs are size-limited. These limits are a basic guard against malformed or resource-exhausting connections.

## License

MIT. See `LICENSE`.
