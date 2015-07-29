package com.newrelic.agent.tracers;

import java.util.concurrent.TimeUnit;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.InboundHeaders;

public abstract class AbstractCrossProcessTracer extends AbstractExternalComponentTracer implements InboundHeaders {
    private final String uri;
    private CrossProcessNameFormat crossProcessFormat;
    private Object response;

    public AbstractCrossProcessTracer(Transaction transaction, ClassMethodSignature sig, Object object, String host,
                                      String library, String uri, String methodName) {
        super(transaction, sig, object, host, library, uri, new String[] {methodName});
        this.uri = uri;
    }

    protected void doFinish(int opcode, Object returnValue) {
        this.response = returnValue;
        getTransaction().getCrossProcessState().processInboundResponseHeaders(this, this, getHost(), this.uri, false);
        super.doFinish(opcode, returnValue);
    }

    public void doFinish(Throwable throwable) {
        getTransaction().getCrossProcessState().processInboundResponseHeaders(this, this, getHost(), this.uri, false);
        super.doFinish(throwable);
    }

    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    public String getHeader(String name) {
        return getHeaderValue(this.response, name);
    }

    protected abstract String getHeaderValue(Object paramObject, String paramString);

    public void setMetricNameFormat(MetricNameFormat nameFormat) {
        super.setMetricNameFormat(nameFormat);
        if ((nameFormat instanceof CrossProcessNameFormat)) {
            this.crossProcessFormat = ((CrossProcessNameFormat) nameFormat);
        }
    }

    protected void doRecordMetrics(TransactionStats transactionStats) {
        super.doRecordMetrics(transactionStats);
        doRecordCrossProcessRollup(transactionStats);
    }

    protected void doRecordCrossProcessRollup(TransactionStats transactionStats) {
        if (this.crossProcessFormat != null) {
            String hostCrossProcessIdRollupMetricName = this.crossProcessFormat.getHostCrossProcessIdRollupMetricName();
            transactionStats.getUnscopedStats().getResponseTimeStats(hostCrossProcessIdRollupMetricName)
                    .recordResponseTime(getExclusiveDuration(), TimeUnit.NANOSECONDS);
        }
    }

    public String getUri() {
        return this.uri;
    }
}