package redis.storage;

public class ValueEntry {
    private final String value;
    private final long expireAtMillis;

    public ValueEntry(String value, long expireAtMillis) {
        this.value = value;
        this.expireAtMillis = expireAtMillis;
    }

    public String getValue() {
        return value;
    }

    public long getExpireAtMillis() {
        return expireAtMillis;
    }

    public boolean isExpired(long nowMillis) {
        return expireAtMillis > 0 && nowMillis >= expireAtMillis;
    }
}
