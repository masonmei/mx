package com.newrelic.agent.tracers;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.transaction.PriorityTransactionName;
import com.newrelic.agent.util.Strings;

public abstract class AbstractTracer implements Tracer {
    private final TransactionActivity transactionActivity;
    private Set<String> rollupMetricNames;
    private Set<String> exclusiveRollupMetricNames;

    public AbstractTracer(Transaction transaction) {
        this(transaction.getTransactionActivity());
    }

    public AbstractTracer(TransactionActivity txa) {
        this.transactionActivity = txa;
    }

    public final Transaction getTransaction() {
        return this.transactionActivity.getTransaction();
    }

    public final TransactionActivity getTransactionActivity() {
        return this.transactionActivity;
    }

    protected Object getInvocationTarget() {
        return null;
    }

    public final Object invoke(Object methodName, Method method, Object[] args) {
        try {
            if (args == null) {
                Agent.LOG.severe("Tracer.finish() was invoked with no arguments");
            } else if ("s" == methodName) {
                if (args.length == 2) {
                    finish(((Integer) args[0]).intValue(), args[1]);
                } else {
                    Agent.LOG.severe(MessageFormat
                                             .format("Tracer.finish(int, Object) was invoked with {0} arguments(s)",
                                                            new Object[] {Integer.valueOf(args.length)}));
                }
            } else if ("u" == methodName) {
                if (args.length == 1) {
                    finish((Throwable) args[0]);
                } else {
                    Agent.LOG.severe(MessageFormat.format("Tracer.finish(Throwable) was invoked with {0} arguments(s)",
                                                                 new Object[] {Integer.valueOf(args.length)}));
                }
            } else {
                Agent.LOG.severe(MessageFormat.format("Tracer.finish was invoked with an unknown method: {0}",
                                                             new Object[] {methodName}));
            }
        } catch (RetryException e) {
            return invoke(methodName, method, args);
        } catch (Throwable t) {
            if (Agent.LOG.isLoggable(Level.FINE)) {
                String msg = MessageFormat
                                     .format("An error occurred finishing method tracer {0} for signature {1} : {2}",
                                                    new Object[] {getClass().getName(), getClassMethodSignature(),
                                                                         t.toString()});

                if (Agent.LOG.isLoggable(Level.FINEST)) {
                    Agent.LOG.log(Level.FINEST, msg, t);
                } else {
                    Agent.LOG.fine(msg);
                }
            }
        }
        return null;
    }

    public abstract ClassMethodSignature getClassMethodSignature();

    public boolean isChildHasStackTrace() {
        return false;
    }

    public void nameTransaction(TransactionNamePriority priority) {
        try {
            ClassMethodSignature classMethodSignature = getClassMethodSignature();
            Object invocationTarget = getInvocationTarget();
            String className = invocationTarget == null ? classMethodSignature.getClassName()
                                       : invocationTarget.getClass().getName();

            String txName = "/Custom/" + className + '/' + classMethodSignature.getMethodName();
            Agent.LOG.log(Level.FINER, "Setting transaction name using instrumented class and method: {0}",
                                 new Object[] {txName});
            Transaction tx = this.transactionActivity.getTransaction();
            tx.setPriorityTransactionName(PriorityTransactionName.create(tx, txName, "Custom", priority));
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINEST, "nameTransaction", t);
        }
    }

    public TracedMethod getParentTracedMethod() {
        return getParentTracer();
    }

    public boolean isLeaf() {
        return false;
    }

    protected Set<String> getRollupMetricNames() {
        return this.rollupMetricNames;
    }

    public void setRollupMetricNames(String[] metricNames) {
        this.rollupMetricNames = new HashSet(metricNames.length);
        for (String metricName : metricNames) {
            this.rollupMetricNames.add(metricName);
        }
    }

    protected Set<String> getExclusiveRollupMetricNames() {
        return this.exclusiveRollupMetricNames;
    }

    public void addRollupMetricName(String[] metricNameParts) {
        if (this.rollupMetricNames == null) {
            this.rollupMetricNames = new HashSet();
        }
        this.rollupMetricNames.add(Strings.join('/', metricNameParts));
    }

    public void addExclusiveRollupMetricName(String[] metricNameParts) {
        if (this.exclusiveRollupMetricNames == null) {
            this.exclusiveRollupMetricNames = new HashSet();
        }
        this.exclusiveRollupMetricNames.add(Strings.join('/', metricNameParts));
    }
}