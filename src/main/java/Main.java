import redis.config.ServerConfig;
import redis.server.RedisServer;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");

        ServerConfig config = ServerConfig.fromArgs(args);
        RedisServer server = new RedisServer(config);
        try {
            server.start();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}
