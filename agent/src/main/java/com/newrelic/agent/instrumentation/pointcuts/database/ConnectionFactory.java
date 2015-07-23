package com.newrelic.agent.instrumentation.pointcuts.database;

import java.sql.Connection;
import java.sql.SQLException;

import com.newrelic.agent.database.DatabaseVendor;

public abstract interface ConnectionFactory {
    public abstract Connection getConnection() throws SQLException;

    public abstract String getUrl();

    public abstract DatabaseVendor getDatabaseVendor();
}