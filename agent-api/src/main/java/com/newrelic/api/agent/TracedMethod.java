package com.newrelic.api.agent;

/**
 * Represents a single instance of the timing mechanism associated with a method that is instrumented using the
 * {@link Trace} annotation.
 * 
 * @author sdaubin
 * @see Agent#getTracedMethod()
 * 
 */
public interface TracedMethod {

    /**
     * Returns the traced method metric name.
     * 
     * @return The metric named used for this traced method.
     * @since 3.9.0
     * 
     */
    String getMetricName();

    /**
     * Sets the traced method metric name by concatenating all given metricNameParts with a '/' separating each part.
     * 
     * @param metricNameParts The segments of the metric name. These values will be concatenated together separated by a
     *        `/` char.
     * @since 3.9.0
     */
    void setMetricName(String... metricNameParts);

    /**
     * Metric names added here will be reported as roll-up metrics. A roll-up metric is an extra unscoped metric (a
     * metric which is not scoped to a specific transaction) that is reported in addition to the normal metric recorded
     * for a traced method. An example of how the agent uses a roll-up metric is the OtherTransaction/all metric. Each
     * background transaction records data to its transaction specific metric and to the OtherTransaction/all roll-up
     * metric which represents all background transactions.
     * 
     * @param metricNameParts The segments of the rollup metric name. These values will be concatenated together
     *        separated by a `/` char.
     * @since 3.9.0
     */
    void addRollupMetricName(String... metricNameParts);

}
