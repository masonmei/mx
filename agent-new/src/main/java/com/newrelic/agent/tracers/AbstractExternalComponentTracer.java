package com.newrelic.agent.tracers;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.SimpleStatsEngine;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.util.Strings;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

public abstract class AbstractExternalComponentTracer extends DefaultTracer
  implements IgnoreChildSocketCalls
{
  private static final String UNKNOWN_HOST = "UnknownHost";
  private String host;

  public AbstractExternalComponentTracer(Transaction transaction, ClassMethodSignature sig, Object object, String host, String library, String uri, String[] operations)
  {
    this(transaction, sig, object, host, library, false, uri, operations);
  }

  public AbstractExternalComponentTracer(Transaction transaction, ClassMethodSignature sig, Object object, String host, String library, boolean includeOperationInMetric, String uri, String[] operations)
  {
    super(transaction, sig, object, ExternalComponentNameFormat.create(host, library, includeOperationInMetric, uri, operations));

    this.host = host;
  }

  public AbstractExternalComponentTracer(Transaction transaction, ClassMethodSignature sig, Object object, String host, MetricNameFormat metricNameFormat)
  {
    super(transaction, sig, object, metricNameFormat);
    this.host = host;
  }

  public String getHost() {
    return this.host;
  }

  public void finish(Throwable throwable)
  {
    if ((throwable instanceof UnknownHostException)) {
      this.host = "UnknownHost";
      MetricNameFormat metricNameFormat = getMetricNameFormat();
      if ((metricNameFormat instanceof ExternalComponentNameFormat)) {
        setMetricNameFormat(((ExternalComponentNameFormat)metricNameFormat).cloneWithNewHost("UnknownHost"));
      }
    }
    super.finish(throwable);
  }

  protected void doRecordMetrics(TransactionStats transactionStats)
  {
    super.doRecordMetrics(transactionStats);

    transactionStats.getUnscopedStats().getResponseTimeStats("External/all").recordResponseTime(getExclusiveDuration(), TimeUnit.NANOSECONDS);

    transactionStats.getUnscopedStats().getResponseTimeStats(getTransaction().isWebTransaction() ? "External/allWeb" : "External/allOther").recordResponseTime(getExclusiveDuration(), TimeUnit.NANOSECONDS);

    String hostRollupMetricName = Strings.join('/', new String[] { "External", getHost(), "all" });
    transactionStats.getUnscopedStats().getResponseTimeStats(hostRollupMetricName).recordResponseTime(getExclusiveDuration(), TimeUnit.NANOSECONDS);
  }
}