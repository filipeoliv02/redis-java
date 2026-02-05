package redis.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KeyValueStore {
    private final Map<String, ValueEntry> store = new ConcurrentHashMap<String, ValueEntry>();

    public void set(String key, String value, Long pxMillis) {
        long expireAt = 0L;
        if (pxMillis != null) {
            expireAt = System.currentTimeMillis() + pxMillis;
        }
        store.put(key, new ValueEntry(value, expireAt));
    }

    public String get(String key) {
        ValueEntry entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired(System.currentTimeMillis())) {
            store.remove(key);
            return null;
        }
        return entry.getValue();
    }

    public List<String> keys() {
        return new ArrayList<String>(store.keySet());
    }

    public boolean exists(String key) {
        return get(key) != null;
    }

    public String typeOf(String key) {
        return exists(key) ? "string" : "none";
    }

    public List<String> keysMatching(String pattern) {
        if (pattern == null || "*".equals(pattern)) {
            List<String> keys = keys();
            Collections.sort(keys);
            return keys;
        }
        return Collections.emptyList();
    }
}
