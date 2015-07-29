package com.newrelic.agent.service.analytics;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.service.ServiceUtils;

public class FixedSizeArrayList<E> implements List<E> {
  protected final int size;
  protected final AtomicInteger numberOfTries = new AtomicInteger();
  private final Object[] data;
  private final AtomicInteger volatileMemoryBarrier = new AtomicInteger(0);

  public FixedSizeArrayList(int size) {
    data = new Object[size];
    this.size = size;
  }

  public E get(int index) {
    rangeCheck(index);
    ServiceUtils.readMemoryBarrier(volatileMemoryBarrier);
    return (E) data[index];
  }

  public boolean add(E t) {
    Integer slot = getSlot();
    if (slot == null) {
      return false;
    }
    set(slot.intValue(), t);
    return true;
  }

  public boolean addAll(Collection<? extends E> c) {
    boolean modified = false;
    for (Iterator i$ = c.iterator(); i$.hasNext(); ) {
      Object e = i$.next();
      modified |= add((E) e);
    }
    return modified;
  }

  public E set(int slot, E element) {
    rangeCheck(slot);
    ServiceUtils.readMemoryBarrier(volatileMemoryBarrier);
    Object oldValue = data[slot];
    data[slot] = element;
    ServiceUtils.writeMemoryBarrier(volatileMemoryBarrier);
    return (E) oldValue;
  }

  private void rangeCheck(int index) {
    if (index >= data.length) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + data.length);
    }
  }

  public Integer getSlot() {
    int insertIndex = numberOfTries.getAndIncrement();
    if (insertIndex >= data.length) {
      return null;
    }
    return Integer.valueOf(insertIndex);
  }

  int getNumberOfTries() {
    return numberOfTries.get();
  }

  public int size() {
    return Math.min(data.length, numberOfTries.get());
  }

  public boolean isEmpty() {
    return numberOfTries.get() == 0;
  }

  public Iterator<E> iterator() {
    return new Iterator() {
      int cursor;

      public boolean hasNext() {
        return cursor != size();
      }

      public E next() {
        int i = cursor;
        if (i >= size()) {
          throw new NoSuchElementException();
        }
        cursor = (i + 1);
        ServiceUtils.readMemoryBarrier(volatileMemoryBarrier);
        return (E) data[i];
      }

      public void remove() {
        Agent.LOG
                .log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.", new Object[0]);
      }
    };
  }

  public Object[] toArray() {
    return Arrays.copyOf(data, size());
  }

  public boolean contains(Object o) {
    Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.", new Object[0]);
    return false;
  }

  public <T> T[] toArray(T[] a) {
    Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.", new Object[0]);
    return null;
  }

  public boolean remove(Object o) {
    Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.", new Object[0]);
    return false;
  }

  public boolean containsAll(Collection<?> c) {
    Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.", new Object[0]);
    return false;
  }

  public boolean addAll(int index, Collection<? extends E> c) {
    Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.", new Object[0]);
    return false;
  }

  public boolean removeAll(Collection<?> c) {
    Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.", new Object[0]);
    return false;
  }

  public boolean retainAll(Collection<?> c) {
    Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.", new Object[0]);
    return false;
  }

  public void clear() {
    Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.", new Object[0]);
  }

  public void add(int index, E element) {
    Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.", new Object[0]);
  }

  public E remove(int index) {
    Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.", new Object[0]);
    return null;
  }

  public int indexOf(Object o) {
    Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.", new Object[0]);
    return -1;
  }

  public int lastIndexOf(Object o) {
    Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.", new Object[0]);
    return -1;
  }

  public ListIterator<E> listIterator() {
    Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.", new Object[0]);
    return null;
  }

  public ListIterator<E> listIterator(int index) {
    Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.", new Object[0]);
    return null;
  }

  public List<E> subList(int fromIndex, int toIndex) {
    Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.", new Object[0]);
    return null;
  }
}