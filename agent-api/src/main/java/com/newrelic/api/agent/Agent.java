package com.newrelic.api.agent;

/**
 * The New Relic Java Agent's API.
 * 
 * @author sdaubin
 * 
 */
public interface Agent {

    /**
     * Returns the current traced method. This can only be invoked within methods that are traced.
     * 
     * @return The current method being traced.
     * @see Trace
     * @since 3.9.0
     */
    TracedMethod getTracedMethod();

    /**
     * Returns the current transaction, creating one if it doesn't currently exist.
     * 
     * @return The current transaction.
     * @since 3.9.0
     */
    Transaction getTransaction();

    /**
     * Returns a logger that logs to the New Relic Java agent log file.
     * @since 3.9.0
     * @return A log where messages can be written to the New Relic Java agent log file.
     */
    Logger getLogger();

    /**
     * Returns the agent's configuration.
     * @since 3.9.0
     * @return The configuration of this Java agent.
     */
    Config getConfig();

    /**
     * Returns a metric aggregator that can be used to record metrics that can be viewed through custom dashboards.
     * 
     * @return Aggregator used to record metrics for custom dashboards.
     * @since 3.9.0
     */
    MetricAggregator getMetricAggregator();

    /**
     * Provides access to the Insights custom events API.
     * 
     * @return Object used to add custom events.
     * @since 3.13.0
     */
    Insights getInsights();
}
