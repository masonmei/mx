package com.newrelic.agent.instrumentation;

import java.text.MessageFormat;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.classmatchers.ClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.tracers.AbstractTracerFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.IgnoreTransactionTracerFactory;
import com.newrelic.agent.tracers.PointCutInvocationHandler;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TracerFactory;
import com.newrelic.deps.com.google.common.collect.ComparisonChain;

public abstract class PointCut implements Comparable<PointCut>, ClassAndMethodMatcher {
    protected static final int HIGH_PRIORITY = 2147483647;
    protected static final int DEFAULT_PRIORITY = 20;
    protected static final int LOW_PRIORITY = -2147483648;
    private final ClassMatcher classMatcher;
    private final MethodMatcher methodMatcher;
    private final PointCutConfiguration config;
    private final boolean isIgnoreTransaction;
    private TracerFactory tracerFactory;
    private int priority = 20;

    protected PointCut(PointCutConfiguration config, ClassMatcher classMatcher, MethodMatcher methodMatcher) {
        assert (config != null);
        this.classMatcher = classMatcher;
        this.methodMatcher = methodMatcher;
        this.config = config;
        isIgnoreTransaction =
                ((Boolean) config.getConfiguration().getProperty("ignore_transaction", Boolean.valueOf(false)))
                        .booleanValue();
    }

    protected static MethodMatcher createMethodMatcher(MethodMatcher[] matchers) {
        return OrMethodMatcher.getMethodMatcher(matchers);
    }

    protected static MethodMatcher createExactMethodMatcher(String methodName, String[] methodDescriptions) {
        return new ExactMethodMatcher(methodName, methodDescriptions);
    }

    public MethodMatcher getMethodMatcher() {
        return methodMatcher;
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    protected void logInstrumentation(String className, Class<?> classBeingRedefined) {
        if (Agent.isDebugEnabled()) {
            Agent.LOG.finer(MessageFormat.format("Instrumenting {0} {1}", new Object[] {className, classBeingRedefined
                                                                                                           == null ? ""
                                                                                                           : "(Second pass)"}));
        }
    }

    public ClassMatcher getClassMatcher() {
        return classMatcher;
    }

    protected boolean isDispatcher() {
        return false;
    }

    public int getPriority() {
        return priority;
    }

    protected void setPriority(int priority) {
        this.priority = priority;
    }

    public final int compareTo(PointCut pc) {
        return ComparisonChain.start().compare(pc.getPriority(), getPriority())
                       .compare(getClass().getName(), pc.getClass().getName()).result();
    }

    public void noticeTransformerStarted(ClassTransformer classTransformer) {
    }

    protected abstract PointCutInvocationHandler getPointCutInvocationHandlerImpl();

    public final PointCutInvocationHandler getPointCutInvocationHandler() {
        return wrapHandler(isIgnoreTransaction() ? new IgnoreTransactionTracerFactory()
                                   : getPointCutInvocationHandlerImpl());
    }

    private PointCutInvocationHandler wrapHandler(final PointCutInvocationHandler pointCutInvocationHandler) {
        if ((isDispatcher()) || (!(pointCutInvocationHandler instanceof TracerFactory))) {
            return pointCutInvocationHandler;
        }
        if (tracerFactory == null) {
            tracerFactory = new AbstractTracerFactory() {
                public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object object,
                                          Object[] args) {
                    if ((!isDispatcher()) && (!transaction.isStarted())) {
                        return null;
                    }
                    if (transaction.getTransactionActivity().isFlyweight()) {
                        return null;
                    }
                    return ((TracerFactory) pointCutInvocationHandler).getTracer(transaction, sig, object, args);
                }
            };
        }
        return tracerFactory;
    }

    protected boolean isIgnoreTransaction() {
        return isIgnoreTransaction;
    }

    public String toString() {
        return config.getName() == null ? "PointCut:" + getPointCutInvocationHandler().getClass().getName()
                       : config.getName();
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (classMatcher == null ? 0 : classMatcher.hashCode());
        result = 31 * result + (methodMatcher == null ? 0 : methodMatcher.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PointCut other = (PointCut) obj;
        if (classMatcher == null) {
            if (other.classMatcher != null) {
                return false;
            }
        } else if (!classMatcher.equals(other.classMatcher)) {
            return false;
        }
        if (methodMatcher == null) {
            if (other.methodMatcher != null) {
                return false;
            }
        } else if (!methodMatcher.equals(other.methodMatcher)) {
            return false;
        }
        return true;
    }
}