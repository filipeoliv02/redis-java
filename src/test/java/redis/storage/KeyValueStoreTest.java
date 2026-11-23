package redis.storage;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class KeyValueStoreTest {

    @Test
    public void expiredKeyIsRemovedAndReturnedAsNull() throws InterruptedException {
        KeyValueStore store = new KeyValueStore();
        store.set("temp", "value", 10L);
        Thread.sleep(30L);

        assertNull(store.get("temp"));
        assertEquals(0, store.keys().size());
    }

    @Test
    public void concurrentWritesAndReadsRemainConsistent() throws Exception {
        KeyValueStore store = new KeyValueStore();
        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();

        for (int i = 0; i < 500; i++) {
            final int value = i;
            futures.add(executor.submit(() -> {
                start.await();
                store.set("key-" + value, "value-" + value, null);
                return ("value-" + value).equals(store.get("key-" + value));
            }));
        }

        start.countDown();
        for (Future<Boolean> future : futures) {
            assertEquals(true, future.get());
        }
        executor.shutdown();
        assertEquals(500, store.keys().size());
    }
}
