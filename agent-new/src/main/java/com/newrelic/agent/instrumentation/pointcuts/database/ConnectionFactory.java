package com.newrelic.agent.instrumentation.pointcuts.database;

import com.newrelic.agent.database.DatabaseVendor;
import java.sql.Connection;
import java.sql.SQLException;

public abstract interface ConnectionFactory
{
  public abstract Connection getConnection()
    throws SQLException;

  public abstract String getUrl();

  public abstract DatabaseVendor getDatabaseVendor();
}