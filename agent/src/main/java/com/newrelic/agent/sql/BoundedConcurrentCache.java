package com.newrelic.agent.sql;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class BoundedConcurrentCache<K, V extends Comparable<V>> {
    private final int maxCapacity;
    private final PriorityQueue<V> priorityQueue;
    private final Map<K, V> cache;
    private final Map<V, K> inverseCache;

    public BoundedConcurrentCache(int size) {
        maxCapacity = size;
        priorityQueue = new PriorityQueue(size);
        cache = new HashMap();
        inverseCache = new HashMap();
    }

    public BoundedConcurrentCache(int size, Comparator<V> comparator) {
        maxCapacity = size;
        priorityQueue = new PriorityQueue(size, comparator);
        cache = new HashMap();
        inverseCache = new HashMap();
    }

    public synchronized V get(K sql) {
        return cache.get(sql);
    }

    public synchronized V putIfAbsent(K key, V value) {
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        if (priorityQueue.size() == maxCapacity) {
            Comparable val = (Comparable) priorityQueue.poll();
            Object sqlToRemove = inverseCache.get(val);
            cache.remove(sqlToRemove);
            inverseCache.remove(val);
        }
        priorityQueue.add(value);
        cache.put(key, value);
        inverseCache.put(value, key);
        return null;
    }

    public synchronized void putReplace(K key, V value) {
        if (cache.containsKey(key)) {
            Comparable valueToUpdate = (Comparable) cache.remove(key);
            inverseCache.remove(valueToUpdate);
            priorityQueue.remove(valueToUpdate);
        }
        putIfAbsent(key, value);
    }

    public synchronized int size() {
        return priorityQueue.size();
    }

    public synchronized void clear() {
        cache.clear();
        inverseCache.clear();
        priorityQueue.clear();
    }

    public synchronized List<V> asList() {
        ArrayList list = new ArrayList();
        Iterator iter = priorityQueue.iterator();
        while (iter.hasNext()) {
            list.add(iter.next());
        }
        return list;
    }
}