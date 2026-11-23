package redis.server;

import redis.config.ServerConfig;
import redis.replication.ReplicationManager;
import redis.storage.KeyValueStore;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RedisServer {
    private final ServerConfig config;
    private final KeyValueStore store;
    private final ReplicationManager replicationManager;
    private final ExecutorService executor;

    public RedisServer(ServerConfig config) {
        this.config = config;
        this.store = new KeyValueStore();
        this.replicationManager = new ReplicationManager(config, store);
        this.executor = new ThreadPoolExecutor(
                8,
                128,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(256),
                new ThreadPoolExecutor.AbortPolicy());
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
            clientSocket.setSoTimeout(5 * 60 * 1000);
            try {
                executor.execute(new ClientHandler(clientSocket, dispatcher));
            } catch (java.util.concurrent.RejectedExecutionException e) {
                clientSocket.close();
            }
        }
    }
}
