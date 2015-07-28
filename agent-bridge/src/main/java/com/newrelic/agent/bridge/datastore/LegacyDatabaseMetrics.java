package com.newrelic.agent.bridge.datastore;

import java.text.MessageFormat;

import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.Transaction;

public class LegacyDatabaseMetrics {
    public static final String METRIC_NAMESPACE = "Database";
    public static final String ALL = "Database/all";
    public static final String ALL_WEB = "Database/allWeb";
    public static final String ALL_OTHER = "Database/allOther";
    public static final String STATEMENT = "Database/{0}/{1}";
    public static final String OPERATION = "Database/{0}";

    public static void doDatabaseMetrics(Transaction tx, TracedMethod method, String table, String operation) {
        method.setMetricName(MessageFormat.format(STATEMENT, table, operation));

        method.addRollupMetricName(MessageFormat.format(STATEMENT, table, operation));
        method.addRollupMetricName(MessageFormat.format(OPERATION, operation));

        method.addRollupMetricName(ALL);
        if (tx.isWebTransaction()) {
            method.addRollupMetricName(ALL_WEB);
        } else {
            method.addRollupMetricName(ALL_OTHER);
        }
    }
}