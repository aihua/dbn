package com.dci.intellij.dbn.common.collections;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Specialised concurrent HashMap
 * Supports nullable values while inheriting the optimised synchronisation logic of {@link ConcurrentHashMap}
 * Wraps values with {@link Optional} 
 *
 * @param <K>
 * @param <V>
 */
public class ConcurrentOptionalValueMap<K, V> implements Map<K, V> {

    private final Map<K, Optional<V>> inner;

    public ConcurrentOptionalValueMap() {
        inner = new ConcurrentHashMap<>();
    }

    public ConcurrentOptionalValueMap(int initialCapacity) {
        this.inner = new ConcurrentHashMap<>(initialCapacity);
    }

    @Override
    public int size() {
        return inner.size();
    }

    @Override
    public boolean isEmpty() {
        return inner.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        if (key != null) {
            return inner.containsKey(key);
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        return inner.containsValue(wrap(value));
    }

    @Override
    public V get(Object key) {
        if (key != null) {
            return unwrap(inner.get(key));
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        if (key != null) {
            return unwrap(inner.put(key, wrap(value)));
        }
        return null;
    }

    @Override
    public V remove(Object key) {
        if (key != null) {
            return unwrap(inner.remove(key));
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        m.forEach((key, value) -> put(key, value));
    }

    @Override
    public void clear() {
        inner.clear();
    }

    @Override
    public Set<K> keySet() {
        return inner.keySet();
    }

    @Override
    public Collection<V> values() {
        return inner.values().stream().map(v -> unwrap(v)).collect(Collectors.toList());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return inner.entrySet().stream().map(e -> new Entry<K, V>() {
            @Override
            public K getKey() {
                return e.getKey();
            }

            @Override
            public V getValue() {
                return unwrap(e.getValue());
            }

            @Override
            public V setValue(V value) {
                return value;
            }
        }).collect(Collectors.toSet());
    }

    private V unwrap(Optional<V> optional) {
        return optional == null ? null : optional.orElse(null);
    }

    private Optional<V> wrap(Object value) {
        return (Optional<V>) Optional.ofNullable(value);
    }

    @Override
    public String toString() {
        return inner.toString();
    }
}
