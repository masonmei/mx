package com.newrelic.agent.instrumentation.pointcuts.database;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.database.RecordSql;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.deps.org.json.simple.JSONAware;

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
            Object[] parameters = this.preparedStatement._nr_getSqlParameters();

            this.sqlParameters = (parameters == null ? null : new Object[parameters.length]);
            if (parameters != null) {
                System.arraycopy(parameters, 0, this.sqlParameters, 0, parameters.length);
            }
        }
        super.doFinish(opcode, returnValue);
    }

    Object[] getSqlParameters() {
        return this.sqlParameters;
    }

    protected Object getSqlObject() {
        String sql = this.preparedStatement._nr_getStatementData().getSql();

        if (RecordSql.raw.equals(getRecordSql())) {
            if ((getSqlParameters() != null) && (getSqlParameters().length > 0)) {
                return new PreparedStatementSql(sql, getSqlParameters());
            }
            return sql;
        }

        return sql;
    }

    protected ExplainPlanExecutor createExplainPlanExecutor(String sql) {
        return new PreparedStatementExplainPlanExecutor(this, sql, this.sqlParameters, getRecordSql());
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
                return AbstractPreparedStatementPointCut.parameterizeSql(this.sql, this.sqlParameters);
            } catch (Exception e) {
            }
            return this.sql;
        }

        public String toString() {
            return toJSONString();
        }
    }
}