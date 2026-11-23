package redis.protocol;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class RespProtocolTest {

    @Test
    public void roundTripSimpleTypes() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        RespWriter writer = new RespWriter(output);

        writer.writeSimpleString("OK");
        writer.writeInteger(5);
        writer.writeBulkString("hi");
        writer.writeBulkString(null);
        writer.flush();

        RespReader reader = new RespReader(new ByteArrayInputStream(output.toByteArray()));

        RespValue v1 = reader.readValue();
        assertEquals(RespType.SIMPLE_STRING, v1.getType());
        assertEquals("OK", v1.getStringValue());

        RespValue v2 = reader.readValue();
        assertEquals(RespType.INTEGER, v2.getType());
        assertEquals(Long.valueOf(5L), v2.getIntegerValue());

        RespValue v3 = reader.readValue();
        assertEquals(RespType.BULK_STRING, v3.getType());
        assertEquals("hi", v3.getStringValue());

        RespValue v4 = reader.readValue();
        assertEquals(RespType.BULK_STRING, v4.getType());
        assertNull(v4.getStringValue());
    }

    @Test
    public void rejectsOversizedBulkLength() throws IOException {
        String input = "$" + (RespReader.MAX_BULK_LENGTH + 1) + "\r\n";
        RespReader reader = new RespReader(new ByteArrayInputStream(input.getBytes("UTF-8")));
        try {
            reader.readValue();
            org.junit.Assert.fail("Expected oversized bulk string to be rejected");
        } catch (IOException expected) {
            assertEquals("RESP bulk string exceeds maximum length", expected.getMessage());
        }
    }

    @Test
    public void rejectsOversizedArray() throws IOException {
        String input = "*" + (RespReader.MAX_ARRAY_LENGTH + 1) + "\r\n";
        RespReader reader = new RespReader(new ByteArrayInputStream(input.getBytes("UTF-8")));
        try {
            reader.readValue();
            org.junit.Assert.fail("Expected oversized array to be rejected");
        } catch (IOException expected) {
            assertEquals("RESP array exceeds maximum length", expected.getMessage());
        }
    }

    @Test
    public void roundTripArray() throws IOException {
        List<RespValue> values = Arrays.asList(
                RespValue.bulkString("PING"),
                RespValue.bulkString("hello")
        );

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        RespWriter writer = new RespWriter(output);
        writer.writeArray(values);
        writer.flush();

        RespReader reader = new RespReader(new ByteArrayInputStream(output.toByteArray()));
        RespValue array = reader.readValue();

        assertEquals(RespType.ARRAY, array.getType());
        assertNotNull(array.getArrayValue());
        assertEquals(2, array.getArrayValue().size());
        assertEquals("PING", array.getArrayValue().get(0).getStringValue());
        assertEquals("hello", array.getArrayValue().get(1).getStringValue());
    }
}
