package com.newrelic.api.agent;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Provides NoOps for API objects to avoid returning <code>null</code>. Do not call these objects directly.
 */
class NoOpAgent implements Agent {
    static final Agent INSTANCE = new NoOpAgent();
    private static final TracedMethod TRACED_METHOD = new TracedMethod() {

        @Override
        public void setMetricName(String... metricNameParts) {
        }

        @Override
        public String getMetricName() {
            return "NoAgent";
        }

        @Override
        public void addRollupMetricName(String... metricNameParts) {
        }
    };

    private static final Transaction TRANSACTION = new Transaction() {

        @Override
        public boolean setTransactionName(TransactionNamePriority namePriority, boolean override, String category,
                                          String... parts) {
            return false;
        }

        @Override
        public boolean isTransactionNameSet() {
            return false;
        }

        @Override
        public void ignore() {
        }

        @Override
        public TracedMethod getLastTracer() {
            return getTracedMethod();
        }

        @Override
        public TracedMethod getTracedMethod() {
            return TRACED_METHOD;
        }

        @Override
        public void ignoreApdex() {
        }

        @Override
        public String getRequestMetadata() {
            return null;
        }

        @Override
        public void processRequestMetadata(String metadata) {
        }

        @Override
        public String getResponseMetadata() {
            return null;
        }

        @Override
        public void processResponseMetadata(String metadata) {
        }
    };
    private static final Logger LOGGER = new Logger() {

        @Override
        public void logToChild(String childName, Level level, String pattern, Object... parts) {
        }

        @Override
        public void log(Level level, Throwable t, String pattern, Object... msg) {
        }

        @Override
        public void log(Level level, String pattern, Object... parts) {
        }

        @Override
        public boolean isLoggable(Level level) {
            return false;
        }
    };
    private static final Config CONFIG = new Config() {

        @Override
        public <T> T getValue(String key, T defaultVal) {
            return defaultVal;
        }

        @Override
        public <T> T getValue(String key) {
            return null;
        }
    };
    private static final MetricAggregator METRIC_AGGREGATOR = new MetricAggregator() {

        @Override
        public void recordResponseTimeMetric(String name, long millis) {
        }

        @Override
        public void recordResponseTimeMetric(String name, long totalTime, long exclusiveTime, TimeUnit timeUnit) {
        }

        @Override
        public void recordMetric(String name, float value) {
        }

        @Override
        public void incrementCounter(String name, int count) {
        }

        @Override
        public void incrementCounter(String name) {
        }
    };
    private static final Insights INSIGHTS = new Insights() {

        @Override
        public void recordCustomEvent(String eventType, Map<String, Object> attributes) {
        }
    };

    @Override
    public TracedMethod getTracedMethod() {
        return TRACED_METHOD;
    }

    @Override
    public Transaction getTransaction() {
        return TRANSACTION;
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Config getConfig() {
        return CONFIG;
    }

    @Override
    public MetricAggregator getMetricAggregator() {
        return METRIC_AGGREGATOR;
    }

    @Override
    public Insights getInsights() {
        return INSIGHTS;
    }
}
