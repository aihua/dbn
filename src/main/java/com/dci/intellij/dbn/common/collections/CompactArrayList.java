package com.dci.intellij.dbn.common.collections;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;

import static com.dci.intellij.dbn.common.exception.Exceptions.unsupported;
import static com.dci.intellij.dbn.common.util.Unsafe.cast;

public final class CompactArrayList<T> implements List<T>, RandomAccess, Serializable {
    private Object[] elements;

    public CompactArrayList(List<T> elements) {
        this.elements = elements.toArray();
    }

    public CompactArrayList(Object[] elements) {
        this.elements = elements;
    }

    public CompactArrayList(int size) {
        this.elements = new Object[size];
    }

    public static <T> List<T> from(List<T> list) {
        return new CompactArrayList<T>(list);
    }

    @Override
    public int size() {
        return elements.length;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return cast(Arrays.stream(elements).iterator());
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return elements;
    }

    @NotNull
    @Override
    public <E> E[] toArray(@NotNull E[] s) {
        return cast(Arrays.copyOf(this.elements, elements.length, s.getClass()));
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> collection) {
        return false;
    }


    @Override
    public T get(int i) {
        return cast(elements[i]);
    }

    @Override
    public int indexOf(Object o) {
        if (o == null) {
            for(int i = 0; i < elements.length; i++) {
                if (elements[i] == null) {
                    return i;
                }
            }
        } else {
            for(int i = 0; i < elements.length; i++) {
                if (o.equals(elements[i])) {
                    return i;
                }
            }
        }

        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        int start = 0;
        int i;
        if (o == null) {
            for (i = size() - 1; i >= 0; i--) {
                if (elements[i] == null) {
                    return i;
                }
            }
        } else {
            for(i = size() - 1; i >= 0; i--) {
                if (o.equals(elements[i])) {
                    return i;
                }
            }
        }

        return -1;
    }

    @Override
    public T set(int i, T t) {
        elements[i] = t;
        return t;
    }

    @Override
    public void clear() {
        elements = new Object[0];
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator() {
        // TODO support
        return unsupported();
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator(int i) {
        // TODO support
        return unsupported();
    }

    @NotNull
    @Override
    public List<T> subList(int from, int to) {
        return cast(Arrays.asList(Arrays.copyOfRange(elements, from, to)));
    }

    @Override
    public void sort(Comparator c) {
        Arrays.sort(elements, c);
    }

    /*************************************************************
     *           unsupported update operations                   *
     *************************************************************/

    @Override
    public boolean add(T t) {
        return unsupported();
    }

    @Override
    public boolean remove(Object o) {
        return unsupported();
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> collection) {
        return unsupported();
    }

    @Override
    public boolean addAll(int i, @NotNull Collection<? extends T> collection) {
        return unsupported();
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> collection) {
        return unsupported();
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> collection) {
        return unsupported();
    }

    @Override
    public void add(int i, T t) {
        unsupported();
    }

    @Override
    public T remove(int i) {
        return unsupported();
    }
}
