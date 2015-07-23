package com.newrelic.agent.instrumentation.pointcuts.scala;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.NameMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.MethodExitTracerNoSkip;
import com.newrelic.agent.tracers.Tracer;

@PointCut
public class ScalaFuturePointCut extends TracerFactoryPointCut {
    public static final boolean DEFAULT_ENABLED = true;
    private static final String CLASS_NAME = "scala/concurrent/Future$";
    private static final String METHOD_NAME = "firstCompletedOf";
    private static final String POINT_CUT_NAME = ScalaFuturePointCut.class.getName();

    public ScalaFuturePointCut(ClassTransformer classTransformer) {
        super(createPointCutConfig(), createClassMatcher(), createMethodMatcher());
    }

    private static PointCutConfiguration createPointCutConfig() {
        return new PointCutConfiguration(POINT_CUT_NAME, "scala_instrumentation", true);
    }

    private static ClassMatcher createClassMatcher() {
        return new ExactClassMatcher("scala/concurrent/Future$");
    }

    private static MethodMatcher createMethodMatcher() {
        return new NameMethodMatcher("firstCompletedOf");
    }

    public Tracer doGetTracer(final Transaction tx, ClassMethodSignature sig, Object object, Object[] args) {
        if (!tx.isStarted()) {
            return null;
        }
        tx.getTransactionState().setInvalidateAsyncJobs(true);
        return new MethodExitTracerNoSkip(sig, tx) {
            protected void doFinish(int opcode, Object returnValue) {
                tx.getTransactionState().setInvalidateAsyncJobs(false);
            }

            public void finish(Throwable throwable) {
                tx.getTransactionState().setInvalidateAsyncJobs(false);
            }
        };
    }
}