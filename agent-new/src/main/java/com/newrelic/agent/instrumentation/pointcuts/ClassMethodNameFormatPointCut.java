package com.newrelic.agent.instrumentation.pointcuts;

import java.util.Map;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.config.BaseConfig;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.yaml.MetricNameFormatFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;

public class ClassMethodNameFormatPointCut extends TracerFactoryPointCut {
    private final MetricNameFormatFactory metricNameFormatFactory;
    private final boolean dispatcher;
    private final boolean skipTransactionTrace;
    private final boolean ignoreTransaction;

    public ClassMethodNameFormatPointCut(MetricNameFormatFactory metricNameFormatFactory, ClassMatcher classMatcher,
                                         MethodMatcher methodMatcher, boolean dispatcher, Map configAttributes) {
        super(new PointCutConfiguration((String) null), classMatcher, methodMatcher);
        setPriority(19);
        this.metricNameFormatFactory = metricNameFormatFactory;
        this.dispatcher = dispatcher;

        BaseConfig config = new BaseConfig(configAttributes);
        this.skipTransactionTrace =
                ((Boolean) config.getProperty("skip_transaction_trace", Boolean.FALSE)).booleanValue();
        this.ignoreTransaction = ((Boolean) config.getProperty("ignore_transaction", Boolean.FALSE)).booleanValue();
    }

    public ClassMethodNameFormatPointCut(MetricNameFormatFactory pMetricNameFormatFactory, ClassMatcher pClassMatcher,
                                         MethodMatcher pMethodMatcher, boolean pDispatcher,
                                         boolean pSkipTransactionTrace, boolean pIgnoreTransaction) {
        super(new PointCutConfiguration((String) null), pClassMatcher, pMethodMatcher);
        setPriority(19);
        this.metricNameFormatFactory = pMetricNameFormatFactory;
        this.dispatcher = pDispatcher;
        this.skipTransactionTrace = pSkipTransactionTrace;
        this.ignoreTransaction = pIgnoreTransaction;
    }

    protected boolean isDispatcher() {
        return this.dispatcher;
    }

    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object object, Object[] args) {
        MetricNameFormat format = this.metricNameFormatFactory.getMetricNameFormat(sig, object, args);

        if (this.dispatcher) {
            return new OtherRootTracer(transaction, sig, object, format);
        }
        int flags = 2;
        if (!this.skipTransactionTrace) {
            flags |= 4;
        }
        return new DefaultTracer(transaction, sig, object, format, flags);
    }

    protected boolean isIgnoreTransaction() {
        return this.ignoreTransaction;
    }
}