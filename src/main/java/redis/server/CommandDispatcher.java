package redis.server;

import redis.commands.Command;
import redis.commands.CommandResult;
import redis.config.ServerConfig;
import redis.protocol.RespValue;
import redis.replication.ReplicationManager;
import redis.storage.KeyValueStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CommandDispatcher {
    public enum Origin {
        CLIENT,
        REPLICA
    }

    private final ServerConfig config;
    private final KeyValueStore store;
    private final ReplicationManager replicationManager;

    public CommandDispatcher(ServerConfig config, KeyValueStore store, ReplicationManager replicationManager) {
        this.config = config;
        this.store = store;
        this.replicationManager = replicationManager;
    }

    public CommandResult dispatch(Command command, ConnectionContext context, Origin origin) throws IOException {
        if (command == null || command.isEmpty()) {
            return CommandResult.respond(RespValue.error("ERR unknown command"));
        }
        String name = command.getName();
        List<String> args = command.getArgs();

        if ("PING".equals(name)) {
            if (args.isEmpty()) {
                return CommandResult.respond(RespValue.simpleString("PONG"));
            }
            return CommandResult.respond(RespValue.bulkString(args.get(0)));
        }

        if ("ECHO".equals(name)) {
            if (args.isEmpty()) {
                return CommandResult.respond(RespValue.error("ERR wrong number of arguments for 'echo' command"));
            }
            return CommandResult.respond(RespValue.bulkString(args.get(0)));
        }

        if ("SET".equals(name)) {
            if (args.size() < 2) {
                return CommandResult.respond(RespValue.error("ERR wrong number of arguments for 'set' command"));
            }
            String key = args.get(0);
            String value = args.get(1);
            Long pxMillis = null;
            if (args.size() > 2) {
                for (int i = 2; i < args.size(); i++) {
                    String option = args.get(i).toUpperCase();
                    if ("PX".equals(option) && i + 1 < args.size()) {
                        try {
                            pxMillis = Long.parseLong(args.get(++i));
                        } catch (NumberFormatException e) {
                            return CommandResult.respond(RespValue.error("ERR PX value is not an integer"));
                        }
                    }
                }
            }
            store.set(key, value, pxMillis);
            if (origin == Origin.CLIENT && replicationManager != null) {
                replicationManager.propagate(command);
            }
            return CommandResult.respond(RespValue.simpleString("OK"));
        }

        if ("GET".equals(name)) {
            if (args.isEmpty()) {
                return CommandResult.respond(RespValue.error("ERR wrong number of arguments for 'get' command"));
            }
            String value = store.get(args.get(0));
            return CommandResult.respond(value == null ? RespValue.nullBulkString() : RespValue.bulkString(value));
        }

        if ("CONFIG".equals(name)) {
            return handleConfig(args);
        }

        if ("KEYS".equals(name)) {
            if (args.isEmpty()) {
                return CommandResult.respond(RespValue.emptyArray());
            }
            List<String> keys = store.keysMatching(args.get(0));
            List<RespValue> values = new ArrayList<RespValue>();
            for (String key : keys) {
                values.add(RespValue.bulkString(key));
            }
            return CommandResult.respond(RespValue.array(values));
        }

        if ("TYPE".equals(name)) {
            if (args.isEmpty()) {
                return CommandResult.respond(RespValue.simpleString("none"));
            }
            return CommandResult.respond(RespValue.simpleString(store.typeOf(args.get(0))));
        }

        if ("INFO".equals(name)) {
            return handleInfo(args);
        }

        if ("REPLCONF".equals(name)) {
            return CommandResult.respond(RespValue.simpleString("OK"));
        }

        if ("PSYNC".equals(name)) {
            if (replicationManager != null && replicationManager.getRole() == ReplicationManager.Role.MASTER) {
                if (context != null && context.getWriter() != null) {
                    replicationManager.sendFullResync(context.getWriter());
                    replicationManager.registerReplica(context.getWriter().getOutputStream());
                    context.markReplica();
                }
                return CommandResult.alreadyWritten();
            }
        }

        return CommandResult.respond(RespValue.error("ERR unknown command"));
    }

    private CommandResult handleConfig(List<String> args) {
        if (args.size() < 2 || !"GET".equalsIgnoreCase(args.get(0))) {
            return CommandResult.respond(RespValue.emptyArray());
        }
        String param = args.get(1).toLowerCase();
        List<RespValue> values = new ArrayList<RespValue>();
        if ("dir".equals(param)) {
            values.add(RespValue.bulkString("dir"));
            values.add(RespValue.bulkString(config.getDir()));
        } else if ("dbfilename".equals(param)) {
            values.add(RespValue.bulkString("dbfilename"));
            values.add(RespValue.bulkString(config.getDbFilename()));
        }
        return CommandResult.respond(values.isEmpty() ? RespValue.emptyArray() : RespValue.array(values));
    }

    private CommandResult handleInfo(List<String> args) {
        boolean includeReplication = args.isEmpty() || "replication".equalsIgnoreCase(args.get(0));
        if (!includeReplication) {
            return CommandResult.respond(RespValue.bulkString(""));
        }
        StringBuilder builder = new StringBuilder();
        builder.append("# Replication\r\n");
        if (replicationManager == null) {
            builder.append("role:master\r\n");
            builder.append("master_replid:0000000000000000000000000000000000000000\r\n");
            builder.append("master_repl_offset:0\r\n");
        } else if (replicationManager.getRole() == ReplicationManager.Role.MASTER) {
            builder.append("role:master\r\n");
            builder.append("master_replid:").append(replicationManager.getReplId()).append("\r\n");
            builder.append("master_repl_offset:").append(replicationManager.getReplOffset()).append("\r\n");
            builder.append("connected_slaves:").append(replicationManager.getConnectedReplicas()).append("\r\n");
            for (String line : replicationManager.getReplicaInfoLines()) {
                builder.append(line).append("\r\n");
            }
        } else {
            builder.append("role:slave\r\n");
            builder.append("master_host:").append(config.getReplicaOfHost()).append("\r\n");
            builder.append("master_port:").append(config.getReplicaOfPort()).append("\r\n");
            builder.append("master_link_status:up\r\n");
        }
        return CommandResult.respond(RespValue.bulkString(builder.toString()));
    }
}
