package com.newrelic.agent.samplers;

import com.newrelic.agent.service.Service;
import java.io.Closeable;
import java.util.concurrent.TimeUnit;

public abstract interface SamplerService extends Service
{
  public abstract Closeable addSampler(Runnable paramRunnable, long paramLong, TimeUnit paramTimeUnit);
}