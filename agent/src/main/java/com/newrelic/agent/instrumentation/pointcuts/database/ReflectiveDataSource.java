//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.pointcuts.database;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.newrelic.agent.Agent;

class ReflectiveDataSource implements DataSource {
    private static final String DATASOURCE_CLASS_NAME = DataSource.class.getName();
    private final Object dataSource;
    private final Class<?> dataSourceClass;

    public ReflectiveDataSource(Object dataSource) throws ClassNotFoundException {
        this.dataSource = dataSource;
        dataSource.getClass();
        this.dataSourceClass = Class.forName(DATASOURCE_CLASS_NAME);
    }

    public Connection getConnection() throws SQLException {
        return (Connection) this.invoke("getConnection", new Object[0]);
    }

    private <T> T invoke(String methodName, Object... args) {
        Class[] argTypes = new Class[args.length];

        for (int e = 0; e < args.length; ++e) {
            argTypes[e] = args[e].getClass();
        }

        try {
            return (T) this.dataSourceClass.getMethod(methodName, argTypes).invoke(this.dataSource, args);
        } catch (Throwable var5) {
            if (Agent.LOG.isLoggable(Level.FINER)) {
                Agent.LOG.log(Level.FINER, "Unable to invoke DataSource method " + methodName, var5);
            }

            return null;
        }
    }

    public Connection getConnection(String username, String password) throws SQLException {
        return (Connection) this.invoke("getConnection", new Object[] {username, password});
    }

    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    public void setLogWriter(PrintWriter arg0) throws SQLException {
    }

    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    public void setLoginTimeout(int arg0) throws SQLException {
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    public Logger getParentLogger() {
        return null;
    }
}
