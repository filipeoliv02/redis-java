package redis.server;

import redis.protocol.RespWriter;

import java.net.Socket;

public class ConnectionContext {
    private final Socket socket;
    private final RespWriter writer;
    private boolean replica;

    public ConnectionContext(Socket socket, RespWriter writer, boolean replica) {
        this.socket = socket;
        this.writer = writer;
        this.replica = replica;
    }

    public Socket getSocket() {
        return socket;
    }

    public RespWriter getWriter() {
        return writer;
    }

    public boolean isReplica() {
        return replica;
    }

    public void markReplica() {
        this.replica = true;
    }
}
