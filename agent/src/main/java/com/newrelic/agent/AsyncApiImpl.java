package com.newrelic.agent;

import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

import com.google.common.collect.MapMaker;
import com.newrelic.agent.bridge.AsyncApi;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.servlet.ServletAsyncTransactionStateImpl;
import com.newrelic.api.agent.Logger;

public class AsyncApiImpl implements AsyncApi {
    private final ConcurrentMap<Object, Transaction> asyncTransactions = new MapMaker().weakKeys().makeMap();
    private final Logger logger;

    public AsyncApiImpl(Logger logger) {
        this.logger = logger;
    }

    public void suspendAsync(Object asyncContext) {
        logger.log(Level.FINEST, "Suspend async");
        if (asyncContext != null) {
            Transaction currentTx = Transaction.getTransaction();
            TransactionState transactionState = setTransactionState(currentTx);
            transactionState.suspendRootTracer();
            asyncTransactions.put(asyncContext, currentTx);
        }
    }

    private TransactionState setTransactionState(Transaction tx) {
        TransactionState txState = tx.getTransactionState();
        if ((txState instanceof ServletAsyncTransactionStateImpl)) {
            return txState;
        }
        txState = new ServletAsyncTransactionStateImpl(tx);
        tx.setTransactionState(txState);
        return txState;
    }

    public com.newrelic.agent.bridge.Transaction resumeAsync(Object asyncContext) {
        logger.log(Level.FINEST, "Resume async");
        if (asyncContext != null) {
            Transaction suspendedTx = asyncTransactions.get(asyncContext);
            if (suspendedTx != null) {
                suspendedTx.getTransactionState().resume();
                if (suspendedTx.isStarted()) {
                    suspendedTx.getTransactionState().getRootTracer();
                    return new BoundTransactionApiImpl(suspendedTx);
                }
            }
        }
        return new TransactionApiImpl();
    }

    public void completeAsync(Object asyncContext) {
        logger.log(Level.FINEST, "Complete async");
        if (asyncContext == null) {
            return;
        }
        Transaction transaction = asyncTransactions.remove(asyncContext);
        if (transaction != null) {
            transaction.getTransactionState().complete();
        }
    }

    public void errorAsync(Object asyncContext, Throwable t) {
        logger.log(Level.FINEST, "Error async");
        if ((asyncContext == null) || (t == null)) {
            return;
        }
        Transaction transaction = asyncTransactions.get(asyncContext);
        if (transaction != null) {
            transaction.setThrowable(t, TransactionErrorPriority.API);
        }
    }

    public void finishRootTracer() {
        Transaction currentTx = Transaction.getTransaction();
        Tracer rootTracer = currentTx.getRootTracer();
        if (rootTracer != null) {
            rootTracer.finish(177, null);
        }
    }
}