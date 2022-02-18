package com.dci.intellij.dbn.common.dispose;

import com.dci.intellij.dbn.common.util.Unsafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public final class Disposed {
    private static final DisposedList LIST = new DisposedList();
    private static final DisposedMap MAP = new DisposedMap<>();

    private Disposed() { }

    public static <T> List<T> list() {
        return Unsafe.cast(LIST);
    }

    public static <K, V> Map<K, V> map() {
        return Unsafe.cast(MAP);
    }

    private static class DisposedList<T> implements List<T> {

        DisposedList() {}

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @NotNull
        @Override
        public Iterator<T> iterator() {
            return Collections.emptyIterator();
        }

        @NotNull
        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        @NotNull
        @Override
        public <T1> T1[] toArray(@NotNull T1[] a) {
            return null;
        }

        @Override
        public boolean add(T t) {
            return false;
        }

        @Override
        public boolean remove(Object o) {
            return false;
        }

        @Override
        public boolean containsAll(@NotNull Collection<?> c) {
            return false;
        }

        @Override
        public boolean addAll(@NotNull Collection<? extends T> c) {
            return false;
        }

        @Override
        public boolean addAll(int index, @NotNull Collection<? extends T> c) {
            return false;
        }

        @Override
        public boolean removeAll(@NotNull Collection<?> c) {
            return false;
        }

        @Override
        public boolean retainAll(@NotNull Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {

        }

        @Override
        public T get(int index) {
            return null;
        }

        @Override
        public T set(int index, T element) {
            return null;
        }

        @Override
        public void add(int index, T element) {

        }

        @Override
        public T remove(int index) {
            return null;
        }

        @Override
        public int indexOf(Object o) {
            return 0;
        }

        @Override
        public int lastIndexOf(Object o) {
            return 0;
        }

        @NotNull
        @Override
        public ListIterator<T> listIterator() {
            return Collections.emptyListIterator();
        }

        @NotNull
        @Override
        public ListIterator<T> listIterator(int index) {
            return Collections.emptyListIterator();
        }

        @NotNull
        @Override
        public List<T> subList(int fromIndex, int toIndex) {
            return this;
        }
    }

    private static final class DisposedMap<K, V> implements Map<K, V> {

        DisposedMap() {}

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public V get(Object key) {
            return null;
        }

        @Nullable
        @Override
        public V put(K key, V value) {
            return null;
        }

        @Override
        public V remove(Object key) {
            return null;
        }

        @Override
        public void putAll(@NotNull Map<? extends K, ? extends V> m) {

        }

        @Override
        public void clear() {

        }

        @NotNull
        @Override
        public Set<K> keySet() {
            return Collections.emptySet();
        }

        @NotNull
        @Override
        public Collection<V> values() {
            return Collections.emptyList();
        }

        @NotNull
        @Override
        public Set<Entry<K, V>> entrySet() {
            return Collections.emptySet();
        }
    }
}
