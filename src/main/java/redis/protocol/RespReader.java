package redis.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RespReader {
    private final PushbackInputStream input;

    public RespReader(InputStream input) {
        this.input = new PushbackInputStream(input, 1);
    }

    public RespValue readValue() throws IOException {
        int first = input.read();
        if (first == -1) {
            return null;
        }
        char prefix = (char) first;
        switch (prefix) {
            case '+':
                return RespValue.simpleString(readLine());
            case '-':
                return RespValue.error(readLine());
            case ':':
                return RespValue.integer(parseLong(readLine()));
            case '$':
                return readBulkString();
            case '*':
                return readArray();
            default:
                return readInline(prefix);
        }
    }

    private RespValue readInline(char firstChar) throws IOException {
        String line = readLineStartingWith(firstChar);
        String[] parts = line.trim().split("\\s+");
        List<RespValue> values = new ArrayList<RespValue>();
        for (String part : parts) {
            if (part.length() == 0) {
                continue;
            }
            values.add(RespValue.bulkString(part));
        }
        return RespValue.array(values);
    }

    private RespValue readBulkString() throws IOException {
        int length = parseInt(readLine());
        if (length == -1) {
            return RespValue.nullBulkString();
        }
        byte[] data = readBytes(length);
        readExpectedCrlf();
        return RespValue.bulkString(new String(data, StandardCharsets.UTF_8));
    }

    private RespValue readArray() throws IOException {
        int count = parseInt(readLine());
        if (count == -1) {
            return RespValue.array(null);
        }
        List<RespValue> values = new ArrayList<RespValue>(count);
        for (int i = 0; i < count; i++) {
            RespValue value = readValue();
            if (value == null) {
                break;
            }
            values.add(value);
        }
        return RespValue.array(values);
    }

    private String readLineStartingWith(char firstChar) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        buffer.write((byte) firstChar);
        return readLineInto(buffer);
    }

    private String readLine() throws IOException {
        return readLineInto(new ByteArrayOutputStream());
    }

    private String readLineInto(ByteArrayOutputStream buffer) throws IOException {
        int b;
        while ((b = input.read()) != -1) {
            if (b == '\r') {
                int next = input.read();
                if (next == '\n') {
                    break;
                }
                buffer.write(b);
                if (next != -1) {
                    buffer.write(next);
                }
                continue;
            }
            buffer.write(b);
        }
        return buffer.toString(StandardCharsets.UTF_8.name());
    }

    private byte[] readBytes(int length) throws IOException {
        byte[] data = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = input.read(data, offset, length - offset);
            if (read == -1) {
                break;
            }
            offset += read;
        }
        if (offset != length) {
            byte[] truncated = new byte[offset];
            System.arraycopy(data, 0, truncated, 0, offset);
            return truncated;
        }
        return data;
    }

    private void readExpectedCrlf() throws IOException {
        int cr = input.read();
        int lf = input.read();
        if (cr != '\r' || lf != '\n') {
            if (lf != -1) {
                input.unread(lf);
            }
            if (cr != -1) {
                input.unread(cr);
            }
        }
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
