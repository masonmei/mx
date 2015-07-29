package com.newrelic.agent.instrumentation.pointcuts;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;

@PointCut
public class MathCSConcurrentPointCut extends TracerFactoryPointCut {
    public MathCSConcurrentPointCut(ClassTransformer classTransformer) {
        super(MathCSConcurrentPointCut.class,
                     new InterfaceMatcher("edu/emory/mathcs/backport/java/util/concurrent/Callable"),
                     createExactMethodMatcher("call", new String[] {"()Ljava/lang/Object;"}));
    }

    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object callable, Object[] args) {
        return new OtherRootTracer(transaction, sig, callable,
                                          new SimpleMetricNameFormat("OtherTransaction/Job/emoryConcurrentCallable"));
    }

    protected boolean isDispatcher() {
        return true;
    }
}