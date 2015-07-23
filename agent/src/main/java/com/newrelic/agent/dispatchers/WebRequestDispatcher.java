package com.newrelic.agent.dispatchers;

import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.StatusCodePolicy;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.bridge.WebResponse;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.servlet.ServletUtils;
import com.newrelic.agent.stats.ApdexStats;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.servlet.ExternalTimeTracker;
import com.newrelic.agent.transaction.TransactionNamer;
import com.newrelic.agent.transaction.WebTransactionNamer;
import com.newrelic.agent.util.Strings;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;

public class WebRequestDispatcher extends DefaultDispatcher implements WebResponse {
    private static final String UNKNOWN_URI = "/Unknown";
    private static final StatusCodePolicy LAST_STATUS_CODE_POLICY = new StatusCodePolicy() {
        public int nextStatus(int currentStatus, int lastStatus) {
            return lastStatus;
        }
    };

    private static final StatusCodePolicy ERROR_STATUS_CODE_POLICY = new StatusCodePolicy() {
        public int nextStatus(int currentStatus, int lastStatus) {
            return currentStatus < 400 ? lastStatus : currentStatus;
        }
    };

    private static final StatusCodePolicy FREEZE_STATUS_CODE_POLICY = new StatusCodePolicy() {
        public int nextStatus(int currentStatus, int lastStatus) {
            return currentStatus;
        }
    };
    private Request request;
    private Response response;
    private String requestURI;
    private ExternalTimeTracker externalTimeTracker;
    private int statusCode;
    private String statusMessage;
    private StatusCodePolicy statusCodePolicy;

    public WebRequestDispatcher(Request request, Response response, Transaction transaction) {
        super(transaction);

        boolean isLastStatusCodePolicy =
                ((Boolean) transaction.getAgentConfig().getValue("last_status_code_policy", Boolean.TRUE))
                        .booleanValue();

        statusCodePolicy = (isLastStatusCodePolicy ? LAST_STATUS_CODE_POLICY : ERROR_STATUS_CODE_POLICY);
        this.request = request;
        this.response = response;
        externalTimeTracker = ExternalTimeTracker.create(request, transaction.getWallClockStartTimeMs());
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        externalTimeTracker = ExternalTimeTracker.create(request, getTransaction().getWallClockStartTimeMs());
        this.request = request;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public void transactionFinished(String transactionName, TransactionStats stats) {
        if (request != null) {
            try {
                setStatus();
                freezeStatus();
                setStatusMessage();
                doRecordMetrics(transactionName, stats);

                ServletUtils.recordParameters(getTransaction(), request);
                storeReferrer();
                if (getStatus() > 0) {
                    getTransaction().getAgentAttributes().put("httpResponseCode", String.valueOf(getStatus()));
                }
                if (getStatusMessage() != null) {
                    getTransaction().getAgentAttributes().put("httpResponseMessage", getStatusMessage());
                }
            } finally {
                request = null;
                response = null;
            }
        }
    }

    public final String getUri() {
        if (requestURI == null) {
            requestURI = initializeRequestURI();
        }
        return requestURI;
    }

    public void setTransactionName() {
        if (Transaction.isDummyRequest(request)) {
            Tracer rootTracer = getTransaction().getRootTracer();
            if (rootTracer != null) {
                rootTracer.nameTransaction(TransactionNamePriority.REQUEST_URI);
            }
        }

        TransactionNamer tn = WebTransactionNamer.create(getTransaction(), getUri());
        tn.setTransactionName();
    }

    private String initializeRequestURI() {
        String result = "/Unknown";
        if (request == null) {
            return result;
        }
        try {
            String uri = request.getRequestURI();
            if ((uri == null) || (uri.length() == 0)) {
                Agent.LOG.log(Level.FINER, "requestURI is null: setting requestURI to {0}", new Object[] {result});
            } else {
                result = ServiceFactory.getNormalizationService().getUrlBeforeParameters(uri);
            }
        } catch (Throwable e) {
            Agent.LOG.severe("Error calling requestURI: " + e.toString());
            Agent.LOG.log(Level.FINEST, e.toString(), e);
            result = "/Unknown";
        }
        return result;
    }

    private void storeReferrer() {
        try {
            String referer = request.getHeader("Referer");
            if (referer != null) {
                referer = referer.split("\\?")[0];
                getTransaction().getAgentAttributes().put("request.headers.referer", referer);
            }
        } catch (Throwable e) {
            Agent.LOG.finer("Error getting referer: " + e.toString());
            Agent.LOG.log(Level.FINEST, e.toString(), e);
        }
    }

    public void freezeStatus() {
        statusCodePolicy = FREEZE_STATUS_CODE_POLICY;
        Agent.LOG.log(Level.FINER, "Freezing status code to {0}", new Object[] {Integer.valueOf(getStatus())});
    }

    private void setStatus() {
        if (response != null) {
            try {
                setStatus(response.getStatus());
            } catch (Exception e) {
                Agent.LOG.log(Level.FINER, "Failed to get response status code {0}", new Object[] {e.toString()});
            }
        }
    }

    private void setStatusMessage() {
        if ((response != null) && (getStatusMessage() == null) && (getStatus() >= 400)) {
            try {
                setStatusMessage(response.getStatusMessage());
            } catch (Exception e) {
                Agent.LOG.log(Level.FINER, "Failed to get response status message {0}", new Object[] {e.toString()});
            }
        }
    }

    private void doRecordMetrics(String transactionName, TransactionStats stats) {
        recordHeaderMetrics(stats);
        recordApdexMetrics(transactionName, stats);
        recordDispatcherMetrics(transactionName, stats);
    }

    public void recordHeaderMetrics(TransactionStats statsEngine) {
        externalTimeTracker.recordMetrics(statsEngine);
    }

    public long getQueueTime() {
        return externalTimeTracker.getExternalTime();
    }

    private void recordDispatcherMetrics(String frontendMetricName, TransactionStats stats) {
        if ((frontendMetricName == null) || (frontendMetricName.length() == 0)) {
            return;
        }
        long frontendTimeInNanos = getTransaction().getTransactionTimer().getResponseTime();

        stats.getUnscopedStats().getResponseTimeStats(frontendMetricName)
                .recordResponseTimeInNanos(frontendTimeInNanos, 0L);

        stats.getUnscopedStats().getResponseTimeStats("WebTransaction").recordResponseTimeInNanos(frontendTimeInNanos);

        stats.getUnscopedStats().getResponseTimeStats("HttpDispatcher").recordResponseTimeInNanos(frontendTimeInNanos);

        if (getStatus() > 0) {
            String metricName = Strings.join(new String[] {"Network/Inbound/StatusCode/", String.valueOf(getStatus())});
            stats.getUnscopedStats().getResponseTimeStats(metricName).recordResponseTimeInNanos(frontendTimeInNanos);
        }

        String totalTimeMetric = getTransTotalName(frontendMetricName, "WebTransaction");
        if ((totalTimeMetric != null) && (totalTimeMetric.length() > 0)) {
            stats.getUnscopedStats().getResponseTimeStats(totalTimeMetric)
                    .recordResponseTimeInNanos(getTransaction().getTransactionTimer().getTotalTime());
        }

        stats.getUnscopedStats().getResponseTimeStats("WebTransactionTotalTime")
                .recordResponseTimeInNanos(getTransaction().getTransactionTimer().getTotalTime());
    }

    private void recordApdexMetrics(String frontendMetricName, TransactionStats stats) {
        if ((frontendMetricName == null) || (frontendMetricName.length() == 0)) {
            return;
        }
        if (!getTransaction().getAgentConfig().isApdexTSet()) {
            return;
        }
        if (isIgnoreApdex()) {
            Agent.LOG.log(Level.FINE, "Ignoring transaction for apdex {0}", new Object[] {frontendMetricName});
            return;
        }
        String frontendApdexMetricName = getApdexMetricName(frontendMetricName, "WebTransaction", "Apdex");

        if ((frontendApdexMetricName == null) || (frontendApdexMetricName.length() == 0)) {
            return;
        }
        long apdexT = getTransaction().getAgentConfig().getApdexTInMillis(frontendMetricName);

        ApdexStats apdexStats = stats.getUnscopedStats().getApdexStats(frontendApdexMetricName);
        ApdexStats overallApdexStats = stats.getUnscopedStats().getApdexStats("Apdex");
        if (isApdexFrustrating()) {
            apdexStats.recordApdexFrustrated();
            overallApdexStats.recordApdexFrustrated();
        } else {
            long responseTimeInMillis =
                    getTransaction().getTransactionTimer().getResponseTimeInMilliseconds() + externalTimeTracker
                                                                                                     .getExternalTime();

            apdexStats.recordApdexResponseTime(responseTimeInMillis, apdexT);
            overallApdexStats.recordApdexResponseTime(responseTimeInMillis, apdexT);
        }
    }

    public boolean isApdexFrustrating() {
        String appName = getTransaction().getPriorityApplicationName().getName();
        IRPMService rpmService = ServiceFactory.getRPMService(appName);
        return (getTransaction().getStatus() >= 400) && (!rpmService.getErrorService()
                                                                  .isIgnoredError(getTransaction().getStatus(),
                                                                                         getTransaction()
                                                                                                 .getReportError()));
    }

    public TransactionTracerConfig getTransactionTracerConfig() {
        return getTransaction().getAgentConfig().getRequestTransactionTracerConfig();
    }

    public boolean isWebTransaction() {
        return true;
    }

    public boolean isAsyncTransaction() {
        return false;
    }

    public String getCookieValue(String name) {
        if (request == null) {
            return null;
        }
        return request.getCookieValue(name);
    }

    public String getHeader(String name) {
        if (request == null) {
            return null;
        }
        return request.getHeader(name);
    }

    public int getStatus() {
        return statusCode;
    }

    public void setStatus(int statusCode) {
        Agent.LOG.log(Level.FINEST, "Called setStatus: {0}", new Object[] {Integer.valueOf(statusCode)});
        if ((statusCode <= 0) || (statusCode == this.statusCode)) {
            return;
        }
        int nextStatusCode = statusCodePolicy.nextStatus(this.statusCode, statusCode);
        if (nextStatusCode != this.statusCode) {
            Agent.LOG.log(Level.FINER, "Setting status to {0}", new Object[] {Integer.valueOf(nextStatusCode)});
        }
        this.statusCode = nextStatusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String message) {
        statusMessage = message;
    }
}