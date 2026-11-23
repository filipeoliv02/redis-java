package redis.replication;

import org.junit.Test;
import redis.commands.Command;
import redis.config.ServerConfig;
import redis.storage.KeyValueStore;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReplicationManagerTest {

    @Test
    public void masterPropagatesSetToRegisteredReplica() {
        ServerConfig config = new ServerConfig(6379, ".", "dump.rdb", null, null);
        ReplicationManager manager = new ReplicationManager(config, new KeyValueStore());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        manager.registerReplica(output);

        manager.propagate(new Command(Arrays.asList("SET", "name", "redis")));

        String propagated = output.toString();
        assertTrue(propagated.contains("SET"));
        assertTrue(propagated.contains("name"));
        assertTrue(propagated.contains("redis"));
    }

    @Test
    public void replicaRoleReportsSlaveMetadata() {
        ServerConfig config = new ServerConfig(6380, ".", "dump.rdb", "127.0.0.1", 6379);
        ReplicationManager manager = new ReplicationManager(config, new KeyValueStore());

        assertEquals(ReplicationManager.Role.SLAVE, manager.getRole());
        assertEquals(0, manager.getConnectedReplicas());
    }
}
