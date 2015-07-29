package com.newrelic.agent.database;

import com.newrelic.agent.instrumentation.pointcuts.database.DatabaseUtils;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import java.text.MessageFormat;

public final class ParsedDatabaseStatement
  implements MetricNameFormat
{
  private final String operation;
  private final String model;
  private final boolean generateMetric;
  private final String metricName;
  private final String operationRollupMetricName;
  private final DatabaseVendor dbVendor;

  public ParsedDatabaseStatement(String model, String operation, boolean generateMetric)
  {
    this(DatabaseVendor.UNKNOWN, model, operation, generateMetric);
  }

  public ParsedDatabaseStatement(DatabaseVendor dbVendor, String model, String operation, boolean generateMetric) {
    this.model = model;
    this.operation = operation;
    this.generateMetric = generateMetric;
    this.dbVendor = dbVendor;

    this.operationRollupMetricName = MessageFormat.format("Datastore/operation/{0}/{1}", new Object[] { DatabaseUtils.getDatastoreVendor(dbVendor), operation });

    if ((null == model) || ("".equals(model)))
      this.metricName = this.operationRollupMetricName;
    else
      this.metricName = MessageFormat.format("Datastore/statement/{0}/{1}/{2}", new Object[] { DatabaseUtils.getDatastoreVendor(dbVendor), model, operation });
  }

  public String getOperation()
  {
    return this.operation;
  }

  public String getModel() {
    return this.model;
  }

  public DatabaseVendor getDbVendor() {
    return this.dbVendor;
  }

  public boolean recordMetric()
  {
    return this.generateMetric;
  }

  public String getMetricName()
  {
    return this.metricName;
  }

  public String toString()
  {
    return this.operation + ' ' + this.model;
  }

  public String getTransactionSegmentName()
  {
    return getMetricName();
  }

  public String getOperationRollupMetricName() {
    return this.operationRollupMetricName;
  }

  public String getTransactionSegmentUri()
  {
    return null;
  }
}