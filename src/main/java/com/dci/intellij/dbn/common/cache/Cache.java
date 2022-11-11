package com.dci.intellij.dbn.common.cache;

import com.dci.intellij.dbn.common.routine.ThrowableCallable;
import com.dci.intellij.dbn.common.util.TimeUtil;
import com.dci.intellij.dbn.language.common.WeakRef;
import com.intellij.util.containers.ContainerUtil;
import lombok.SneakyThrows;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import static com.dci.intellij.dbn.common.util.Unsafe.cast;

/**
 * Dynamic multi level cache
 */
public class Cache {
    private final Map<String, Map<String, ?>> data = new ConcurrentHashMap<>(30);
    private final long expiryMillis;


    public Cache(long expiryMillis) {
        this.expiryMillis = expiryMillis;
        CACHE_CLEANUP_TASK.register(this);
    }

    private boolean isValid(CacheValue cacheValue) {
        return cacheValue != null && !cacheValue.isOlderThan(expiryMillis);
    }

    private void cleanup() {
        cleanup(data);
    }

    private void cleanup(Object data) {
        if (data instanceof Map) {
            Map<String, ?> cache = cast(data);
            for (String key : cache.keySet()) {
                Object value = cache.get(key);
                if (value instanceof CacheValue) {
                    CacheValue cacheValue = (CacheValue) value;
                    if (!isValid(cacheValue)) {
                        cache.remove(key);
                    }
                } else {
                    cleanup(cast(value));
                }
            }
        }
    }

    public void reset() {
        data.clear();
    }

    public <T, E extends Throwable> T get(CacheKey<T> key, ThrowableCallable<T, E> loader) throws E {
        Map<String, CacheValue<T>> elements = elements(key);
        CacheValue<T> cacheValue = elements.compute(key.getKey(), (k, v) -> {
            if (!isValid(v)) {
                T value = load(loader);
                v = new CacheValue<T>(value);
            }
            return v;
        });
        return cacheValue.getValue();
    }

    private <T> Map<String, CacheValue<T>> elements(CacheKey<T> key) {
        Map<String, Map<String, ?>> cache = data;
        String[] path = key.getPath();
        for (String token : path) {
            cache = cast(cache.computeIfAbsent(token, k -> new ConcurrentHashMap<>()));
        }

        return cast(cache);
    }

    @SneakyThrows
    private <T, E extends Throwable> T load(ThrowableCallable<T, E> loader) {
        return loader.call();
    }

    private static class ConnectionCacheCleanupTask extends TimerTask {
        List<WeakRef<Cache>> caches = ContainerUtil.createConcurrentList();

        @Override
        public void run() {
            for (WeakRef<Cache> cacheRef : caches) {
                Cache cache = cacheRef.get();
                if (cache == null) {
                    caches.remove(cacheRef);
                } else {
                    cache.cleanup();
                }
            }
        }

        void register(Cache cache) {
            caches.add(WeakRef.of(cache));
        }
    }

    private static final ConnectionCacheCleanupTask CACHE_CLEANUP_TASK = new ConnectionCacheCleanupTask();
    static {
        Timer poolCleaner = new Timer("DBN - Connection Cache Cleaner");
        poolCleaner.schedule(CACHE_CLEANUP_TASK, TimeUtil.Millis.THREE_MINUTES, TimeUtil.Millis.THREE_MINUTES);
    }
}
