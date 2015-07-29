//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.errors;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionErrorPriority;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.config.ErrorCollectorConfig;
import com.newrelic.agent.config.StripExceptionConfig;
import com.newrelic.agent.instrumentation.PointCut;
import com.newrelic.agent.instrumentation.methodmatchers.InvalidMethodDescriptor;
import com.newrelic.agent.instrumentation.yaml.PointCutFactory;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.ClassMethodSignature;

public class ErrorService {
  public static final int ERROR_LIMIT_PER_REPORTING_PERIOD = 20;
  public static final String STRIPPED_EXCEPTION_REPLACEMENT =
          "Message removed by New Relic \'strip_exception_messages\' setting";
  private static final Set<String> IGNORE_ERRORS;

  static {
    HashSet ignoreErrors = new HashSet(4);
    ignoreErrors.add("org.eclipse.jetty.continuation.ContinuationThrowable");
    ignoreErrors.add("org.mortbay.jetty.RetryRequest");
    IGNORE_ERRORS = Collections.unmodifiableSet(ignoreErrors);
  }

  protected final AtomicInteger errorCountThisHarvest = new AtomicInteger();
  private final AtomicInteger errorCount = new AtomicInteger();
  private final AtomicLong totalErrorCount = new AtomicLong();
  private final AtomicReferenceArray<TracedError> tracedErrors;
  private final boolean shouldRecordErrorCount;
  private final String appName;
  private volatile ErrorCollectorConfig errorCollectorConfig;
  private volatile StripExceptionConfig stripExceptionConfig;

  public ErrorService(String appName) {
    this.appName = appName;
    this.errorCollectorConfig = ServiceFactory.getConfigService().getErrorCollectorConfig(appName);
    this.stripExceptionConfig = ServiceFactory.getConfigService().getStripExceptionConfig(appName);
    this.tracedErrors = new AtomicReferenceArray(20);
    ServiceFactory.getTransactionService().addTransactionListener(new ErrorService.MyTransactionListener());
    ServiceFactory.getConfigService().addIAgentConfigListener(new ErrorService.MyConfigListener());
    this.shouldRecordErrorCount = !Boolean.getBoolean("com.newrelic.agent.errors.no_error_metric");
  }

  public static void reportException(Throwable throwable, Map<String, String> params) {
    Transaction tx = Transaction.getTransaction().getRootTransaction();
    if (tx.isInProgress()) {
      if (params != null) {
        tx.getErrorAttributes().putAll(params);
      }

      synchronized(tx) {
        tx.setThrowable(throwable, TransactionErrorPriority.API);
      }
    } else {
      String uri = '/' + Thread.currentThread().getName();
      ThrowableError error = new ThrowableError((String) null, "OtherTransaction" + uri, throwable, uri,
                                                       System.currentTimeMillis(), (Map) null, (Map) null,
                                                       (Map) null, params, (Map) null);
      ServiceFactory.getRPMService().getErrorService().reportError(error);
    }

  }

  public static void reportError(String message, Map<String, String> params) {
    Transaction tx = Transaction.getTransaction().getRootTransaction();
    if (tx.isInProgress()) {
      if (params != null) {
        tx.getErrorAttributes().putAll(params);
      }

      synchronized(tx) {
        tx.setThrowable(new ErrorService.ReportableError(message), TransactionErrorPriority.API);
      }
    } else {
      String uri = '/' + Thread.currentThread().getName();
      HttpTracedError error = new HttpTracedError((String) null, "OtherTransaction" + uri, 500, message, uri,
                                                         System.currentTimeMillis(), (Map) null, (Map) null,
                                                         (Map) null, params, (Map) null);
      ServiceFactory.getRPMService().getErrorService().reportError(error);
    }

  }

  public static Collection<? extends PointCut> getEnabledErrorHandlerPointCuts() {
    AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
    Object exceptionHandlers = config.getErrorCollectorConfig().getProperty("exception_handlers");
    if (exceptionHandlers == null) {
      return Collections.emptyList();
    } else {
      ArrayList pointcuts = new ArrayList();
      if (exceptionHandlers instanceof Collection) {
        Iterator i$ = ((Collection) exceptionHandlers).iterator();

        while (i$.hasNext()) {
          Object sigObject = i$.next();
          ExceptionHandlerPointCut pc;
          if (sigObject instanceof ExceptionHandlerSignature) {
            ExceptionHandlerSignature signature = (ExceptionHandlerSignature) sigObject;
            String e = MessageFormat.format("Instrumenting exception handler signature {0}",
                                                   new Object[] {signature.toString()});
            Agent.LOG.finer(e);
            pc = new ExceptionHandlerPointCut(signature);
            if (pc.isEnabled()) {
              pointcuts.add(pc);
            }
          } else if (sigObject instanceof String) {
            ClassMethodSignature signature1 =
                    PointCutFactory.parseClassMethodSignature(sigObject.toString());

            try {
              ExceptionHandlerSignature e1 = new ExceptionHandlerSignature(signature1);
              Agent.LOG.info(MessageFormat.format("Instrumenting exception handler signature {0}",
                                                         new Object[] {e1.toString()}));
              pc = new ExceptionHandlerPointCut(e1);
              if (pc.isEnabled()) {
                pointcuts.add(pc);
              }
            } catch (InvalidMethodDescriptor var8) {
              Agent.LOG.severe(MessageFormat.format("Unable to instrument exception handler {0} : {1}",
                                                           new Object[] {sigObject.toString(),
                                                                                var8.toString()}));
            }
          } else if (sigObject instanceof Exception) {
            Agent.LOG.severe(MessageFormat.format("Unable to instrument exception handler : {0}",
                                                         new Object[] {sigObject.toString()}));
          }
        }
      }

      return pointcuts;
    }
  }

  public static void reportHTTPError(String message, int statusCode, String uri) {
    HttpTracedError error = new HttpTracedError((String) null, "WebTransaction" + uri, statusCode, message, uri,
                                                       System.currentTimeMillis(), (Map) null, (Map) null,
                                                       (Map) null, (Map) null, (Map) null);
    ServiceFactory.getRPMService().getErrorService().reportError(error);
    Agent.LOG.finer(MessageFormat.format("Reported HTTP error {0} with status code {1} URI {2}",
                                                new Object[] {message, Integer.valueOf(statusCode), uri}));
  }

  public static String getStrippedExceptionMessage(Throwable throwable) {
    ErrorService errorService = ServiceFactory.getRPMService().getErrorService();
    return errorService.stripExceptionConfig.isEnabled() && !errorService.stripExceptionConfig.getWhitelist()
                                                                     .contains(throwable.getClass().getName())
                   ? "Message removed by New Relic \'strip_exception_messages\' setting" : throwable.getMessage();
  }

  protected void refreshErrorCollectorConfig(AgentConfig agentConfig) {
    ErrorCollectorConfig oldErrorConfig = this.errorCollectorConfig;
    this.errorCollectorConfig = agentConfig.getErrorCollectorConfig();
    if (this.errorCollectorConfig.isEnabled() != oldErrorConfig.isEnabled()) {
      String msg = MessageFormat.format("Errors will{0} be sent to New Relic for {1}",
                                               new Object[] {this.errorCollectorConfig.isEnabled() ? "" : " not",
                                                                    this.appName});
      Agent.LOG.info(msg);
    }
  }

  protected void refreshStripExceptionConfig(AgentConfig agentConfig) {
    StripExceptionConfig oldStripExceptionConfig = this.stripExceptionConfig;
    this.stripExceptionConfig = agentConfig.getStripExceptionConfig();
    if (this.stripExceptionConfig.isEnabled() != oldStripExceptionConfig.isEnabled()) {
      Agent.LOG.info(MessageFormat
                             .format("Exception messages will{0} be stripped before sending to New Relic for {1}",
                                            new Object[] {this.stripExceptionConfig.isEnabled() ? "" : " not",
                                                                 this.appName}));
    }

    if (!this.stripExceptionConfig.getWhitelist().equals(oldStripExceptionConfig.getWhitelist())) {
      Agent.LOG.info(MessageFormat.format("Exception message whitelist updated to {0} for {1}",
                                                 new Object[] {this.stripExceptionConfig.getWhitelist()
                                                                       .toString(), this.appName}));
    }

  }

  public void reportError(TracedError error) {
    if (error != null) {
      String msg;
      if (error instanceof ThrowableError && this.isIgnoredError(200, ((ThrowableError) error).getThrowable())) {
        if (Agent.LOG.isLoggable(Level.FINER)) {
          Throwable index1 = ((ThrowableError) error).getThrowable();
          msg = index1 == null ? "" : index1.getClass().getName();
          String msg1 = MessageFormat.format("Ignoring error {0} for {1}", new Object[] {msg, this.appName});
          Agent.LOG.finer(msg1);
        }

      } else {
        if (error.incrementsErrorMetric()) {
          this.errorCountThisHarvest.incrementAndGet();
        }

        if (this.errorCollectorConfig.isEnabled()) {
          if (this.errorCount.get() >= 20) {
            Agent.LOG.finer(MessageFormat.format("Error limit exceeded for {0}: {1}",
                                                        new Object[] {this.appName, error}));
          } else {
            int index = (int) this.totalErrorCount.getAndIncrement() % 20;
            if (this.tracedErrors.compareAndSet(index, null, error)) {
              this.errorCount.getAndIncrement();
              if (Agent.LOG.isLoggable(Level.FINER)) {
                msg = MessageFormat.format("Recording error for {0} : {1}",
                                                  new Object[] {this.appName, error});
                Agent.LOG.finer(msg);
              }
            }

          }
        }
      }
    }
  }

  public void reportErrors(TracedError... errors) {
    TracedError[] arr$ = errors;
    int len$ = errors.length;

    for (int i$ = 0; i$ < len$; ++i$) {
      TracedError error = arr$[i$];
      this.reportError(error);
    }

  }

  public List<TracedError> getTracedErrors() {
    ArrayList errors = new ArrayList(20);

    for (int i = 0; i < this.tracedErrors.length(); ++i) {
      TracedError error = (TracedError) this.tracedErrors.getAndSet(i, null);
      if (error != null) {
        this.errorCount.getAndDecrement();
        errors.add(error);
      }
    }

    return errors;
  }

  public List<TracedError> harvest(IRPMService rpmService, StatsEngine statsEngine) {
    if (!this.errorCollectorConfig.isEnabled()) {
      return Collections.emptyList();
    } else {
      this.recordMetrics(statsEngine);
      return rpmService.isConnected() ? this.getTracedErrors() : Collections.EMPTY_LIST;
    }
  }

  private void recordMetrics(StatsEngine statsEngine) {
    int errorCount = this.errorCountThisHarvest.getAndSet(0);
    if (this.shouldRecordErrorCount) {
      statsEngine.getStats("Errors/all").incrementCallCount(errorCount);
    }

  }

  private void noticeTransaction(TransactionData td, TransactionStats transactionStats) {
    if (this.appName.equals(td.getApplicationName())) {
      if (this.errorCollectorConfig.isEnabled()) {
        String statusMessage = td.getStatusMessage();
        int responseStatus = td.getResponseStatus();
        Throwable throwable = td.getThrowable();
        boolean isReportable = responseStatus >= 400 || throwable != null;
        if (throwable instanceof ErrorService.ReportableError) {
          statusMessage = throwable.getMessage();
          throwable = null;
        }

        if (isReportable) {
          if (this.isIgnoredError(td)) {
            if (Agent.LOG.isLoggable(Level.FINER)) {
              String error1 = throwable == null ? "" : throwable.getClass().getName();
              String msg = MessageFormat.format("Ignoring error {0} for {1} {2} ({3})",
                                                       new Object[] {error1, td.getRequestUri(),
                                                                            this.appName,
                                                                            Integer.valueOf(responseStatus)});
              Agent.LOG.finer(msg);
            }

            return;
          }

          Object error;
          if (throwable != null) {
            error = new ThrowableError(this.appName, td.getBlameOrRootMetricName(), throwable,
                                              td.getRequestUri(), td.getWallClockStartTimeMs(),
                                              td.getPrefixedAttributes(), td.getUserAttributes(),
                                              td.getAgentAttributes(), td.getErrorAttributes(),
                                              td.getIntrinsicAttributes());
          } else {
            error = new HttpTracedError(this.appName, td.getBlameOrRootMetricName(), responseStatus,
                                               statusMessage, td.getRequestUri(), td.getStartTimeInNanos(),
                                               td.getPrefixedAttributes(), td.getUserAttributes(),
                                               td.getAgentAttributes(), td.getErrorAttributes(),
                                               td.getIntrinsicAttributes());
          }

          if (this.shouldRecordErrorCount && ((TracedError) error).incrementsErrorMetric()) {
            this.recordErrorCount(td, transactionStats);
          }

          if (this.errorCount.get() < 20) {
            this.reportError((TracedError) error);
          } else if (((TracedError) error).incrementsErrorMetric()) {
            this.errorCountThisHarvest.incrementAndGet();
          }
        }

      }
    }
  }

  private void recordErrorCount(TransactionData td, TransactionStats transactionStats) {
    String metricName = this.getErrorCountMetricName(td);
    if (metricName != null) {
      transactionStats.getUnscopedStats().getStats(metricName).incrementCallCount();
    }

    String metricNameAll = td.isWebTransaction() ? "Errors/allWeb" : "Errors/allOther";
    transactionStats.getUnscopedStats().getStats(metricNameAll).incrementCallCount();
  }

  private String getErrorCountMetricName(TransactionData td) {
    String blameMetricName = td.getBlameMetricName();
    if (blameMetricName != null) {
      StringBuilder output = new StringBuilder("Errors/".length() + blameMetricName.length());
      output.append("Errors/");
      output.append(blameMetricName);
      return output.toString();
    } else {
      return null;
    }
  }

  public boolean isIgnoredError(int responseStatus, Throwable throwable) {
    if (this.errorCollectorConfig.getIgnoreStatusCodes().contains(Integer.valueOf(responseStatus))) {
      return true;
    } else {
      while (throwable != null) {
        String name = throwable.getClass().getName();
        if (this.errorCollectorConfig.getIgnoreErrors().contains(name)) {
          return true;
        }

        if (IGNORE_ERRORS.contains(name)) {
          return true;
        }

        throwable = throwable.getCause();
      }

      return false;
    }
  }

  public boolean isIgnoredError(TransactionData transactionData) {
    return this.isIgnoredError(transactionData.getResponseStatus(), transactionData.getThrowable());
  }

  private static class ReportableError extends Throwable {
    private static final long serialVersionUID = 3472056044517410355L;

    public ReportableError(String message) {
      super(message);
    }
  }

  private class MyConfigListener implements AgentConfigListener {
    private MyConfigListener() {
    }

    public void configChanged(String appName, AgentConfig agentConfig) {
      if (ErrorService.this.appName.equals(appName)) {
        Agent.LOG.fine(MessageFormat.format("Error service received configuration change notification for {0}",
                                                   new Object[] {appName}));
        ErrorService.this.refreshErrorCollectorConfig(agentConfig);
        ErrorService.this.refreshStripExceptionConfig(agentConfig);
      }

    }
  }

  private class MyTransactionListener implements TransactionListener {
    private MyTransactionListener() {
    }

    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
      ErrorService.this.noticeTransaction(transactionData, transactionStats);
    }
  }
}
