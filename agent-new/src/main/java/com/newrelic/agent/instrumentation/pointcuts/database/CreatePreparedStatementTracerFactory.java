package com.newrelic.agent.instrumentation.pointcuts.database;

import java.sql.Statement;
import java.text.MessageFormat;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;
import com.newrelic.agent.tracers.AbstractTracerFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DatabaseTracer;
import com.newrelic.agent.tracers.MethodExitTracer;
import com.newrelic.agent.tracers.Tracer;

public class CreatePreparedStatementTracerFactory extends AbstractTracerFactory {
  public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object connection, Object[] args) {
    String sql = args.length > 0 ? (String) args[0] : null;
    if (sql != null) {
      return new CreatePreparedStatementTracer(transaction, sig, connection, sql);
    }
    if (Agent.isDebugEnabled()) {
      String msg = MessageFormat.format("Prepared statement sql was null: {0}", sig);
      Agent.LOG.finest(msg);
    }
    return null;
  }

  @InterfaceMixin(originalClassName = {"com/newrelic/deps/org/apache/commons/dbcp/DelegatingPreparedStatement"})
  public interface DelegatingPreparedStatement {
  }

  private class CreatePreparedStatementTracer extends MethodExitTracer implements DatabaseTracer {
    private final boolean initParameters;
    private String sql;

    public CreatePreparedStatementTracer(Transaction transaction, ClassMethodSignature sig, Object connection,
                                         String sql) {
      super(sig, transaction);
      this.sql = sql;
      initParameters = (("raw" == transaction.getTransactionTracerConfig().getRecordSql()) || (transaction
                                                                                                       .getTransactionTracerConfig()
                                                                                                       .isExplainEnabled()));
    }

    protected void doFinish(int opcode, Object statement) {
      boolean isLoggable = Agent.LOG.isLoggable(Level.FINEST);
      if ((statement instanceof DelegatingPreparedStatement)) {
        if (isLoggable) {
          String msg = MessageFormat.format("Skipping delegating prepared statement: {0}",
                                                   statement.getClass().getName());

          Agent.LOG.finest(msg);
        }
        return;
      }

      if (!(statement instanceof PreparedStatementExtension)) {
        if (isLoggable) {
          String msg = MessageFormat.format("{0} does not implement {1}", statement.getClass().getName(),
                                                   PreparedStatementExtension.class
                                                           .getName());

          Agent.LOG.finest(msg);
        }
        return;
      }
      PreparedStatementExtension prepStatment = (PreparedStatementExtension) statement;
      if (prepStatment._nr_getStatementData() != null) {
        return;
      }

      DefaultStatementData statementData =
              new DefaultStatementData(getTransaction().getDatabaseStatementParser(), (Statement) statement, sql);

      prepStatment._nr_setStatementData(statementData);

      if (isLoggable) {
        Agent.LOG.finest(MessageFormat.format("Storing SQL: {0} for PreparedStatement: {1}", sql,
                                                     statement.getClass().getName()));
      }

      if (initParameters) {
        Object[] statementParameters = new Object[16];
        prepStatment._nr_setSqlParameters(statementParameters);
      }
      sql = null;
    }
  }
}