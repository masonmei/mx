package com.newrelic.agent.util;

import java.util.TreeSet;

public class FixedSizeSet<E> extends TreeSet<E> {
  private static final long serialVersionUID = 1474558437021809591L;
  private int size;

  public FixedSizeSet(int size) {
    this.size = size;
  }

  public int getFixedSize() {
    return size;
  }

  public boolean add(E o) {
    if (size() >= size) {
      E first = first();
      int comparison =
              (o instanceof Comparable) ? ((Comparable) o).compareTo(first) : comparator().compare(o, first);

      if (comparison < 0) {
        return false;
      }
      super.remove(first);
    }

    return super.add(o);
  }
}