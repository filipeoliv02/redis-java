package redis.server;

import redis.commands.Command;
import redis.commands.CommandParser;
import redis.commands.CommandResult;
import redis.protocol.RespReader;
import redis.protocol.RespValue;
import redis.protocol.RespWriter;

import java.io.IOException;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final CommandDispatcher dispatcher;

    public ClientHandler(Socket socket, CommandDispatcher dispatcher) {
        this.socket = socket;
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        try {
            RespReader reader = new RespReader(socket.getInputStream());
            RespWriter writer = new RespWriter(socket.getOutputStream());
            CommandParser parser = new CommandParser();
            ConnectionContext context = new ConnectionContext(socket, writer, false);

            while (true) {
                RespValue value = reader.readValue();
                if (value == null) {
                    break;
                }
                Command command = parser.parse(value);
                if (command == null || command.isEmpty()) {
                    continue;
                }
                CommandResult result = dispatcher.dispatch(command, context, CommandDispatcher.Origin.CLIENT);
                if (result == null) {
                    continue;
                }
                if (!result.isResponseWritten()) {
                    writer.writeValue(result.getResponse());
                    writer.flush();
                }
            }
        } catch (IOException e) {
            // Client disconnected.
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
