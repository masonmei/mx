package com.newrelic.agent.samplers;

import com.newrelic.agent.service.NoopService;
import java.io.Closeable;
import java.util.concurrent.TimeUnit;

public class NoopSamplerService extends NoopService
  implements SamplerService
{
  public NoopSamplerService()
  {
    super("SamplerService");
  }

  public Closeable addSampler(Runnable sampler, long period, TimeUnit timeUnit)
  {
    return null;
  }
}