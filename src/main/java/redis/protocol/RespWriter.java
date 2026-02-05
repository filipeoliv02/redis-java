package redis.protocol;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class RespWriter {
    private final OutputStream output;

    public RespWriter(OutputStream output) {
        this.output = output;
    }

    public void writeValue(RespValue value) throws IOException {
        if (value == null) {
            return;
        }
        switch (value.getType()) {
            case SIMPLE_STRING:
                writeSimpleString(value.getStringValue());
                break;
            case ERROR:
                writeError(value.getStringValue());
                break;
            case INTEGER:
                writeInteger(value.getIntegerValue() == null ? 0L : value.getIntegerValue());
                break;
            case BULK_STRING:
                writeBulkString(value.getStringValue());
                break;
            case ARRAY:
                writeArray(value.getArrayValue());
                break;
            default:
                break;
        }
    }

    public void writeSimpleString(String value) throws IOException {
        writeRaw("+" + (value == null ? "" : value) + "\r\n");
    }

    public void writeError(String value) throws IOException {
        writeRaw("-" + (value == null ? "ERR" : value) + "\r\n");
    }

    public void writeInteger(long value) throws IOException {
        writeRaw(":" + value + "\r\n");
    }

    public void writeBulkString(String value) throws IOException {
        if (value == null) {
            writeRaw("$-1\r\n");
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeRaw("$" + bytes.length + "\r\n");
        output.write(bytes);
        writeRaw("\r\n");
    }

    public void writeArray(List<RespValue> values) throws IOException {
        if (values == null) {
            writeRaw("*-1\r\n");
            return;
        }
        writeRaw("*" + values.size() + "\r\n");
        for (RespValue value : values) {
            writeValue(value);
        }
    }

    public void flush() throws IOException {
        output.flush();
    }

    public void writeRaw(String value) throws IOException {
        output.write(value.getBytes(StandardCharsets.UTF_8));
    }

    public OutputStream getOutputStream() {
        return output;
    }
}
