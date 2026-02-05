package redis.replication;

import redis.commands.Command;
import redis.config.ServerConfig;
import redis.protocol.RespValue;
import redis.protocol.RespWriter;
import redis.storage.KeyValueStore;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ReplicationManager {
    public enum Role {
        MASTER,
        SLAVE
    }

    private final ServerConfig config;
    private final KeyValueStore store;
    private final Role role;
    private final List<ReplicaConnection> replicas = new CopyOnWriteArrayList<ReplicaConnection>();
    private final String replId;
    private volatile long replOffset;

    public ReplicationManager(ServerConfig config, KeyValueStore store) {
        this.config = config;
        this.store = store;
        this.role = config.isReplica() ? Role.SLAVE : Role.MASTER;
        this.replId = "0000000000000000000000000000000000000000";
        this.replOffset = 0L;
    }

    public Role getRole() {
        return role;
    }

    public String getReplId() {
        return replId;
    }

    public long getReplOffset() {
        return replOffset;
    }

    public int getConnectedReplicas() {
        return replicas.size();
    }

    public void registerReplica(OutputStream outputStream) {
        replicas.add(new ReplicaConnection(outputStream));
    }

    public void propagate(Command command) {
        if (role != Role.MASTER) {
            return;
        }
        if (command == null || command.isEmpty()) {
            return;
        }
        List<RespValue> values = new ArrayList<RespValue>();
        for (String part : command.getParts()) {
            values.add(RespValue.bulkString(part));
        }
        RespValue payload = RespValue.array(values);
        for (ReplicaConnection replica : replicas) {
            try {
                replica.getWriter().writeValue(payload);
                replica.getWriter().flush();
            } catch (IOException e) {
                replicas.remove(replica);
            }
        }
    }

    public void sendFullResync(RespWriter writer) throws IOException {
        writer.writeSimpleString("FULLRESYNC " + replId + " " + replOffset);
        writer.flush();
        writeEmptyRdb(writer);
    }

    private void writeEmptyRdb(RespWriter writer) throws IOException {
        byte[] rdb = RdbPayloads.emptyRdb();
        writer.writeRaw("$" + rdb.length + "\r\n");
        writer.getOutputStream().write(rdb);
        writer.writeRaw("\r\n");
        writer.flush();
    }

    public void startReplicaSync(ReplicaSyncListener listener) {
        if (role != Role.SLAVE) {
            return;
        }
        ReplicaSyncClient client = new ReplicaSyncClient(config, store, listener);
        Thread thread = new Thread(client, "replica-sync");
        thread.setDaemon(true);
        thread.start();
    }

    public List<String> getReplicaInfoLines() {
        if (role != Role.MASTER) {
            return Collections.emptyList();
        }
        List<String> lines = new ArrayList<String>();
        int index = 0;
        for (ReplicaConnection replica : replicas) {
            lines.add("slave" + index + ":ip=unknown,port=0,state=online,offset=" + replOffset + ",lag=0");
            index++;
        }
        return lines;
    }

    private static class ReplicaConnection {
        private final RespWriter writer;

        ReplicaConnection(OutputStream outputStream) {
            this.writer = new RespWriter(outputStream);
        }

        public RespWriter getWriter() {
            return writer;
        }
    }

    public interface ReplicaSyncListener {
        void onReplicaConnected(Socket socket);
    }
}
