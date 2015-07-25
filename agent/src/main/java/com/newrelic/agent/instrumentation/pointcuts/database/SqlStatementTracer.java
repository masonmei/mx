//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.pointcuts.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.BoundTransactionApiImpl;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.datastore.DatastoreMetrics;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.DatabaseStatementParser;
import com.newrelic.agent.database.DatabaseVendor;
import com.newrelic.agent.database.ParsedDatabaseStatement;
import com.newrelic.agent.database.RecordSql;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DatabaseTracer;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.ISqlStatementTracer;
import com.newrelic.agent.util.Strings;

public class SqlStatementTracer extends DefaultTracer implements DatabaseTracer, ISqlStatementTracer,
                                                                         Comparable<SqlStatementTracer> {
    public static final String EXPLAIN_PLAN_PARAMETER_NAME = "explanation";
    public static final String EXPLAIN_PLAN_FORMAT_PARAMETER_NAME = "explanation_format";
    public static final String DATABASE_VENDOR_PARAMETER_NAME = "database_vendor";
    public static final String SQL_PARAMETER_NAME = "sql";
    public static final String SQL_OBFUSCATED_PARAMETER_NAME = "sql_obfuscated";
    private final StatementData statementData;
    private ParsedDatabaseStatement parsedStatement;
    private ExplainPlanExecutor explainPlanExecutor;
    private ConnectionFactory connectionFactory;
    private Object sqlObject;
    private DatabaseVendor databaseVendor;

    public SqlStatementTracer(Transaction transaction, ClassMethodSignature sig, Object statementObject,
                              StatementData statementData) {
        super(transaction, sig, statementObject, NULL_METRIC_NAME_FORMATTER, 38);
        this.statementData = statementData;
        if (Agent.isDebugEnabled() && statementData == null) {
            Agent.LOG.finer("No sql for sql statement " + statementObject);
        }

    }

    protected void doFinish(Throwable throwable) {
        this.setDatabaseVendor();
        Object sql = this.getSqlObject();
        if (sql != null) {
            this.getTransaction().getIntrinsicAttributes().put("sql", sql);
        }

    }

    protected Object getSqlObject() {
        return this.sqlObject != null ? this.sqlObject
                       : (this.statementData == null ? null : this.statementData.getSql());
    }

    public Object getSql() {
        return this.getSqlObject();
    }

    protected void doFinish(int opcode, Object returnValue) {
        super.doFinish(opcode, returnValue);
        this.getTransaction().getSqlTracerListener().noticeSqlTracer(this);
        if (this.statementData != null) {
            if (this.isTransactionSegment() && this.captureSql()) {
                this.sqlObject = this.getSqlObject();
            }

            this.parsedStatement = this.statementData.getParsedStatement(returnValue,
                                                                                this.getTransaction().getRPMService()
                                                                                        .getConnectionTimestamp());
            this.setDatabaseVendor();
            this.parsedStatement = new ParsedDatabaseStatement(this.databaseVendor, this.parsedStatement.getModel(),
                                                                      this.parsedStatement.getOperation(),
                                                                      this.parsedStatement.recordMetric());
        }

        if (this.isTransactionSegment() && this.statementData != null && this.statementData.getSql() != null) {
            TransactionTracerConfig transactionTracerConfig = this.getTransaction().getTransactionTracerConfig();
            double explainThresholdInNanos = transactionTracerConfig.getExplainThresholdInNanos();
            if (transactionTracerConfig.isExplainEnabled()) {
                this.captureExplain(this.parsedStatement, explainThresholdInNanos, transactionTracerConfig);
            } else if (Agent.isDebugEnabled()) {
                String msg = MessageFormat.format("Statement exceeded threshold?: {0}",
                                                         (double) this.getDuration() > explainThresholdInNanos);
                Agent.LOG.finer(msg);
            }
        }

    }

    protected boolean shouldStoreStackTrace() {
        return super.shouldStoreStackTrace() && this.statementData != null && this.statementData.getSql() != null;
    }

    private void setDatabaseVendor() {
        try {
            Connection connection = this.statementData.getStatement().getConnection();
            this.databaseVendor = DatabaseUtils.getDatabaseVendor(connection);
            return;
        } catch (Throwable var6) {
            Agent.LOG.log(Level.FINEST, "Error getting database information", var6);
        } finally {
            if (this.databaseVendor == null) {
                this.databaseVendor = DatabaseVendor.UNKNOWN;
            }

        }

    }

    private void captureExplain(ParsedDatabaseStatement parsedStatement, double explainThresholdInNanos,
                                TransactionTracerConfig transactionTracerConfig) {
        if ((double) this.getDuration() > explainThresholdInNanos && "select".equals(parsedStatement.getOperation())) {
            if (this.getTransaction().getTransactionCounts().getExplainPlanCount() < transactionTracerConfig
                                                                                             .getMaxExplainPlans()) {
                String sql = this.statementData.getSql();
                if (Strings.isEmpty(sql)) {
                    this.setExplainPlan("Unable to run the explain plan because we have no sql");
                } else {
                    try {
                        String msg;
                        try {
                            Connection e = this.statementData.getStatement().getConnection();
                            if (e == null) {
                                this.setExplainPlan("Unable to run the explain plan because the "
                                                            + "statement returned a null connection");
                                if (Agent.LOG.isLoggable(Level.FINER)) {
                                    Agent.LOG.log(Level.FINER, "Unable to run an explain plan because the Statement"
                                                                       + ".getConnection() returned null : "
                                                                       + this.statementData.getStatement().getClass()
                                                                                 .getName());
                                }

                                return;
                            }

                            if (this.databaseVendor.isExplainPlanSupported()) {
                                this.connectionFactory =
                                        SqlDriverPointCut.getConnectionFactory(this.getTransaction(), e);
                                if (this.connectionFactory != null) {
                                    this.explainPlanExecutor = this.createExplainPlanExecutor(sql);
                                    if (this.explainPlanExecutor != null) {
                                        if (Agent.LOG.isLoggable(Level.FINEST)) {
                                            Agent.LOG.finest("Capturing information for explain plan");
                                        }

                                        this.getTransaction().getTransactionCounts()
                                                .incrementExplainPlanCountAndLogIfReachedMax(transactionTracerConfig
                                                                                                     .getMaxExplainPlans());
                                        return;
                                    }
                                } else {
                                    this.setExplainPlan("Unable to create a connection to run the explain plan");
                                    if (Agent.isDebugEnabled()) {
                                        msg = MessageFormat.format("Unable to run explain plan because no connection "
                                                                           + "factory ({0}) was found for connection "
                                                                           + "{1}, " + "statement {2}",
                                                                          this.getTransaction().getConnectionCache()
                                                                                  .getConnectionFactoryCacheSize(),
                                                                          e.getClass().getName(),
                                                                          this.statementData.getStatement().getClass()
                                                                                  .getName());
                                        Agent.LOG.finer(msg);
                                        return;
                                    }
                                }

                                return;
                            }

                            this.setExplainPlan("Unable to run explain plans for " + this.databaseVendor.getName()
                                                        + " databases");
                        } catch (SQLException var11) {
                            msg = MessageFormat.format("An error occurred running the explain plan: {0}", var11);
                            this.setExplainPlan(msg);
                            Agent.LOG.finer(msg);
                            return;
                        }
                    } finally {
                        if (this.explainPlanExecutor == null) {
                            this.connectionFactory = null;
                        }

                    }

                }
            }
        }
    }

    protected RecordSql getRecordSql() {
        return RecordSql.get(this.getTransaction().getTransactionTracerConfig().getRecordSql());
    }

    protected ExplainPlanExecutor createExplainPlanExecutor(String sql) {
        return new DefaultExplainPlanExecutor(this, sql, this.getRecordSql());
    }

    private boolean captureSql() {
        return "off" != this.getTransaction().getTransactionTracerConfig().getRecordSql();
    }

    public boolean hasExplainPlan() {
        return this.getAttribute("explanation") != null;
    }

    public void setExplainPlan(Object... explainPlan) {
        this.setAttribute("explanation", Arrays.asList(explainPlan));
        if (this.databaseVendor != DatabaseVendor.UNKNOWN) {
            this.setAttribute("database_vendor", this.databaseVendor.getType());
            this.setAttribute("explanation_format", this.databaseVendor.getExplainPlanFormat());
        }

    }

    public boolean isMetricProducer() {
        return this.parsedStatement != null && this.parsedStatement.recordMetric();
    }

    protected void recordMetrics(TransactionStats transactionStats) {
        if (this.isMetricProducer()) {
            DatastoreMetrics dsMetrics =
                    DatastoreMetrics.getInstance(DatabaseUtils.getDatastoreVendor(this.databaseVendor));
            dsMetrics.collectDatastoreMetrics(new BoundTransactionApiImpl(this.getTransaction()), this,
                                                     this.parsedStatement.getModel(),
                                                     this.parsedStatement.getOperation(), null, null);
            if (this.parsedStatement == DatabaseStatementParser.UNPARSEABLE_STATEMENT) {
                dsMetrics.unparsedQuerySupportability();
            }
        }

        super.recordMetrics(transactionStats);
    }

    public ExplainPlanExecutor getExplainPlanExecutor() {
        return this.explainPlanExecutor;
    }

    public ConnectionFactory getConnectionFactory() {
        return this.connectionFactory;
    }

    public int compareTo(SqlStatementTracer otherTracer) {
        long durationDifference = this.getDuration() - otherTracer.getDuration();
        return durationDifference < 0L ? -1 : (durationDifference > 0L ? 1 : 0);
    }
}
