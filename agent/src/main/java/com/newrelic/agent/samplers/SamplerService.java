package com.newrelic.agent.samplers;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

import com.newrelic.agent.service.Service;

public abstract interface SamplerService extends Service {
    public abstract Closeable addSampler(Runnable paramRunnable, long paramLong, TimeUnit paramTimeUnit);
}