package redis.config;

import java.util.Objects;

public class ServerConfig {
    private final int port;
    private final String dir;
    private final String dbFilename;
    private final String replicaOfHost;
    private final Integer replicaOfPort;

    public ServerConfig(int port, String dir, String dbFilename, String replicaOfHost, Integer replicaOfPort) {
        this.port = port;
        this.dir = dir;
        this.dbFilename = dbFilename;
        this.replicaOfHost = replicaOfHost;
        this.replicaOfPort = replicaOfPort;
    }

    public int getPort() {
        return port;
    }

    public String getDir() {
        return dir;
    }

    public String getDbFilename() {
        return dbFilename;
    }

    public String getReplicaOfHost() {
        return replicaOfHost;
    }

    public Integer getReplicaOfPort() {
        return replicaOfPort;
    }

    public boolean isReplica() {
        return replicaOfHost != null && replicaOfPort != null;
    }

    public static ServerConfig fromArgs(String[] args) {
        int port = 6379;
        String dir = null;
        String dbFilename = null;
        String replicaOfHost = null;
        Integer replicaOfPort = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--port".equals(arg) && i + 1 < args.length) {
                port = Integer.parseInt(args[++i]);
            } else if ("--dir".equals(arg) && i + 1 < args.length) {
                dir = args[++i];
            } else if ("--dbfilename".equals(arg) && i + 1 < args.length) {
                dbFilename = args[++i];
            } else if ("--replicaof".equals(arg) && i + 2 < args.length) {
                replicaOfHost = args[++i];
                replicaOfPort = Integer.parseInt(args[++i]);
            }
        }

        if (dir == null) {
            dir = System.getProperty("user.dir");
        }
        if (dbFilename == null) {
            dbFilename = "dump.rdb";
        }

        return new ServerConfig(port, dir, dbFilename, replicaOfHost, replicaOfPort);
    }

    @Override
    public String toString() {
        return "ServerConfig{" +
                "port=" + port +
                ", dir='" + dir + '\'' +
                ", dbFilename='" + dbFilename + '\'' +
                ", replicaOfHost='" + replicaOfHost + '\'' +
                ", replicaOfPort=" + replicaOfPort +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServerConfig that = (ServerConfig) o;
        return port == that.port &&
                Objects.equals(dir, that.dir) &&
                Objects.equals(dbFilename, that.dbFilename) &&
                Objects.equals(replicaOfHost, that.replicaOfHost) &&
                Objects.equals(replicaOfPort, that.replicaOfPort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(port, dir, dbFilename, replicaOfHost, replicaOfPort);
    }
}
