package com.newrelic.agent.instrumentation.pointcuts.database;

import com.newrelic.deps.org.json.simple.JSONAware;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.database.RecordSql;
import com.newrelic.agent.tracers.ClassMethodSignature;

class PreparedStatementTracer extends SqlStatementTracer {
    private final PreparedStatementExtension preparedStatement;
    private Object[] sqlParameters;

    public PreparedStatementTracer(Transaction transaction, ClassMethodSignature sig,
                                   PreparedStatementExtension preparedStatement, StatementData statementData) {
        super(transaction, sig, preparedStatement, statementData);
        this.preparedStatement = preparedStatement;
    }

    protected void doFinish(int opcode, Object returnValue) {
        if (("raw" == getTransaction().getTransactionTracerConfig().getRecordSql()) || (getDuration() > getTransaction()
                                                                                                                .getTransactionTracerConfig()
                                                                                                                .getExplainThresholdInNanos())) {
            Object[] parameters = preparedStatement._nr_getSqlParameters();

            sqlParameters = (parameters == null ? null : new Object[parameters.length]);
            if (parameters != null) {
                System.arraycopy(parameters, 0, sqlParameters, 0, parameters.length);
            }
        }
        super.doFinish(opcode, returnValue);
    }

    Object[] getSqlParameters() {
        return sqlParameters;
    }

    protected Object getSqlObject() {
        String sql = preparedStatement._nr_getStatementData().getSql();

        if (RecordSql.raw.equals(getRecordSql())) {
            if ((getSqlParameters() != null) && (getSqlParameters().length > 0)) {
                return new PreparedStatementSql(sql, getSqlParameters());
            }
            return sql;
        }

        return sql;
    }

    protected ExplainPlanExecutor createExplainPlanExecutor(String sql) {
        return new PreparedStatementExplainPlanExecutor(this, sql, sqlParameters, getRecordSql());
    }

    private static class PreparedStatementSql implements JSONAware {
        private final String sql;
        private final Object[] sqlParameters;

        public PreparedStatementSql(String sql, Object[] sqlParameters) {
            this.sql = sql;
            this.sqlParameters = sqlParameters;
        }

        public String toJSONString() {
            try {
                return AbstractPreparedStatementPointCut.parameterizeSql(sql, sqlParameters);
            } catch (Exception e) {
            }
            return sql;
        }

        public String toString() {
            return toJSONString();
        }
    }
}