package redis.server;

import org.junit.Test;
import redis.commands.Command;
import redis.commands.CommandResult;
import redis.config.ServerConfig;
import redis.protocol.RespType;
import redis.protocol.RespValue;
import redis.storage.KeyValueStore;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CommandDispatcherTest {

    @Test
    public void pingRespondsPong() throws IOException {
        CommandDispatcher dispatcher = newDispatcher();
        CommandResult result = dispatcher.dispatch(
                new Command(Arrays.asList("PING")),
                new ConnectionContext(null, null, false),
                CommandDispatcher.Origin.CLIENT
        );
        assertEquals(RespType.SIMPLE_STRING, result.getResponse().getType());
        assertEquals("PONG", result.getResponse().getStringValue());
    }

    @Test
    public void echoReturnsMessage() throws IOException {
        CommandDispatcher dispatcher = newDispatcher();
        CommandResult result = dispatcher.dispatch(
                new Command(Arrays.asList("ECHO", "hello")),
                new ConnectionContext(null, null, false),
                CommandDispatcher.Origin.CLIENT
        );
        assertEquals(RespType.BULK_STRING, result.getResponse().getType());
        assertEquals("hello", result.getResponse().getStringValue());
    }

    @Test
    public void setAndGet() throws IOException {
        CommandDispatcher dispatcher = newDispatcher();
        dispatcher.dispatch(
                new Command(Arrays.asList("SET", "name", "redis")),
                new ConnectionContext(null, null, false),
                CommandDispatcher.Origin.CLIENT
        );

        CommandResult result = dispatcher.dispatch(
                new Command(Arrays.asList("GET", "name")),
                new ConnectionContext(null, null, false),
                CommandDispatcher.Origin.CLIENT
        );

        assertEquals(RespType.BULK_STRING, result.getResponse().getType());
        assertEquals("redis", result.getResponse().getStringValue());
    }

    @Test
    public void setWithExpiryExpires() throws IOException, InterruptedException {
        CommandDispatcher dispatcher = newDispatcher();
        dispatcher.dispatch(
                new Command(Arrays.asList("SET", "temp", "value", "PX", "30")),
                new ConnectionContext(null, null, false),
                CommandDispatcher.Origin.CLIENT
        );

        Thread.sleep(50);

        CommandResult result = dispatcher.dispatch(
                new Command(Arrays.asList("GET", "temp")),
                new ConnectionContext(null, null, false),
                CommandDispatcher.Origin.CLIENT
        );

        assertEquals(RespType.BULK_STRING, result.getResponse().getType());
        assertNull(result.getResponse().getStringValue());
    }

    private CommandDispatcher newDispatcher() {
        ServerConfig config = new ServerConfig(6379, System.getProperty("user.dir"), "dump.rdb", null, null);
        KeyValueStore store = new KeyValueStore();
        return new CommandDispatcher(config, store, null);
    }
}
