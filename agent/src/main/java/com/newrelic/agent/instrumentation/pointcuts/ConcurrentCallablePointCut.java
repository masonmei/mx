package com.newrelic.agent.instrumentation.pointcuts;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OrClassMatcher;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;

@PointCut
public class ConcurrentCallablePointCut extends TracerFactoryPointCut {
    public ConcurrentCallablePointCut(ClassTransformer classTransformer) {
        super(new PointCutConfiguration(ConcurrentCallablePointCut.class.getName(), null, false),
                     new OrClassMatcher(new ClassMatcher[] {new ExactClassMatcher
                                                                    ("java/util/concurrent/Executors$RunnableAdapter"),
                                                                   new InterfaceMatcher
                                                                           ("java/util/concurrent/Callable")}),
                     createExactMethodMatcher("call", new String[] {"()Ljava/lang/Object;"}));
    }

    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object callable, Object[] args) {
        return new OtherRootTracer(transaction, sig, callable,
                                          new ClassMethodMetricNameFormat(sig, callable, "OtherTransaction/Job"));
    }

    protected boolean isDispatcher() {
        return true;
    }
}