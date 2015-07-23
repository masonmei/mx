package com.newrelic.agent.instrumentation.pointcuts.scala;

import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionErrorPriority;
import com.newrelic.agent.async.AsyncTransactionState;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OrClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.FieldAccessor;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.instrumentation.pointcuts.TransactionHolder;
import com.newrelic.agent.tracers.AsyncRootTracer;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;

@PointCut
public class TransactionHolderDispatcherPointCut extends TracerFactoryPointCut {
    public static final boolean DEFAULT_ENABLED = true;
    public static final String SCALA_INSTRUMENTATION_GROUP_NAME = "scala_instrumentation";
    public static final TransactionHolder TRANSACTION_HOLDER = new TransactionHolder() {
        public Object _nr_getTransaction() {
            return null;
        }

        public void _nr_setTransaction(Object tx) {
        }

        public Object _nr_getName() {
            return null;
        }

        public void _nr_setName(Object tx) {
        }
    };

    private static final String POINT_CUT_NAME = TransactionHolderDispatcherPointCut.class.getName();

    public TransactionHolderDispatcherPointCut(ClassTransformer classTransformer) {
        super(createPointCutConfig(), createClassMatcher(), createMethodMatcher());
    }

    public TransactionHolderDispatcherPointCut(PointCutConfiguration config, ClassMatcher classMatcher,
                                               MethodMatcher methodMatcher) {
        super(config, classMatcher, methodMatcher);
    }

    private static PointCutConfiguration createPointCutConfig() {
        return new PointCutConfiguration(POINT_CUT_NAME, "scala_instrumentation", true);
    }

    private static ClassMatcher createClassMatcher() {
        return OrClassMatcher
                       .getClassMatcher(new ClassMatcher[] {new ExactClassMatcher
                                                                    ("scala/concurrent/impl/Future$PromiseCompletingRunnable"),
                                                                   new ExactClassMatcher
                                                                           ("scala/concurrent/impl/CallbackRunnable")});
    }

    private static MethodMatcher createMethodMatcher() {
        return new ExactMethodMatcher("run", "()V");
    }

    public Tracer doGetTracer(Transaction tx, ClassMethodSignature sig, Object object, Object[] args) {
        if ((object instanceof ScalaPromiseCompletingRunnable)) {
            object = ((ScalaPromiseCompletingRunnable) object)._nr_promise();
        }
        if ((object instanceof TransactionHolder)) {
            TransactionHolder txHolder = (TransactionHolder) object;
            Object obj = txHolder._nr_getTransaction();
            Transaction savedTx = null;
            if ((obj instanceof Transaction)) {
                savedTx = (Transaction) obj;
            } else {
                return null;
            }
            if (tx == savedTx) {
                tx.getTransactionState().asyncJobInvalidate(txHolder);
                Agent.LOG.log(Level.FINEST,
                                     "The transaction is the same transaction as its parent. Transaction: {0}. "
                                             + "Invalidating job {1}",
                                     new Object[] {tx, txHolder});

                return null;
            }
            if (tx.getDispatcher() != null) {
                Agent.LOG.log(Level.FINEST,
                                     "The job {0} is being run in an existing transaction {1}. Remove from parent "
                                             + "transaction: {2}",
                                     new Object[] {txHolder, tx, savedTx});

                savedTx.getTransactionState().asyncJobInvalidate(txHolder);
                return null;
            }
            if (savedTx.getRootTransaction().isIgnore()) {
                return null;
            }
            tx.setTransactionState(new AsyncTransactionState(tx.getTransactionActivity(),
                                                                    savedTx.getInitialTransactionActivity()));

            tx.getTransactionState().asyncJobStarted(TRANSACTION_HOLDER);
            tx.setRootTransaction(savedTx.getRootTransaction());
            savedTx.getTransactionState().asyncTransactionStarted(tx, txHolder);
            return createTracer(tx, sig, txHolder, savedTx);
        }

        Transaction.clearTransaction();
        return null;
    }

    protected boolean isDispatcher() {
        return true;
    }

    private Tracer createTracer(final Transaction tx, final ClassMethodSignature sig, final TransactionHolder txHolder,
                                final Transaction savedTx) {
        MetricNameFormat metricNameFormat = null;
        if (txHolder._nr_getName() == null) {
            metricNameFormat = new ClassMethodMetricNameFormat(sig, null);
        } else {
            metricNameFormat = new SimpleMetricNameFormat((String) txHolder._nr_getName());
        }
        return new AsyncRootTracer(tx, sig, txHolder, metricNameFormat) {
            public void finish(int opcode, Object returnValue) {
                super.finish(opcode, returnValue);
                if (sig.getClassName() != "scala/concurrent/impl/Future$PromiseCompletingRunnable") {
                    savedTx.getTransactionState().asyncJobFinished(txHolder);
                }

                Throwable t = tx.getReportError();
                if (t != null) {
                    savedTx.getRootTransaction().setThrowable(t, TransactionErrorPriority.ASYNC_POINTCUT);
                }
                if (tx.isIgnore()) {
                    savedTx.setIgnore(true);
                }
                tx.getTransactionState().asyncJobFinished(TransactionHolderDispatcherPointCut.TRANSACTION_HOLDER);
            }

            public final void finish(Throwable throwable) {
                super.finish(throwable);
                if (sig.getClassName() != "scala/concurrent/impl/Future$PromiseCompletingRunnable") {
                    savedTx.getTransactionState().asyncJobFinished(txHolder);
                }

                savedTx.getRootTransaction().setThrowable(throwable, TransactionErrorPriority.ASYNC_POINTCUT);
                if (tx.isIgnore()) {
                    savedTx.setIgnore(true);
                }
                tx.getTransactionState().asyncJobFinished(TransactionHolderDispatcherPointCut.TRANSACTION_HOLDER);
            }
        };
    }

    @InterfaceMixin(originalClassName = {"scala/concurrent/impl/Future$PromiseCompletingRunnable"})
    public static abstract interface ScalaPromiseCompletingRunnable {
        public static final String CLASS = "scala/concurrent/impl/Future$PromiseCompletingRunnable";

        @FieldAccessor(fieldName = "promise", fieldDesc = "Lscala/concurrent/impl/Promise$DefaultPromise;", existingField = true)
        public abstract Object _nr_promise();
    }
}