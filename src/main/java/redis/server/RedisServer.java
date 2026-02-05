package redis.server;

import redis.config.ServerConfig;
import redis.replication.ReplicationManager;
import redis.storage.KeyValueStore;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RedisServer {
    private final ServerConfig config;
    private final KeyValueStore store;
    private final ReplicationManager replicationManager;
    private final ExecutorService executor;

    public RedisServer(ServerConfig config) {
        this.config = config;
        this.store = new KeyValueStore();
        this.replicationManager = new ReplicationManager(config, store);
        this.executor = Executors.newCachedThreadPool();
    }

    public void start() throws IOException {
        if (config.isReplica()) {
            replicationManager.startReplicaSync(new ReplicationManager.ReplicaSyncListener() {
                @Override
                public void onReplicaConnected(Socket socket) {
                    // No-op: keep connection referenced by the client thread.
                }
            });
        }

        CommandDispatcher dispatcher = new CommandDispatcher(config, store, replicationManager);
        ServerSocket serverSocket = new ServerSocket(config.getPort());
        serverSocket.setReuseAddress(true);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            executor.submit(new ClientHandler(clientSocket, dispatcher));
        }
    }
}
