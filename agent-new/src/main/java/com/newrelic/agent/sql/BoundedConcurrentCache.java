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
        this.maxCapacity = size;
        this.priorityQueue = new PriorityQueue(size);
        this.cache = new HashMap();
        this.inverseCache = new HashMap();
    }

    public BoundedConcurrentCache(int size, Comparator<V> comparator) {
        this.maxCapacity = size;
        this.priorityQueue = new PriorityQueue(size, comparator);
        this.cache = new HashMap();
        this.inverseCache = new HashMap();
    }

    public synchronized V get(K sql) {
        return this.cache.get(sql);
    }

    public synchronized V putIfAbsent(K key, V value) {
        if (this.cache.containsKey(key)) {
            return this.cache.get(key);
        }

        if (this.priorityQueue.size() == this.maxCapacity) {
            Comparable val = (Comparable) this.priorityQueue.poll();
            Object sqlToRemove = this.inverseCache.get(val);
            this.cache.remove(sqlToRemove);
            this.inverseCache.remove(val);
        }
        this.priorityQueue.add(value);
        this.cache.put(key, value);
        this.inverseCache.put(value, key);
        return null;
    }

    public synchronized void putReplace(K key, V value) {
        if (this.cache.containsKey(key)) {
            Comparable valueToUpdate = (Comparable) this.cache.remove(key);
            this.inverseCache.remove(valueToUpdate);
            this.priorityQueue.remove(valueToUpdate);
        }
        putIfAbsent(key, value);
    }

    public synchronized int size() {
        return this.priorityQueue.size();
    }

    public synchronized void clear() {
        this.cache.clear();
        this.inverseCache.clear();
        this.priorityQueue.clear();
    }

    public synchronized List<V> asList() {
        ArrayList list = new ArrayList();
        Iterator iter = this.priorityQueue.iterator();
        while (iter.hasNext()) {
            list.add(iter.next());
        }
        return list;
    }
}