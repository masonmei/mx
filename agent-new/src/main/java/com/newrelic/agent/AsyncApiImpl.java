package com.newrelic.agent;

import com.newrelic.agent.bridge.AsyncApi;
import com.newrelic.deps.com.google.common.collect.MapMaker;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.servlet.ServletAsyncTransactionStateImpl;
import com.newrelic.api.agent.Logger;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

public class AsyncApiImpl
  implements AsyncApi
{
  private final ConcurrentMap<Object, Transaction> asyncTransactions = new MapMaker().weakKeys().makeMap();
  private final Logger logger;

  public AsyncApiImpl(Logger logger)
  {
    this.logger = logger;
  }

  public void suspendAsync(Object asyncContext)
  {
    this.logger.log(Level.FINEST, "Suspend async", new Object[0]);
    if (asyncContext != null) {
      Transaction currentTx = Transaction.getTransaction();
      TransactionState transactionState = setTransactionState(currentTx);
      transactionState.suspendRootTracer();
      this.asyncTransactions.put(asyncContext, currentTx);
    }
  }

  private TransactionState setTransactionState(Transaction tx)
  {
    TransactionState txState = tx.getTransactionState();
    if ((txState instanceof ServletAsyncTransactionStateImpl)) {
      return txState;
    }
    txState = new ServletAsyncTransactionStateImpl(tx);
    tx.setTransactionState(txState);
    return txState;
  }

  public com.newrelic.agent.bridge.Transaction resumeAsync(Object asyncContext)
  {
    this.logger.log(Level.FINEST, "Resume async", new Object[0]);
    if (asyncContext != null) {
      Transaction suspendedTx = (Transaction)this.asyncTransactions.get(asyncContext);
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

  public void completeAsync(Object asyncContext)
  {
    this.logger.log(Level.FINEST, "Complete async", new Object[0]);
    if (asyncContext == null) {
      return;
    }
    Transaction transaction = (Transaction)this.asyncTransactions.remove(asyncContext);
    if (transaction != null)
      transaction.getTransactionState().complete();
  }

  public void errorAsync(Object asyncContext, Throwable t)
  {
    this.logger.log(Level.FINEST, "Error async", new Object[0]);
    if ((asyncContext == null) || (t == null)) {
      return;
    }
    Transaction transaction = (Transaction)this.asyncTransactions.get(asyncContext);
    if (transaction != null)
      transaction.setThrowable(t, TransactionErrorPriority.API);
  }

  public void finishRootTracer()
  {
    Transaction currentTx = Transaction.getTransaction();
    Tracer rootTracer = currentTx.getRootTracer();
    if (rootTracer != null)
      rootTracer.finish(177, null);
  }
}