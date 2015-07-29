package com.newrelic.agent.util;

import java.util.Arrays;

public class InsertOnlyArray<E> {
    private volatile Object[] elements;
    private int size = 0;

    public InsertOnlyArray(int capacity) {
        elements = new Object[capacity];
    }

    public E get(int index) {
        return (E) elements[index];
    }

    public int getIndex(E element) {
        Object[] arr = elements;
        for (int i = 0; i < arr.length; i++) {
            if (element.equals(arr[i])) {
                return i;
            }
        }
        return -1;
    }

    public synchronized int add(E newElement) {
        int position = size;
        if (size + 1 > elements.length) {
            grow(size + 1);
        }
        elements[position] = newElement;
        size += 1;

        return position;
    }

    private void grow(int minCapacity) {
        int oldCapacity = elements.length;

        int newCapacity = oldCapacity + (oldCapacity >> 1);
        newCapacity = Math.max(newCapacity, minCapacity);
        elements = Arrays.copyOf(elements, newCapacity);
    }
}