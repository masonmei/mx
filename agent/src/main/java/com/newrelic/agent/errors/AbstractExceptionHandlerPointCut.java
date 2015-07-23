package com.newrelic.agent.errors;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionErrorPriority;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;

public abstract class AbstractExceptionHandlerPointCut extends TracerFactoryPointCut {
    public AbstractExceptionHandlerPointCut(PointCutConfiguration config, ClassMatcher classMatcher,
                                            MethodMatcher methodMatcher) {
        super(config, classMatcher, methodMatcher);
    }

    public final Tracer doGetTracer(final Transaction transaction, ClassMethodSignature sig, Object errorHandler,
                                    Object[] args) {
        final Throwable throwable = getThrowable(sig, args);
        if (throwable == null) {
            return null;
        }
        return new DefaultTracer(transaction, sig, errorHandler, new ClassMethodMetricNameFormat(sig, errorHandler)) {
            protected void doFinish(int opcode, Object returnValue) {
                transaction.setThrowable(throwable, TransactionErrorPriority.API);

                super.doFinish(opcode, returnValue);
            }
        };
    }

    protected abstract Throwable getThrowable(ClassMethodSignature paramClassMethodSignature,
                                              Object[] paramArrayOfObject);
}