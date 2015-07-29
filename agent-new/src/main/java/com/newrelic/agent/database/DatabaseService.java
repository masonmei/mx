package com.newrelic.agent.database;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.instrumentation.pointcuts.database.ConnectionFactory;
import com.newrelic.agent.instrumentation.pointcuts.database.DatabaseUtils;
import com.newrelic.agent.instrumentation.pointcuts.database.ExplainPlanExecutor;
import com.newrelic.agent.instrumentation.pointcuts.database.SqlStatementTracer;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import java.sql.Connection;
import java.text.MessageFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public class DatabaseService extends AbstractService
  implements AgentConfigListener
{
  private static final SqlObfuscator DEFAULT_SQL_OBFUSCATOR = SqlObfuscator.getDefaultSqlObfuscator();

  private final ConcurrentMap<String, SqlObfuscator> sqlObfuscators = new ConcurrentHashMap();
  private final AtomicReference<SqlObfuscator> defaultSqlObfuscator = new AtomicReference();
  private final String defaultAppName;
  private final DatabaseStatementParser databaseStatementParser;

  public DatabaseService()
  {
    super(DatabaseService.class.getSimpleName());
    AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
    this.defaultAppName = config.getApplicationName();
    this.databaseStatementParser = new DefaultDatabaseStatementParser(config);
  }

  protected void doStart()
  {
    ServiceFactory.getConfigService().addIAgentConfigListener(this);
  }

  protected void doStop()
  {
    ServiceFactory.getConfigService().removeIAgentConfigListener(this);
  }

  public boolean isEnabled()
  {
    return true;
  }

  public SqlObfuscator getDefaultSqlObfuscator()
  {
    return DEFAULT_SQL_OBFUSCATOR;
  }

  public SqlObfuscator getSqlObfuscator(String appName) {
    SqlObfuscator sqlObfuscator = findSqlObfuscator(appName);
    if (sqlObfuscator != null) {
      return sqlObfuscator;
    }
    return createSqlObfuscator(appName);
  }

  private SqlObfuscator findSqlObfuscator(String appName) {
    if ((appName == null) || (appName.equals(this.defaultAppName))) {
      return (SqlObfuscator)this.defaultSqlObfuscator.get();
    }
    return (SqlObfuscator)this.sqlObfuscators.get(appName);
  }

  private SqlObfuscator createSqlObfuscator(String appName) {
    TransactionTracerConfig ttConfig = ServiceFactory.getConfigService().getTransactionTracerConfig(appName);
    SqlObfuscator sqlObfuscator = createSqlObfuscator(ttConfig);
    if ((appName == null) || (appName.equals(this.defaultAppName))) {
      if (this.defaultSqlObfuscator.getAndSet(sqlObfuscator) == null) {
        logConfig(appName, ttConfig);
      }
    }
    else if (this.sqlObfuscators.put(appName, sqlObfuscator) == null) {
      logConfig(appName, ttConfig);
    }

    return sqlObfuscator;
  }

  private SqlObfuscator createSqlObfuscator(TransactionTracerConfig ttConfig) {
    if (!ttConfig.isEnabled()) {
      return SqlObfuscator.getNoSqlObfuscator();
    }
    String recordSql = ttConfig.getRecordSql();
    if ("off".equals(recordSql)) {
      return SqlObfuscator.getNoSqlObfuscator();
    }
    if ("raw".equals(recordSql)) {
      return SqlObfuscator.getNoObfuscationSqlObfuscator();
    }
    return SqlObfuscator.getDefaultSqlObfuscator();
  }

  private void logConfig(String appName, TransactionTracerConfig ttConfig) {
    if (ttConfig.isLogSql()) {
      String msg = MessageFormat.format("Agent is configured to log {0} SQL for {1}", new Object[] { ttConfig.getRecordSql(), appName });

      Agent.LOG.fine(msg);
    } else {
      String msg = MessageFormat.format("Agent is configured to send {0} SQL to New Relic for {1}", new Object[] { ttConfig.getRecordSql(), appName });

      Agent.LOG.fine(msg);
    }
    if (!isValidRecordSql(ttConfig.getRecordSql())) {
      String msg = MessageFormat.format("Unknown value \"{0}\" for record_sql", new Object[] { ttConfig.getRecordSql(), appName });
      Agent.LOG.warning(msg);
    }
  }

  private boolean isValidRecordSql(String recordSql) {
    return ("raw".equals(recordSql)) || ("off".equals(recordSql)) || ("obfuscated".equals(recordSql));
  }

  public void configChanged(String appName, AgentConfig agentConfig)
  {
    Agent.LOG.fine(MessageFormat.format("Database service received configuration change notification for {0}", new Object[] { appName }));

    if ((appName == null) || (appName.equals(this.defaultAppName)))
      this.defaultSqlObfuscator.set(null);
    else
      this.sqlObfuscators.remove(appName);
  }

  public void runExplainPlan(SqlStatementTracer sqlTracer)
  {
    ExplainPlanExecutor explainExecutor = sqlTracer.getExplainPlanExecutor();
    ConnectionFactory connectionFactory = sqlTracer.getConnectionFactory();
    if ((sqlTracer.hasExplainPlan()) || (explainExecutor == null) || (connectionFactory == null)) {
      return;
    }
    runExplainPlan(explainExecutor, connectionFactory);
  }

  private void runExplainPlan(ExplainPlanExecutor explainExecutor, ConnectionFactory connectionFactory) {
    Connection connection = null;
    try {
      connection = connectionFactory.getConnection();
      DatabaseVendor vendor = DatabaseUtils.getDatabaseVendor(connection);
      explainExecutor.runExplainPlan(this, connection, vendor);
    } catch (Throwable t) {
      String msg = MessageFormat.format("An error occurred executing an explain plan: {0}", new Object[] { t });
      if (Agent.LOG.isLoggable(Level.FINER))
        Agent.LOG.log(Level.FINER, msg, t);
      else
        Agent.LOG.fine(msg);
    }
    finally {
      if (connection != null)
        try {
          connection.close();
        } catch (Exception e) {
          Agent.LOG.log(Level.FINER, "Unable to close connection", e);
        }
    }
  }

  public DatabaseStatementParser getDatabaseStatementParser()
  {
    return this.databaseStatementParser;
  }
}