package redis.commands;

import redis.protocol.RespType;
import redis.protocol.RespValue;

import java.util.ArrayList;
import java.util.List;

public class CommandParser {
    public Command parse(RespValue value) {
        if (value == null) {
            return null;
        }
        if (value.getType() != RespType.ARRAY) {
            return new Command(new ArrayList<String>());
        }
        List<RespValue> array = value.getArrayValue();
        if (array == null) {
            return new Command(new ArrayList<String>());
        }
        List<String> parts = new ArrayList<String>();
        for (RespValue item : array) {
            if (item == null) {
                continue;
            }
            switch (item.getType()) {
                case BULK_STRING:
                case SIMPLE_STRING:
                case ERROR:
                    parts.add(item.getStringValue() == null ? "" : item.getStringValue());
                    break;
                case INTEGER:
                    parts.add(String.valueOf(item.getIntegerValue() == null ? 0L : item.getIntegerValue()));
                    break;
                default:
                    break;
            }
        }
        return new Command(parts);
    }
}
