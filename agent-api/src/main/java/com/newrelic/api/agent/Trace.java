package com.newrelic.api.agent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If you annotate a method with the Trace annotation it will be automatically timed by the New Relic agent with the
 * following measurements:
 * 
 * <ul>
 * <li>call count</li>
 * <li>calls per minute</li>
 * <li>total call time</li>
 * <li>standard deviation call time</li>
 * <li>min call time</li>
 * <li>min call time</li>
 * </ul>
 * 
 * A metric representing these measurements will be reporting inside the call scope of the current transaction (e.g.
 * "/servlets/myservlet") so that New Relic can "break out" the response time of a given transaction by specific called
 * methods. An rollup summary metric (all invocations of the method for every transaction) will also be reported.
 * 
 * Be mindful when using this attribute. When placed on relatively heavyweight operations such as database access or
 * webservice invocation, its overhead will be negligible. On the other hand, if placed on a tight, frequently called
 * method (e.g. an accessor that is called thousands of times per second), then the tracer will introduce higher
 * overhead to your application.
 * 
 * @author cirne
 * @see TracedMethod
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Trace {
    public static final String NULL = "";

    /**
     * Sets the metric name for this tracer. If unspecified, the class / method name will be used.
     * 
     * @return The metric name for this tracer.
     * @since 1.3.0
     */
    String metricName() default NULL;

    /**
     * Specifies one or more rollup metrics names. When the tracer finishes, an unscoped metric (a metric which is not
     * scoped to a specific transaction) will be recorded with the given metric name. This is useful when you want to
     * record a summary value across multiple methods or transaction names.
     * 
     * @return One or more rollup metric names.
     * @since 3.5.0
     */
    String[] rollupMetricName() default NULL;

    /**
     * If true, this method will be considered the start of a transaction. When this method is invoked within the
     * context of an existing transaction this has no effect. If false and this method is invoked outside a transaction,
     * then this tracer will not be reported.
     * 
     * @return True if this method should start a transaction if one has not already been started.
     * @since 1.3.0
     */
    boolean dispatcher() default false;

    /**
     * Names the current transaction using this tracer's metric name.
     * 
     * @return True if this traced method should be used to name the transaction, else false.
     * @since 3.1.0
     */
    boolean nameTransaction() default false;

    /**
     * @deprecated Do not use.
     * @return
     * @since 1.3.0
     */
    String tracerFactoryName() default NULL;

    /**
     * Ignores the entire current transaction.
     * 
     * @return True if this transaction should be ignored, else false.
     * @since 2.21.0
     */
    boolean skipTransactionTrace() default false;

    /**
     * Excludes this traced method from transaction traces. Metric data is still generated.
     * 
     * @return True if this traced method should be excluded from transaction traces, else false.
     * @since 3.1.0
     */
    boolean excludeFromTransactionTrace() default false;

    /**
     * A leaf tracer will not have any child tracers. This is useful when all time should be attributed to the tracer
     * even if other trace points are encountered during its execution. For example, database tracers often act as leaf
     * so that all time is attributed to database activity even if instrumented external calls are made.
     * 
     * If a leaf tracer does not participate in transaction traces ({@link #excludeFromTransactionTrace()}) the agent
     * can create a tracer with lower overhead.
     * 
     * @return True if this traced method should be a leaf, else false.
     * @since 3.1.0
     */
    boolean leaf() default false;
}
