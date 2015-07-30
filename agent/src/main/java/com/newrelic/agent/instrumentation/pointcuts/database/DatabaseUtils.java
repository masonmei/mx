package com.newrelic.agent.instrumentation.pointcuts.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.database.DatabaseVendor;

public class DatabaseUtils {
    public static Connection getInnerConnection(Connection conn) {
        if ((conn instanceof DelegatingConnection)) {
            DelegatingConnection delegatingConnection = (DelegatingConnection) conn;
            Connection connection = delegatingConnection.getInnermostDelegate();
            if (connection != null) {
                return getInnerConnection(connection);
            }
            if ((conn instanceof DelegatingConnection.PoolGuardConnectionWrapper)) {
                connection = ((DelegatingConnection.PoolGuardConnectionWrapper) conn)._nr_getDelegate();
                if (connection != null) {
                    return getInnerConnection(connection);
                }
            }
        }
        return conn;
    }

    public static DatabaseVendor getDatabaseVendor(Connection connection) {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            if (metaData == null) {
                return DatabaseVendor.UNKNOWN;
            }
            String url = metaData.getURL();
            if (url == null) {
                return DatabaseVendor.UNKNOWN;
            }
            return DatabaseVendor.getDatabaseVendor(url);
        } catch (SQLException e) {
            Agent.LOG.log(Level.FINER, "Unable to determine database vendor", e);
        }
        return DatabaseVendor.UNKNOWN;
    }

    public static DatastoreVendor getDatastoreVendor(DatabaseVendor databaseVendor) {
        switch (databaseVendor) {
            case MYSQL:
                return DatastoreVendor.MySQL;
            case ORACLE:
                return DatastoreVendor.Oracle;
            case MICROSOFT:
                return DatastoreVendor.MSSQL;
            case POSTGRES:
                return DatastoreVendor.Postgres;
            case DB2:
                return DatastoreVendor.IBMDB2;
            case DERBY:
                return DatastoreVendor.Derby;
            case UNKNOWN:
            default:
                Agent.LOG.log(Level.FINEST, "ERROR: Unknown Database Vendor: {0}. Defaulting to JDBC.", databaseVendor);
        }
        return DatastoreVendor.JDBC;
    }
}