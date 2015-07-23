package com.newrelic.agent.database;

import java.text.MessageFormat;

import com.newrelic.agent.instrumentation.pointcuts.database.DatabaseUtils;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;

public final class ParsedDatabaseStatement implements MetricNameFormat {
    private final String operation;
    private final String model;
    private final boolean generateMetric;
    private final String metricName;
    private final String operationRollupMetricName;
    private final DatabaseVendor dbVendor;

    public ParsedDatabaseStatement(String model, String operation, boolean generateMetric) {
        this(DatabaseVendor.UNKNOWN, model, operation, generateMetric);
    }

    public ParsedDatabaseStatement(DatabaseVendor dbVendor, String model, String operation, boolean generateMetric) {
        this.model = model;
        this.operation = operation;
        this.generateMetric = generateMetric;
        this.dbVendor = dbVendor;

        operationRollupMetricName = MessageFormat.format("Datastore/operation/{0}/{1}", new Object[] {DatabaseUtils
                                                                                                              .getDatastoreVendor(dbVendor),
                                                                                                             operation});

        if ((null == model) || ("".equals(model))) {
            metricName = operationRollupMetricName;
        } else {
            metricName = MessageFormat.format("Datastore/statement/{0}/{1}/{2}",
                                                     new Object[] {DatabaseUtils.getDatastoreVendor(dbVendor), model,
                                                                          operation});
        }
    }

    public String getOperation() {
        return operation;
    }

    public String getModel() {
        return model;
    }

    public DatabaseVendor getDbVendor() {
        return dbVendor;
    }

    public boolean recordMetric() {
        return generateMetric;
    }

    public String getMetricName() {
        return metricName;
    }

    public String toString() {
        return operation + ' ' + model;
    }

    public String getTransactionSegmentName() {
        return getMetricName();
    }

    public String getOperationRollupMetricName() {
        return operationRollupMetricName;
    }

    public String getTransactionSegmentUri() {
        return null;
    }
}