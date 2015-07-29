package com.newrelic.agent.dispatchers;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.stats.ApdexStats;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.transaction.OtherTransactionNamer;
import com.newrelic.agent.transaction.TransactionNamer;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;

public class OtherDispatcher extends DefaultDispatcher {
    private final MetricNameFormat uri;

    public OtherDispatcher(Transaction transaction, MetricNameFormat uri) {
        super(transaction);
        this.uri = uri;
    }

    public void setTransactionName() {
        TransactionNamer tn = OtherTransactionNamer.create(getTransaction(), getUri());
        tn.setTransactionName();
    }

    public String getUri() {
        return this.uri.getMetricName();
    }

    public TransactionTracerConfig getTransactionTracerConfig() {
        return getTransaction().getAgentConfig().getBackgroundTransactionTracerConfig();
    }

    public boolean isAsyncTransaction() {
        return false;
    }

    public void transactionFinished(String transactionName, TransactionStats stats) {
        stats.getUnscopedStats().getResponseTimeStats(transactionName)
                .recordResponseTime(getTransaction().getTransactionTimer().getResponseTime(), 0L, TimeUnit.NANOSECONDS);

        String totalTimeMetric = getTransTotalName(transactionName, "OtherTransaction");
        if ((totalTimeMetric != null) && (totalTimeMetric.length() > 0)) {
            stats.getUnscopedStats().getResponseTimeStats(totalTimeMetric)
                    .recordResponseTime(getTransaction().getTransactionTimer().getTotalTime(), 0L,
                                               TimeUnit.NANOSECONDS);
        }

        stats.getUnscopedStats().getResponseTimeStats("OtherTransaction/all")
                .recordResponseTime(getTransaction().getTransactionTimer().getResponseTime(),
                                           getTransaction().getTransactionTimer().getResponseTime(),
                                           TimeUnit.NANOSECONDS);

        stats.getUnscopedStats().getResponseTimeStats("OtherTransactionTotalTime")
                .recordResponseTime(getTransaction().getTransactionTimer().getTotalTime(),
                                           getTransaction().getTransactionTimer().getTotalTime(), TimeUnit.NANOSECONDS);

        recordApdexMetrics(transactionName, stats);
    }

    private void recordApdexMetrics(String transactionName, TransactionStats stats) {
        if ((transactionName == null) || (transactionName.length() == 0)) {
            return;
        }
        if (!getTransaction().getAgentConfig().isApdexTSet(transactionName)) {
            return;
        }
        if (isIgnoreApdex()) {
            Agent.LOG.log(Level.FINE, "Ignoring transaction for apdex {0}", new Object[] {transactionName});
            return;
        }
        String apdexMetricName = getApdexMetricName(transactionName, "OtherTransaction", "ApdexOther/Transaction");

        if ((apdexMetricName == null) || (apdexMetricName.length() == 0)) {
            return;
        }
        long apdexT = getTransaction().getAgentConfig().getApdexTInMillis(transactionName);

        ApdexStats apdexStats = stats.getUnscopedStats().getApdexStats(apdexMetricName);
        ApdexStats overallApdexStats = stats.getUnscopedStats().getApdexStats("ApdexOther");

        long responseTimeInMillis = getTransaction().getTransactionTimer().getResponseTimeInMilliseconds();
        apdexStats.recordApdexResponseTime(responseTimeInMillis, apdexT);
        overallApdexStats.recordApdexResponseTime(responseTimeInMillis, apdexT);
    }

    public boolean isWebTransaction() {
        return false;
    }

    public String getCookieValue(String name) {
        return null;
    }

    public String getHeader(String name) {
        return null;
    }

    public Request getRequest() {
        return null;
    }

    public void setRequest(Request request) {
    }

    public Response getResponse() {
        return null;
    }

    public void setResponse(Response response) {
    }
}