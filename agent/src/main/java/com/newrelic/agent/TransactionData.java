package com.newrelic.agent;

import java.util.Collection;
import java.util.Map;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.sql.SqlTracerListener;
import com.newrelic.agent.stats.ApdexPerfZone;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.transaction.PriorityTransactionName;
import com.newrelic.agent.transaction.TransactionTimer;
import com.newrelic.api.agent.Insights;

public class TransactionData {
    private final Transaction tx;
    private final int transactionSize;
    private final long threadId;

    public TransactionData(Transaction transaction, int transactionSize) {
        tx = transaction;
        this.transactionSize = transactionSize;
        threadId = Thread.currentThread().getId();
    }

    public Insights getInsightsData() {
        return tx.getInsightsData();
    }

    public Dispatcher getDispatcher() {
        return tx.getDispatcher();
    }

    public TransactionTimer getTransactionTime() {
        return tx.getTransactionTimer();
    }

    public Tracer getRootTracer() {
        return tx.getRootTracer();
    }

    public Collection<Tracer> getTracers() {
        return tx.getAllTracers();
    }

    public long getWallClockStartTimeMs() {
        return tx.getWallClockStartTimeMs();
    }

    public long getStartTimeInNanos() {
        return tx.getTransactionTimer().getStartTime();
    }

    public long getEndTimeInNanos() {
        return tx.getTransactionTimer().getEndTime();
    }

    public String getRequestUri() {
        return getDispatcher().getUri();
    }

    public int getResponseStatus() {
        return tx.getStatus();
    }

    public String getStatusMessage() {
        return tx.getStatusMessage();
    }

    public final long getThreadId() {
        return threadId;
    }

    public String getApplicationName() {
        return tx.getRPMService().getApplicationName();
    }

    public AgentConfig getAgentConfig() {
        return tx.getAgentConfig();
    }

    public TransactionTracerConfig getTransactionTracerConfig() {
        return getAgentConfig() == null ? null : getAgentConfig().getTransactionTracerConfig();
    }

    public Map<String, Object> getInternalParameters() {
        return tx.getInternalParameters();
    }

    public Map<String, Map<String, String>> getPrefixedAttributes() {
        return tx.getPrefixedAgentAttributes();
    }

    public Map<String, Object> getUserAttributes() {
        return tx.getUserAttributes();
    }

    public Map<String, Object> getAgentAttributes() {
        return tx.getAgentAttributes();
    }

    public Map<String, String> getErrorAttributes() {
        return tx.getErrorAttributes();
    }

    public Map<String, Object> getIntrinsicAttributes() {
        return tx.getIntrinsicAttributes();
    }

    public PriorityTransactionName getPriorityTransactionName() {
        return tx.getPriorityTransactionName();
    }

    public String getBlameMetricName() {
        return getPriorityTransactionName().getName();
    }

    public String getBlameOrRootMetricName() {
        return getBlameMetricName() == null ? getRootTracer().getMetricName() : getBlameMetricName();
    }

    public Throwable getThrowable() {
        return tx.getReportError();
    }

    protected final int getTransactionSize() {
        return transactionSize;
    }

    public long getDurationInMillis() {
        return tx.getTransactionTimer().getResponseTimeInMilliseconds();
    }

    public long getDuration() {
        return tx.getTransactionTimer().getResponseTime();
    }

    public String getGuid() {
        return tx.getGuid();
    }

    public String getReferrerGuid() {
        return tx.getInboundHeaderState().getReferrerGuid();
    }

    public String getTripId() {
        return tx.getCrossProcessTransactionState().getTripId();
    }

    public int generatePathHash() {
        return tx.getCrossProcessTransactionState().generatePathHash();
    }

    public Integer getReferringPathHash() {
        return tx.getInboundHeaderState().getReferringPathHash();
    }

    public String getAlternatePathHashes() {
        return tx.getCrossProcessTransactionState().getAlternatePathHashes();
    }

    public String getSyntheticsResourceId() {
        return tx.getInboundHeaderState().getSyntheticsResourceId();
    }

    public String getSyntheticsJobId() {
        return tx.getInboundHeaderState().getSyntheticsJobId();
    }

    public String getSyntheticsMonitorId() {
        return tx.getInboundHeaderState().getSyntheticsMonitorId();
    }

    public ApdexPerfZone getApdexPerfZone() {
        if ((!isWebTransaction()) && (!tx.getAgentConfig().isApdexTSet(getPriorityTransactionName().getName()))) {
            return null;
        }
        long responseTimeInMillis = tx.getTransactionTimer().getResponseTimeInMilliseconds() + tx.getExternalTime();
        long apdexTInMillis = tx.getAgentConfig().getApdexTInMillis(getPriorityTransactionName().getName());
        return ApdexPerfZone.getZone(responseTimeInMillis, apdexTInMillis);
    }

    public boolean isWebTransaction() {
        return getDispatcher().isWebTransaction();
    }

    public boolean isSyntheticTransaction() {
        return tx.isSynthetic();
    }

    public SqlTracerListener getSqlTracerListener() {
        return tx.getSqlTracerListener();
    }

    public String toString() {
        String name = getRequestUri() == null ? getRootTracer().getMetricName() : getRequestUri();
        StringBuilder builder =
                new StringBuilder(name == null ? "" : name).append(' ').append(getDurationInMillis()).append("ms");

        if (getThrowable() != null) {
            builder.append(' ').append(getThrowable().toString());
        }
        return builder.toString();
    }
}