package com.newrelic.agent.instrumentation.pointcuts.frameworks.faces;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;

public class PhasePointCut extends TracerFactoryPointCut {
    public PhasePointCut() {
        super(new PointCutConfiguration(PhasePointCut.class), new ExactClassMatcher("com/sun/faces/lifecycle/Phase"),
                     createExactMethodMatcher("doPhase", new String[] {"(Ljavax/faces/context/FacesContext;"
                                                                               + "Ljavax/faces/lifecycle/Lifecycle;"
                                                                               + "Ljava/util/ListIterator;)V"}));
    }

    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object phase, Object[] args) {
        return new DefaultTracer(transaction, sig, phase, new ClassMethodMetricNameFormat(sig, phase));
    }
}