package com.newrelic.agent.instrumentation.pointcuts.net;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.AbstractCrossProcessTracer;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.IOTracer;
import java.net.HttpURLConnection;

public class HttpURLConnectionTracer extends AbstractCrossProcessTracer
  implements IOTracer
{
  public HttpURLConnectionTracer(Transaction transaction, ClassMethodSignature sig, Object object, String host, String library, String uri, String methodName)
  {
    super(transaction, sig, object, host, library, uri, methodName);
  }

  protected String getHeaderValue(Object returnValue, String name)
  {
    HttpURLConnection connection = (HttpURLConnection)getInvocationTarget();
    return connection.getHeaderField(name);
  }

  protected void doRecordMetrics(TransactionStats transactionStats)
  {
    doRecordCrossProcessRollup(transactionStats);
  }
}