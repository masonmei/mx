package com.newrelic.agent.util.collect;

import java.util.Collection;
import java.util.Set;

import com.newrelic.deps.com.google.common.collect.ForwardingMultimap;
import com.newrelic.deps.com.google.common.collect.ForwardingSetMultimap;
import com.newrelic.deps.com.google.common.collect.ImmutableList;
import com.newrelic.deps.com.google.common.collect.ImmutableSet;
import com.newrelic.deps.com.google.common.collect.Multimap;
import com.newrelic.deps.com.google.common.collect.SetMultimap;

public class NRMultimaps {
  public static final <K, V> SetMultimap<K, V> performantSetMultimapFrom(final SetMultimap<K, V> multimap) {
    return new ForwardingSetMultimap<K, V>() {
      public Set<V> get(K key) {
        return delegate().containsKey(key) ? delegate().get(key) : ImmutableSet.<V>of();
      }

      protected SetMultimap<K, V> delegate() {
        return multimap;
      }
    };
  }

  public static final <K, V> Multimap<K, V> performantMultimapFrom(final Multimap<K, V> multimap) {
    return new ForwardingMultimap<K, V>() {
      public Collection<V> get(K key) {
        return delegate().containsKey(key) ? delegate().get(key) : ImmutableList.<V>of();
      }

      protected Multimap<K, V> delegate() {
        return multimap;
      }
    };
  }
}