package com.newrelic.agent.circuitbreaker;

public class SamplingCounter
{
  private final long samplingRate;
  private long count;

  public SamplingCounter(long samplingRate)
  {
    this.count = 0L;
    this.samplingRate = samplingRate;
  }

  public boolean shouldSample()
  {
    if (++this.count > this.samplingRate) {
      this.count = 0L;
      return true;
    }
    return false;
  }
}