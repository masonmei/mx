package com.newrelic.agent.sql;

import com.newrelic.agent.TransactionData;
import com.newrelic.agent.instrumentation.pointcuts.database.SqlStatementTracer;

class SqlTracerInfo {
    private final SqlStatementTracer sqlTracer;
    private TransactionData transactionData;

    SqlTracerInfo(TransactionData transactionData, SqlStatementTracer sqlTracer) {
        this.transactionData = transactionData;
        this.sqlTracer = sqlTracer;
    }

    public TransactionData getTransactionData() {
        return transactionData;
    }

    public void setTransactionData(TransactionData td) {
        transactionData = td;
    }

    public SqlStatementTracer getSqlTracer() {
        return sqlTracer;
    }
}