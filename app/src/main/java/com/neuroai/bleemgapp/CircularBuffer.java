package com.neuroai.bleemgapp;

import androidx.annotation.NonNull;

import java.util.Arrays;

public class CircularBuffer<T> {
    private final Object[] buffer;
    private final int capacity;
    private int index = 0;
    private int size = 0;

    public CircularBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new Object[capacity];
    }

    public void add(T element) {
        buffer[(index + size) % capacity] = element;
        if (size == capacity) {
            index = (index + 1) % capacity; // basically just overwrites oldest element
        } else {
            size++;
        }
    }

    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index out of bounds");
        }
        return (T) buffer[(this.index + index) % capacity];
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < size; i++) {
            sb.append(get(i)).append(i < size - 1 ? ", " : "");
        }
        sb.append("]");
        return sb.toString();
    }
}
