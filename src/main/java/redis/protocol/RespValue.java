package redis.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RespValue {
    private final RespType type;
    private final String stringValue;
    private final Long integerValue;
    private final List<RespValue> arrayValue;

    private RespValue(RespType type, String stringValue, Long integerValue, List<RespValue> arrayValue) {
        this.type = type;
        this.stringValue = stringValue;
        this.integerValue = integerValue;
        this.arrayValue = arrayValue;
    }

    public static RespValue simpleString(String value) {
        return new RespValue(RespType.SIMPLE_STRING, value, null, null);
    }

    public static RespValue error(String value) {
        return new RespValue(RespType.ERROR, value, null, null);
    }

    public static RespValue integer(long value) {
        return new RespValue(RespType.INTEGER, null, value, null);
    }

    public static RespValue bulkString(String value) {
        return new RespValue(RespType.BULK_STRING, value, null, null);
    }

    public static RespValue nullBulkString() {
        return new RespValue(RespType.BULK_STRING, null, null, null);
    }

    public static RespValue array(List<RespValue> values) {
        List<RespValue> safe = values == null ? null : new ArrayList<RespValue>(values);
        return new RespValue(RespType.ARRAY, null, null, safe);
    }

    public static RespValue emptyArray() {
        return new RespValue(RespType.ARRAY, null, null, Collections.<RespValue>emptyList());
    }

    public RespType getType() {
        return type;
    }

    public String getStringValue() {
        return stringValue;
    }

    public Long getIntegerValue() {
        return integerValue;
    }

    public List<RespValue> getArrayValue() {
        return arrayValue == null ? null : Collections.unmodifiableList(arrayValue);
    }
}
