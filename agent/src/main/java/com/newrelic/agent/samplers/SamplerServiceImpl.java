package com.newrelic.agent.samplers;

import java.io.Closeable;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.deps.com.google.common.collect.Sets;
import com.newrelic.agent.IAgent;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.MergeStatsEngine;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsEngineImpl;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsWork;
import com.newrelic.agent.util.DefaultThreadFactory;
import com.newrelic.agent.util.SafeWrappers;

public class SamplerServiceImpl extends AbstractService implements SamplerService {
    private static final String SAMPLER_THREAD_NAME = "New Relic Sampler Service";
    private static final long INITIAL_DELAY_IN_MILLISECONDS = 1000L;
    private static final long DELAY_IN_MILLISECONDS = 5000L;
    private final ScheduledExecutorService scheduledExecutor;
    private final Set<ScheduledFuture<?>> tasks =
            Sets.newSetFromMap(Maps.<ScheduledFuture<?>, Boolean>newConcurrentMap());
    private final StatsEngine statsEngine = new StatsEngineImpl();
    private final IAgent agent;
    private final String defaultAppName;
    private final boolean isAutoAppNamingEnabled;

    public SamplerServiceImpl() {
        super(SamplerService.class.getSimpleName());
        ThreadFactory threadFactory = new DefaultThreadFactory("New Relic Sampler Service", true);
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        agent = ServiceFactory.getAgent();
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        isAutoAppNamingEnabled = config.isAutoAppNamingEnabled();
        defaultAppName = config.getApplicationName();
    }

    protected void doStart() {
        MemorySampler memorySampler = new MemorySampler();
        memorySampler.start();
        addMetricSampler(memorySampler, 1000L, 5000L, TimeUnit.MILLISECONDS);

        ThreadSampler threadSampler = new ThreadSampler();
        addMetricSampler(threadSampler, 1000L, 5000L, TimeUnit.MILLISECONDS);
    }

    protected void doStop() {
        synchronized(tasks) {
            for (ScheduledFuture task : tasks) {
                task.cancel(false);
            }
            tasks.clear();
        }
        scheduledExecutor.shutdown();
    }

    public boolean isEnabled() {
        return true;
    }

    private void addMetricSampler(final MetricSampler sampler, long initialDelay, long delay, TimeUnit unit) {
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    SamplerServiceImpl.this.runSampler(sampler);
                } catch (Throwable t) {
                    String msg =
                            MessageFormat.format("Unable to sample {0}: {1}", new Object[] {getClass().getName(), t});
                    if (getLogger().isLoggable(Level.FINER)) {
                        getLogger().log(Level.WARNING, msg, t);
                    } else {
                        getLogger().warning(msg);
                    }
                } finally {
                    statsEngine.clear();
                }
            }
        };
        addSampler(runnable, delay, unit);
    }

    private void runSampler(MetricSampler sampler) {
        if (!agent.isEnabled()) {
            return;
        }
        sampler.sample(statsEngine);
        if (!isAutoAppNamingEnabled) {
            mergeStatsEngine(defaultAppName);
            return;
        }
        List<IRPMService> rpmServices = ServiceFactory.getRPMServiceManager().getRPMServices();
        for (IRPMService rpmService : rpmServices) {
            String appName = rpmService.getApplicationName();
            mergeStatsEngine(appName);
        }
    }

    private void mergeStatsEngine(String appName) {
        StatsService statsService = ServiceFactory.getStatsService();
        StatsWork work = new MergeStatsEngine(appName, statsEngine);
        statsService.doStatsWork(work);
    }

    public Closeable addSampler(Runnable sampler, long period, TimeUnit timeUnit) {
        if (scheduledExecutor.isShutdown()) {
            return null;
        }
        final ScheduledFuture task =
                scheduledExecutor.scheduleWithFixedDelay(SafeWrappers.safeRunnable(sampler), period, period, timeUnit);

        tasks.add(task);
        return new Closeable() {
            public void close() throws IOException {
                tasks.remove(task);
                task.cancel(false);
            }
        };
    }
}