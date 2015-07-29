package com.newrelic.agent.stats;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;

public abstract class AbstractMetricAggregator implements MetricAggregator {
    private final Logger logger;

    protected AbstractMetricAggregator() {
        this(Agent.LOG);
    }

    protected AbstractMetricAggregator(Logger logger) {
        this.logger = logger;
    }

    private static void logException(Logger logger, Throwable t, String pattern, Object[] parts) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, t, pattern, parts);
        } else if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, pattern, parts);
        }
    }

    public final void recordResponseTimeMetric(String name, long totalTime, long exclusiveTime, TimeUnit timeUnit) {
        if ((name == null) || (name.length() == 0)) {
            this.logger
                    .log(Level.FINER, "recordResponseTimeMetric was invoked with a null or empty name", new Object[0]);
            return;
        }
        try {
            doRecordResponseTimeMetric(name, totalTime, exclusiveTime, timeUnit);
            this.logger.log(Level.FINER, "Recorded response time metric \"{0}\": {1}",
                                   new Object[] {name, Long.valueOf(totalTime)});
        } catch (Throwable t) {
            logException(this.logger, t, "Exception recording response time metric \"{0}\": {1}",
                                new Object[] {name, t});
        }
    }

    protected abstract void doRecordResponseTimeMetric(String paramString, long paramLong1, long paramLong2,
                                                       TimeUnit paramTimeUnit);

    public final void recordMetric(String name, float value) {
        if ((name == null) || (name.length() == 0)) {
            this.logger.log(Level.FINER, "recordMetric was invoked with a null or empty name", new Object[0]);
            return;
        }
        try {
            doRecordMetric(name, value);
            this.logger.log(Level.FINER, "Recorded metric \"{0}\": {1}", new Object[] {name, Float.valueOf(value)});
        } catch (Throwable t) {
            logException(this.logger, t, "Exception recording metric \"{0}\": {1}", new Object[] {name, t});
        }
    }

    protected abstract void doRecordMetric(String paramString, float paramFloat);

    public final void recordResponseTimeMetric(String name, long millis) {
        recordResponseTimeMetric(name, millis, millis, TimeUnit.MILLISECONDS);
    }

    public final void incrementCounter(String name) {
        incrementCounter(name, 1);
    }

    public final void incrementCounter(String name, int count) {
        if ((name == null) || (name.length() == 0)) {
            this.logger.log(Level.FINER, "incrementCounter was invoked with a null metric name", new Object[0]);
            return;
        }
        try {
            doIncrementCounter(name, count);
            this.logger
                    .log(Level.FINER, "incremented counter \"{0}\": {1}", new Object[] {name, Integer.valueOf(count)});
        } catch (Throwable t) {
            logException(this.logger, t, "Exception incrementing counter \"{0}\",{1} : {2}",
                                new Object[] {name, Integer.valueOf(count), t});
        }
    }

    protected abstract void doIncrementCounter(String paramString, int paramInt);
}