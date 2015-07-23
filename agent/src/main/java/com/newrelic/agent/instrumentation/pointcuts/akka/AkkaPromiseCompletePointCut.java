package com.newrelic.agent.instrumentation.pointcuts.akka;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.NameMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.TransactionHolder;
import com.newrelic.agent.instrumentation.pointcuts.scala.Either;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.PointCutInvocationHandler;
import com.newrelic.agent.tracers.Tracer;

@com.newrelic.agent.instrumentation.pointcuts.PointCut
public class AkkaPromiseCompletePointCut extends com.newrelic.agent.instrumentation.PointCut implements
        EntryInvocationHandler {
    public static final boolean DEFAULT_ENABLED = true;
    private static final String POINT_CUT_NAME = AkkaPromiseCompletePointCut.class.getName();

    public AkkaPromiseCompletePointCut(ClassTransformer classTransformer) {
        super(createPointCutConfig(), createClassMatcher(), createMethodMatcher());
    }

    private static PointCutConfiguration createPointCutConfig() {
        return new PointCutConfiguration(POINT_CUT_NAME, "akka_instrumentation", true);
    }

    private static ClassMatcher createClassMatcher() {
        return new ExactClassMatcher("akka/dispatch/DefaultPromise");
    }

    private static MethodMatcher createMethodMatcher() {
        return new NameMethodMatcher("tryComplete");
    }

    protected PointCutInvocationHandler getPointCutInvocationHandlerImpl() {
        return this;
    }

    public void handleInvocation(ClassMethodSignature sig, Object object, Object[] args) {
        if ((object instanceof TransactionHolder)) {
            TransactionHolder promise = (TransactionHolder) object;
            Transaction tx = (Transaction) promise._nr_getTransaction();
            if ((tx == null) || (!tx.isStarted())) {
                return;
            }
            finishTracer(promise, args);
            tx.getTransactionState().asyncJobFinished(promise);
        }
    }

    private void finishTracer(TransactionHolder promise, Object[] args) {
        if ((promise instanceof AkkaTracerHolder)) {
            AkkaTracerHolder tracerHolder = (AkkaTracerHolder) promise;
            if ((tracerHolder._nr_getTracer() instanceof Tracer)) {
                Tracer tracer = (Tracer) tracerHolder._nr_getTracer();
                if ((args[0] instanceof Either)) {
                    Object resolved = ((Either) args[0]).get();
                  if ((resolved instanceof Throwable)) {
                    tracer.finish((Throwable) resolved);
                  } else {
                    tracer.finish(176, resolved);
                  }
                }
                tracerHolder._nr_setTracer(null);
            }
        }
    }
}