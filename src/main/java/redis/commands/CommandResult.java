package redis.commands;

import redis.protocol.RespValue;

public class CommandResult {
    private final RespValue response;
    private final boolean responseWritten;

    private CommandResult(RespValue response, boolean responseWritten) {
        this.response = response;
        this.responseWritten = responseWritten;
    }

    public static CommandResult respond(RespValue value) {
        return new CommandResult(value, false);
    }

    public static CommandResult alreadyWritten() {
        return new CommandResult(null, true);
    }

    public RespValue getResponse() {
        return response;
    }

    public boolean isResponseWritten() {
        return responseWritten;
    }
}
