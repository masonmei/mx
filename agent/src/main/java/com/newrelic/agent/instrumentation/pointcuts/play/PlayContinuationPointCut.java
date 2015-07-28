package com.newrelic.agent.instrumentation.pointcuts.play;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.Tracer;

@PointCut
public class PlayContinuationPointCut extends TracerFactoryPointCut {
    private static final String POINT_CUT_NAME = PlayContinuationPointCut.class.getName();
    private static final String CONTINUATION_CLASS = "org/apache/commons/javaflow/Continuation";
    private static final String SUSPEND_METHOD_NAME = "suspend";
    private static final String SUSPEND_METHOD_DESC = "(Ljava/lang/Object;)Ljava/lang/Object;";

    public PlayContinuationPointCut(ClassTransformer classTransformer) {
        super(createPointCutConfig(), createClassMatcher(), createMethodMatcher());
    }

    private static PointCutConfiguration createPointCutConfig() {
        return new PointCutConfiguration(POINT_CUT_NAME, "play_instrumentation", true);
    }

    private static ClassMatcher createClassMatcher() {
        return new ExactClassMatcher(CONTINUATION_CLASS);
    }

    private static MethodMatcher createMethodMatcher() {
        return new ExactMethodMatcher(SUSPEND_METHOD_NAME, SUSPEND_METHOD_DESC);
    }

    public Tracer doGetTracer(Transaction tx, ClassMethodSignature sig, Object object, Object[] args) {
        tx.getTransactionState().suspend();
        return null;
    }
}