//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.pointcuts.scala;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.instrumentation.pointcuts.asynchttp.AsyncHttpClientRequestPointCut.AsyncHttpClientTracerInfo;
import com.newrelic.agent.instrumentation.pointcuts.asynchttp.AsyncHttpClientTracer;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.PointCutInvocationHandler;

@PointCut
public class ScalaAbstractPromiseCompletePointCut extends com.newrelic.agent.instrumentation.PointCut implements
        EntryInvocationHandler {
    public static final boolean DEFAULT_ENABLED = true;
    private static final String POINT_CUT_NAME = ScalaAbstractPromiseCompletePointCut.class.getName();
    private static final String DESC = "(Ljava/lang/Object;Ljava/lang/Object;)Z";
    private static final String METHOD = "updateState";

    public ScalaAbstractPromiseCompletePointCut(ClassTransformer classTransformer) {
        super(createPointCutConfig(), createClassMatcher(), createMethodMatcher());
    }

    private static PointCutConfiguration createPointCutConfig() {
        return new PointCutConfiguration(POINT_CUT_NAME, "scala_instrumentation", true);
    }

    private static ClassMatcher createClassMatcher() {
        return new ExactClassMatcher("scala/concurrent/impl/AbstractPromise");
    }

    private static MethodMatcher createMethodMatcher() {
        return new ExactMethodMatcher("updateState", "(Ljava/lang/Object;Ljava/lang/Object;)Z");
    }

    protected PointCutInvocationHandler getPointCutInvocationHandlerImpl() {
        return this;
    }

    public void handleInvocation(ClassMethodSignature sig, Object object, Object[] args) {
        boolean replacingTH = args[0] instanceof ScalaTransactionHolder;
        boolean beingLinked = args[1] instanceof ScalaTransactionHolder;
        boolean completing = args[1] instanceof ScalaTry;
        Object target = null;
        ScalaTry result = null;
        if (completing) {
            target = object;
            result = (ScalaTry) args[1];
        }

        if (beingLinked && !replacingTH) {
            target = args[1];
        }

        if (target instanceof ScalaTransactionHolder) {
            ScalaTransactionHolder promise = (ScalaTransactionHolder) object;
            Transaction tx = (Transaction) promise._nr_getTransaction();
            if (tx == null || !tx.isStarted()) {
                return;
            }

            this.finishTracer(promise, result);
            tx.getTransactionState().asyncJobFinished(promise);
        }

    }

    private void finishTracer(ScalaTransactionHolder promise, ScalaTry result) {
        if (promise instanceof ScalaTracerHolder) {
            ScalaTracerHolder tracerHolder = (ScalaTracerHolder) promise;
            if (tracerHolder._nr_getTracer() instanceof AsyncHttpClientTracerInfo) {
                AsyncHttpClientTracerInfo tracerInfo = (AsyncHttpClientTracerInfo) tracerHolder._nr_getTracer();
                Transaction savedTx = (Transaction) promise._nr_getTransaction();
                Transaction tx = Transaction.getTransaction();
                TransactionActivity txa = tx.getTransactionActivity();
                String txName = (String) promise._nr_getName();
                AsyncHttpClientTracer tracer =
                        new AsyncHttpClientTracer(tx, txName, tracerInfo.getClassMethodSignature(), (Object) null,
                                                         tracerInfo.getHost(), "AsyncHttpClient", tracerInfo.getUri(),
                                                         tracerInfo.getStartTime(), tracerInfo.getMethodName());
                tx.getTransactionActivity().tracerStarted(tracer);
                if (result instanceof ScalaSuccess) {
                    ScalaSuccess success = (ScalaSuccess) result;
                    tracer.setResponse(success._nr_value());
                    tracer.finish(176, success);
                    if (tx.getRootTracer() == tracer) {
                        savedTx.getTransactionState().asyncTransactionStarted(tx, promise);
                        savedTx.getTransactionState().asyncTransactionFinished(txa);
                    }
                } else if (result instanceof ScalaFailure) {
                    tracer.finish(((ScalaFailure) result)._nr_exception());
                }

                tracerHolder._nr_setTracer((Object) null);
            }
        }

    }
}
