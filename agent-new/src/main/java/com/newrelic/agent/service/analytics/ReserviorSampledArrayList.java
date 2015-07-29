package com.newrelic.agent.service.analytics;

import java.util.Random;

public class ReserviorSampledArrayList<E> extends FixedSizeArrayList<E> {
  private final ThreadLocal<Random> random;

  public ReserviorSampledArrayList(int reservoirSize) {
    super(reservoirSize);
    random = new ThreadLocal() {
      protected Random initialValue() {
        return new Random();
      }
    };
  }

  public Integer getSlot() {
    int currentCount = numberOfTries.incrementAndGet() - 1;
    int insertIndex;
    if (currentCount < size) {
      insertIndex = currentCount;
    } else {
      insertIndex = ((Random) random.get()).nextInt(currentCount);
    }
    if (insertIndex >= size) {
      return null;
    }
    return Integer.valueOf(insertIndex);
  }

  void setRandomFixedSeed(long seed) {
    random.set(new Random(seed));
  }
}