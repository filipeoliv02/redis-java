package redis.replication;

import redis.commands.Command;
import redis.commands.CommandParser;
import redis.commands.CommandResult;
import redis.config.ServerConfig;
import redis.protocol.RespReader;
import redis.protocol.RespValue;
import redis.protocol.RespWriter;
import redis.server.CommandDispatcher;
import redis.server.ConnectionContext;
import redis.storage.KeyValueStore;

import java.io.IOException;
import java.net.Socket;

public class ReplicaSyncClient implements Runnable {
    private final ServerConfig config;
    private final KeyValueStore store;
    private final ReplicationManager.ReplicaSyncListener listener;

    public ReplicaSyncClient(ServerConfig config, KeyValueStore store, ReplicationManager.ReplicaSyncListener listener) {
        this.config = config;
        this.store = store;
        this.listener = listener;
    }

    @Override
    public void run() {
        Socket socket = null;
        try {
            socket = new Socket(config.getReplicaOfHost(), config.getReplicaOfPort());
            if (listener != null) {
                listener.onReplicaConnected(socket);
            }
            RespWriter writer = new RespWriter(socket.getOutputStream());
            RespReader reader = new RespReader(socket.getInputStream());
            CommandParser parser = new CommandParser();
            CommandDispatcher dispatcher = new CommandDispatcher(config, store, null);

            sendArray(writer, "PING");
            reader.readValue();

            sendArray(writer, "REPLCONF", "listening-port", String.valueOf(config.getPort()));
            reader.readValue();

            sendArray(writer, "REPLCONF", "capa", "psync2");
            reader.readValue();

            sendArray(writer, "PSYNC", "?", "-1");
            reader.readValue();
            readRdbPayload(reader);

            while (true) {
                RespValue value = reader.readValue();
                if (value == null) {
                    break;
                }
                Command command = parser.parse(value);
                if (command == null || command.isEmpty()) {
                    continue;
                }
                ConnectionContext context = new ConnectionContext(null, null, true);
                CommandResult result = dispatcher.dispatch(command, context, CommandDispatcher.Origin.REPLICA);
                if (result != null && !result.isResponseWritten() && result.getResponse() != null) {
                    // Master does not expect replies for propagated commands; ignore.
                }
            }
        } catch (IOException e) {
            // Ignore connection failures for now.
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void sendArray(RespWriter writer, String... parts) throws IOException {
        RespValue[] values = new RespValue[parts.length];
        for (int i = 0; i < parts.length; i++) {
            values[i] = RespValue.bulkString(parts[i]);
        }
        writer.writeValue(RespValue.array(java.util.Arrays.asList(values)));
        writer.flush();
    }

    private void readRdbPayload(RespReader reader) throws IOException {
        RespValue rdb = reader.readValue();
        // RDB payload arrives as a bulk string; ignore contents.
        if (rdb == null) {
            return;
        }
    }
}
