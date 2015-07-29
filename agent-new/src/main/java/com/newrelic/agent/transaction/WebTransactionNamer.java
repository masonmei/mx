package com.newrelic.agent.transaction;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.application.PriorityApplicationName;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.normalization.NormalizationService;
import com.newrelic.agent.normalization.Normalizer;
import com.newrelic.agent.service.ServiceFactory;
import java.text.MessageFormat;
import java.util.logging.Level;

public class WebTransactionNamer extends AbstractTransactionNamer
{
  private WebTransactionNamer(Transaction tx, String requestUri)
  {
    super(tx, requestUri);
  }

  public void setTransactionName()
  {
    if (!canSetTransactionName(TransactionNamePriority.STATUS_CODE)) {
      return;
    }
    Transaction tx = getTransaction();
    int responseStatusCode = tx.getStatus();
    if (responseStatusCode >= 400) {
      String normalizedStatus = normalizeStatus(responseStatusCode);
      if (Agent.LOG.isLoggable(Level.FINER)) {
        String msg = MessageFormat.format("Setting transaction name to \"{0}\" using response status", new Object[] { normalizedStatus });

        Agent.LOG.finer(msg);
      }
      if (canSetTransactionName(TransactionNamePriority.STATUS_CODE)) {
        setTransactionName(normalizedStatus, "NormalizedUri", TransactionNamePriority.STATUS_CODE);
        tx.freezeStatus();
      }
      return;
    }

    if (!canSetTransactionName()) {
      return;
    }

    String requestUri = getUri();

    if (requestUri == null) {
      if (Agent.LOG.isLoggable(Level.FINER)) {
        String msg = MessageFormat.format("Setting transaction name to \"{0}\" because request uri is null", new Object[] { "Unknown" });

        Agent.LOG.finer(msg);
      }
      setTransactionName("Unknown", "NormalizedUri", TransactionNamePriority.REQUEST_URI);
      return;
    }

    String appName = tx.getPriorityApplicationName().getName();

    String normalizedUri = ServiceFactory.getNormalizationService().getUrlNormalizer(appName).normalize(requestUri);
    if (normalizedUri == null) {
      if (Agent.LOG.isLoggable(Level.FINER)) {
        String msg = "Ignoring transaction because normalized request uri is null";
        Agent.LOG.finer(msg);
      }
      tx.setIgnore(true);
      return;
    }
    if (normalizedUri == requestUri) {
      if (Agent.LOG.isLoggable(Level.FINER)) {
        String msg = MessageFormat.format("Setting transaction name to \"{0}\" using request uri", new Object[] { requestUri });
        Agent.LOG.finer(msg);
      }
      setTransactionName(requestUri, "Uri", TransactionNamePriority.REQUEST_URI);
      return;
    }
    if (Agent.LOG.isLoggable(Level.FINER)) {
      String msg = MessageFormat.format("Setting transaction name to \"{0}\" using normalized request uri", new Object[] { normalizedUri });

      Agent.LOG.finer(msg);
    }
    setTransactionName(normalizedUri, "NormalizedUri", TransactionNamePriority.REQUEST_URI);
  }

  private static String normalizeStatus(int responseStatus)
  {
    return "/" + String.valueOf(responseStatus) + "/*";
  }

  public static TransactionNamer create(Transaction tx, String requestUri) {
    return new WebTransactionNamer(tx, requestUri);
  }
}