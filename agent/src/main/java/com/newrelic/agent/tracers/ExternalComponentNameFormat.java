package com.newrelic.agent.tracers;

import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.util.Strings;

public class ExternalComponentNameFormat implements MetricNameFormat {
    private final String[] operations;
    private final boolean includeOperationInMetric;
    private final String host;
    private final String library;
    private final String transactionSegmentUri;
    private String metricName;
    private String transactionSegmentName;

    public ExternalComponentNameFormat(String host, String library, boolean includeOperationInMetric,
                                       String pTransactionSegmentUri, String[] operations) {
        this.host = host;
        this.library = library;
        this.operations = operations;
        this.includeOperationInMetric = includeOperationInMetric;
        transactionSegmentUri = pTransactionSegmentUri;

        setMetricName();
    }

    public static MetricNameFormat create(String host, String library, boolean includeOperationInMetric, String uri,
                                          String[] operations) {
        return new ExternalComponentNameFormat(host, library, includeOperationInMetric, uri, operations);
    }

    public ExternalComponentNameFormat cloneWithNewHost(String hostName) {
        return new ExternalComponentNameFormat(hostName, library, includeOperationInMetric, transactionSegmentUri,
                                                      operations);
    }

    private void setMetricName() {
        metricName = Strings.join('/', new String[] {"External", host, library});
        if (includeOperationInMetric) {
            metricName += fixOperations(operations);
            transactionSegmentName = metricName;
        }
    }

    public String getMetricName() {
        return metricName;
    }

    public String getTransactionSegmentName() {
        if (transactionSegmentName == null) {
            transactionSegmentName = (metricName + fixOperations(operations));
        }
        return transactionSegmentName;
    }

    private String fixOperations(String[] operations) {
        StringBuilder builder = new StringBuilder();
        for (String operation : operations) {
            if (operation.startsWith("/")) {
                builder.append(operation);
            } else {
                builder.append('/').append(operation);
            }
        }
        return builder.toString();
    }

    public String getTransactionSegmentUri() {
        return transactionSegmentUri;
    }
}