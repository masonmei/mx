package com.newrelic.agent.instrumentation.pointcuts.database;

import com.newrelic.agent.database.DatabaseService;
import com.newrelic.agent.database.DatabaseVendor;
import java.sql.Connection;
import java.sql.SQLException;

public abstract interface ExplainPlanExecutor
{
  public abstract void runExplainPlan(DatabaseService paramDatabaseService, Connection paramConnection,
                                      DatabaseVendor paramDatabaseVendor)
    throws SQLException;
}