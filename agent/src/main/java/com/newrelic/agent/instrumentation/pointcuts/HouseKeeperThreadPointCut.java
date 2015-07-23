package com.newrelic.agent.instrumentation.pointcuts;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;

@PointCut
public class HouseKeeperThreadPointCut extends TracerFactoryPointCut {
    public HouseKeeperThreadPointCut(ClassTransformer classTransformer) {
        super(HouseKeeperThreadPointCut.class, new ExactClassMatcher("org/logicalcobwebs/proxool/HouseKeeperThread"),
                     createExactMethodMatcher("run", new String[] {"()V"}));
    }

    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object thread, Object[] args) {
        return new OtherRootTracer(transaction, sig, thread,
                                          new ClassMethodMetricNameFormat(sig, thread, "OtherTransaction/Job"));
    }

    protected boolean isDispatcher() {
        return true;
    }
}