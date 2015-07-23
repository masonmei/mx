package com.newrelic.agent.bridge.external;

import java.text.MessageFormat;

import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.Transaction;

public class ExternalMetrics {
    public static final String METRIC_NAMESPACE = "External";
    public static final String METRIC_NAME = "External/{0}/{1}";
    public static final String TRANSACTION_SEGMENT_NAME = "External/{0}/{1}/{2}";
    public static final String ALL = "External/all";
    public static final String ALL_WEB = "External/allWeb";
    public static final String ALL_OTHER = "External/allOther";
    public static final String ALL_HOST = "External/{0}/all";

    private static String fixOperations(String[] operations) {
        StringBuilder builder = new StringBuilder();
        for (String operation : operations) {
            if (operation.startsWith("/")) {
                builder.append(operation);
            } else {
                builder.append('/').append(operation);
            }
        }
        return builder.substring(1);
    }

    public static void makeExternalComponentTrace(Transaction tx, TracedMethod method, String host, String library,
                                                  boolean includeOperationInMetric, String uri, String[] operations) {
        String transactionSegmentName =
                MessageFormat.format("External/{0}/{1}/{2}", new Object[] {host, library, fixOperations(operations)});

        String metricName = includeOperationInMetric ? transactionSegmentName
                                    : MessageFormat.format("External/{0}/{1}", new Object[] {host, library});

        method.setMetricNameFormatInfo(metricName, transactionSegmentName, uri);

        method.addExclusiveRollupMetricName(new String[] {"External/all"});

        if (tx.isWebTransaction()) {
            method.addExclusiveRollupMetricName(new String[] {"External/allWeb"});
        } else {
            method.addExclusiveRollupMetricName(new String[] {"External/allOther"});
        }

        method.addExclusiveRollupMetricName(new String[] {MessageFormat
                                                                  .format("External/{0}/all", new Object[] {host})});
    }
}