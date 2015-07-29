//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.pointcuts.database;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.database.DatabaseVendor;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DatabaseTracer;
import com.newrelic.agent.tracers.MethodExitTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.deps.com.google.common.cache.Cache;

@PointCut
public class SqlDriverPointCut extends TracerFactoryPointCut {
    public SqlDriverPointCut(ClassTransformer classTransformer) {
        super(new PointCutConfiguration("jdbc_driver"), new InterfaceMatcher("java/sql/Driver"),
                     createExactMethodMatcher("connect", new String[] {"(Ljava/lang/String;Ljava/util/Properties;)"
                                                                               + "Ljava/sql/Connection;"}));
    }

    public static final void putConnectionFactory(Transaction tx, Connection connection, ConnectionFactory factory) {
        Connection innerConnection = DatabaseUtils.getInnerConnection(connection);
        if (!(innerConnection instanceof ConnectionExtension)
                    || ((ConnectionExtension) innerConnection)._nr_getConnectionFactory() == null) {
            if (Agent.isDebugEnabled()) {
                Agent.LOG.finer("Tracking connection: " + connection.getClass().getName());
            }

            if (connection instanceof ConnectionExtension) {
                ((ConnectionExtension) connection)._nr_setConnectionFactory(factory);
            } else {
                if (factory.getDatabaseVendor().isExplainPlanSupported() && tx.getTransactionTracerConfig().isEnabled()
                            && tx.getTransactionTracerConfig().isExplainEnabled()) {
                    tx.getConnectionCache().putConnectionFactory(connection, factory);
                }

            }
        }
    }

    public static ConnectionFactory getConnectionFactory(Transaction transaction, Connection connection) {
        connection = DatabaseUtils.getInnerConnection(connection);
        if (connection instanceof ConnectionExtension) {
            ConnectionFactory connections = ((ConnectionExtension) connection)._nr_getConnectionFactory();
            if (connections != null) {
                return connections;
            }
        }

        Cache connections1 = transaction.getConnectionCache().getConnectionFactoryCache();
        if (connections1 == null) {
            return null;
        } else {
            ConnectionFactory connectionFactory = (ConnectionFactory) connections1.getIfPresent(connection);
            if (connectionFactory == null) {
                if (connections1.size() == 1L) {
                    return (ConnectionFactory) connections1.asMap().values().iterator().next();
                }

                if (connections1.size() < 100L) {
                    Iterator i$ = connections1.asMap().entrySet().iterator();

                    Entry entry;
                    do {
                        if (!i$.hasNext()) {
                            return connectionFactory;
                        }

                        entry = (Entry) i$.next();
                    } while (!connection.equals(entry.getKey()) && !((Connection) entry.getKey()).equals(connection));

                    connections1.put(connection, entry.getValue());
                    return (ConnectionFactory) entry.getValue();
                }
            }

            return connectionFactory;
        }
    }

    protected boolean isDispatcher() {
        return true;
    }

    public Tracer doGetTracer(Transaction tx, ClassMethodSignature sig, Object driver, Object[] args) {
        return tx.getTransactionCounts().isOverTracerSegmentLimit() ? null
                       : new SqlDriverPointCut.ConnectionTracer(tx, sig, driver, args);
    }

    private static class DriverConnectionFactory implements ConnectionFactory {
        private static final Properties EMPTY_PROPERTIES = new Properties();
        private final Driver driver;
        private final String url;
        private final Properties props;
        private DatabaseVendor databaseVendor;

        public DriverConnectionFactory(Driver driver, String url, Properties props) {
            this.databaseVendor = DatabaseVendor.UNKNOWN;
            this.driver = driver;
            this.url = url;
            this.props = props != null && !props.isEmpty() ? props : EMPTY_PROPERTIES;
        }

        public Connection getConnection() throws SQLException {
            try {
                return this.driver.connect(this.url, this.props);
            } catch (SQLException var2) {
                this.logError();
                throw var2;
            } catch (Exception var3) {
                this.logError();
                throw new SQLException(var3);
            }
        }

        private void logError() {
            if (Agent.LOG.isLoggable(Level.FINER)) {
                Agent.LOG.finer(MessageFormat
                                        .format("An error occurred getting a database connection. Driver:{0} url:{1}",
                                                       new Object[] {this.driver, this.url}));
            }

        }

        public String getUrl() {
            return this.url;
        }

        public DatabaseVendor getDatabaseVendor() {
            return this.databaseVendor;
        }

        void setDatabaseVendor(DatabaseVendor databaseVendor) {
            this.databaseVendor = databaseVendor;
        }
    }

    private static class ConnectionTracer extends SqlDriverPointCut.ConnectionErrorTracer {
        private final SqlDriverPointCut.DriverConnectionFactory connectionFactory;

        public ConnectionTracer(Transaction transaction, ClassMethodSignature sig, Object driver, Object[] args) {
            super(sig, transaction);
            this.connectionFactory = new SqlDriverPointCut.DriverConnectionFactory((Driver) driver, (String) args[0],
                                                                                          (Properties) args[1]);
        }

        protected void doFinish(int opcode, Object connection) {
            super.doFinish(opcode, connection);
            if (connection != null) {
                this.connectionFactory.setDatabaseVendor(DatabaseUtils.getDatabaseVendor((Connection) connection));
                SqlDriverPointCut
                        .putConnectionFactory(this.getTransaction(), (Connection) connection, this.connectionFactory);
            }

        }
    }

    private static class ConnectionErrorTracer extends MethodExitTracer implements DatabaseTracer {
        public ConnectionErrorTracer(ClassMethodSignature signature, Transaction transaction) {
            super(signature, transaction);
        }

        public void finish(Throwable throwable) {
            super.finish(throwable);
            this.getTransactionActivity().getTransactionStats().getUnscopedStats().getStats("DatastoreErrors/all")
                    .incrementCallCount();
        }

        protected void doFinish(int opcode, Object returnValue) {
        }
    }
}
