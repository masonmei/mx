package com.newrelic.agent;

import com.newrelic.agent.bridge.CrossProcessState;
import com.newrelic.agent.bridge.NoOpCrossProcessState;
import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.agent.bridge.NoOpWebResponse;
import com.newrelic.agent.bridge.WebResponse;
import com.newrelic.api.agent.ApplicationNamePriority;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;
import java.util.Map;

public class TransactionApiImpl
  implements com.newrelic.agent.bridge.Transaction
{
  public boolean equals(Object obj)
  {
    if (!(obj instanceof TransactionApiImpl)) {
      return false;
    }
    TransactionApiImpl objTxi = (TransactionApiImpl)obj;
    return getTransaction() == objTxi.getTransaction();
  }

  public int hashCode()
  {
    Transaction tx = getTransaction();
    return tx == null ? 42 : tx.hashCode();
  }

  public Transaction getTransaction()
  {
    return Transaction.getTransaction(false);
  }

  public boolean registerAsyncActivity(Object asyncContext)
  {
    return getTransaction().registerAsyncActivity(asyncContext);
  }

  public boolean startAsyncActivity(Object asyncContext)
  {
    return getTransaction().startAsyncActivity(asyncContext);
  }

  public boolean ignoreAsyncActivity(Object asyncContext)
  {
    return getTransaction().ignoreAsyncActivity(asyncContext);
  }

  public boolean setTransactionName(com.newrelic.api.agent.TransactionNamePriority namePriority, boolean override, String category, String[] parts)
  {
    Transaction tx = getTransaction();
    return tx != null ? tx.setTransactionName(namePriority, override, category, parts) : false;
  }

  public boolean isTransactionNameSet()
  {
    Transaction tx = getTransaction();
    return tx != null ? tx.isTransactionNameSet() : false;
  }

  public com.newrelic.api.agent.TracedMethod getLastTracer()
  {
    return getTracedMethod();
  }

  public com.newrelic.api.agent.TracedMethod getTracedMethod()
  {
    Transaction tx = getTransaction();
    if (tx == null) {
      return null;
    }
    TransactionActivity txa = tx.getTransactionActivity();
    if (txa == null) {
      return null;
    }
    return txa.getLastTracer();
  }

  public void ignore()
  {
    Transaction tx = getTransaction();
    if (tx != null)
      tx.ignore();
  }

  public void ignoreApdex()
  {
    Transaction tx = getTransaction();
    if (tx != null)
      tx.ignoreApdex();
  }

  public boolean setTransactionName(com.newrelic.agent.bridge.TransactionNamePriority namePriority, boolean override, String category, String[] parts)
  {
    Transaction tx = getTransaction();
    return tx != null ? tx.setTransactionName(namePriority, override, category, parts) : false;
  }

  public void beforeSendResponseHeaders()
  {
    Transaction tx = getTransaction();
    if (tx != null)
      tx.beforeSendResponseHeaders();
  }

  public boolean isStarted()
  {
    Transaction tx = getTransaction();
    return tx != null ? tx.isStarted() : false;
  }

  public void setApplicationName(ApplicationNamePriority priority, String appName)
  {
    Transaction tx = getTransaction();
    if (tx != null)
      tx.setApplicationName(priority, appName);
  }

  public boolean isAutoAppNamingEnabled()
  {
    Transaction tx = getTransaction();
    return tx != null ? tx.isAutoAppNamingEnabled() : false;
  }

  public boolean isWebRequestSet()
  {
    Transaction tx = getTransaction();
    return tx != null ? tx.isWebRequestSet() : false;
  }

  public boolean isWebResponseSet()
  {
    Transaction tx = getTransaction();
    return tx != null ? tx.isWebResponseSet() : false;
  }

  public void setWebRequest(Request request)
  {
    Transaction tx = getTransaction();
    if (tx != null)
      tx.setWebRequest(request);
  }

  public void setWebResponse(Response response)
  {
    Transaction tx = getTransaction();
    if (tx != null)
      tx.setWebResponse(response);
  }

  public WebResponse getWebResponse()
  {
    Transaction tx = getTransaction();
    return tx != null ? tx.getWebResponse() : NoOpWebResponse.INSTANCE;
  }

  public void convertToWebTransaction()
  {
    Transaction tx = getTransaction();
    if (tx != null)
      tx.convertToWebTransaction();
  }

  public boolean isWebTransaction()
  {
    Transaction tx = getTransaction();
    if (tx != null) {
      return tx.isWebTransaction();
    }
    return false;
  }

  public void requestInitialized(Request request, Response response)
  {
    Transaction tx = getTransaction();
    if (tx != null)
      tx.requestInitialized(request, response);
  }

  public void requestDestroyed()
  {
    Transaction tx = getTransaction();
    if (tx != null)
      tx.requestDestroyed();
  }

  public void saveMessageParameters(Map<String, String> parameters)
  {
    Transaction tx = getTransaction();
    if (tx != null)
      tx.saveMessageParameters(parameters);
  }

  public CrossProcessState getCrossProcessState()
  {
    Transaction tx = getTransaction();
    return tx != null ? tx.getCrossProcessState() : NoOpCrossProcessState.INSTANCE;
  }

  public com.newrelic.agent.bridge.TracedMethod startFlyweightTracer()
  {
    Transaction tx = getTransaction();
    if ((tx == null) || (!tx.isStarted())) {
      return null;
    }
    return tx.getTransactionActivity().startFlyweightTracer();
  }

  public void finishFlyweightTracer(com.newrelic.agent.bridge.TracedMethod parent, long startInNanos, long finishInNanos, String className, String methodName, String methodDesc, String metricName, String[] rollupMetricNames)
  {
    Transaction tx = getTransaction();
    if ((tx != null) && (tx.isStarted()))
      tx.getTransactionActivity().finishFlyweightTracer(parent, startInNanos, finishInNanos, className, methodName, methodDesc, metricName, rollupMetricNames);
  }

  public Map<String, Object> getAgentAttributes()
  {
    Transaction tx = getTransaction();
    return tx != null ? tx.getAgentAttributes() : NoOpTransaction.INSTANCE.getAgentAttributes();
  }

  public void provideHeaders(InboundHeaders headers)
  {
    Transaction tx = getTransaction();
    if (tx != null)
      tx.provideHeaders(headers);
  }

  public String getRequestMetadata()
  {
    Transaction tx = getTransaction();
    return tx != null ? tx.getCrossProcessState().getRequestMetadata() : NoOpCrossProcessState.INSTANCE.getRequestMetadata();
  }

  public void processRequestMetadata(String metadata)
  {
    Transaction tx = getTransaction();
    if (tx != null)
      tx.getCrossProcessState().processRequestMetadata(metadata);
  }

  public String getResponseMetadata()
  {
    Transaction tx = getTransaction();
    return tx != null ? tx.getCrossProcessState().getResponseMetadata() : NoOpCrossProcessState.INSTANCE.getResponseMetadata();
  }

  public void processResponseMetadata(String metadata)
  {
    Transaction tx = getTransaction();
    if (tx != null)
      tx.getCrossProcessState().processResponseMetadata(metadata);
  }
}