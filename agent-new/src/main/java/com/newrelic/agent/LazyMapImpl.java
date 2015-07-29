//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.newrelic.deps.com.google.common.collect.MapMaker;

public class LazyMapImpl<K, V> implements Map<K, V> {
  private final AtomicReference<Map<K, V>> parameters;
  private final MapMaker factory;

  public LazyMapImpl() {
    this(5);
  }

  public LazyMapImpl(int initialSize) {
    this((new MapMaker()).initialCapacity(initialSize).concurrencyLevel(1));
  }

  public LazyMapImpl(MapMaker factory) {
    this.parameters = new AtomicReference();
    this.factory = factory;
  }

  private Map<K, V> getParameters() {
    if (this.parameters.get() == null) {
      this.parameters.compareAndSet(null, this.factory.<K, V>makeMap());
    }

    return this.parameters.get();
  }

  public V put(K key, V value) {
    return this.getParameters().put(key, value);
  }

  public void putAll(Map<? extends K, ? extends V> params) {
    if (params != null && !params.isEmpty()) {
      this.getParameters().putAll(params);
    }

  }

  public V remove(Object key) {
    return this.parameters.get() == null ? null : this.getParameters().remove(key);
  }

  public V get(Object key) {
    return this.parameters.get() == null ? null : this.getParameters().get(key);
  }

  public void clear() {
    if (this.parameters.get() != null) {
      ((Map) this.parameters.get()).clear();
    }

  }

  public int size() {
    return this.parameters.get() == null ? 0 : this.getParameters().size();
  }

  public boolean isEmpty() {
    return this.parameters.get() == null ? true : this.getParameters().isEmpty();
  }

  public boolean containsKey(Object key) {
    return this.parameters.get() == null ? false : this.getParameters().containsKey(key);
  }

  public boolean containsValue(Object value) {
    return this.parameters.get() == null ? false : this.getParameters().containsValue(value);
  }

  public Set<K> keySet() {
    return this.getParameters().keySet();
  }

  public Collection<V> values() {
    return this.getParameters().values();
  }

  public Set<Entry<K, V>> entrySet() {
    return this.getParameters().entrySet();
  }
}
