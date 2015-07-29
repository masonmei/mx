//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.pointcuts.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.sql.DataSource;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.DatabaseVendor;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DatabaseTracer;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.deps.org.objectweb.asm.Type;

@PointCut
public class DataSourcePointCut extends TracerFactoryPointCut {
    public DataSourcePointCut(ClassTransformer classTransformer) {
        super(new PointCutConfiguration("jdbc_data_source"),
                     new InterfaceMatcher(Type.getInternalName(DataSource.class)),
                     createMethodMatcher(new MethodMatcher[] {new ExactMethodMatcher("getConnection", "()"
                                                                                                              + Type.getDescriptor(Connection.class)),
                                                                     new ExactMethodMatcher("getConnection",
                                                                                                   "(Ljava/lang/String;Ljava/lang/String;)"
                                                                                                           + Type.getDescriptor(Connection.class))}));
    }

    protected boolean isDispatcher() {
        return true;
    }

    private boolean explainsEnabled(Transaction tx) {
        TransactionTracerConfig ttConfig = tx.getTransactionTracerConfig();
        return ttConfig.isEnabled() && ttConfig.isExplainEnabled();
    }

    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object dataSource, Object[] args) {
        Tracer parentTracer = transaction.getTransactionActivity().getLastTracer();
        ClassMethodMetricNameFormat metricName1;
        if (this.explainsEnabled(transaction) && !transaction.getTransactionCounts().isOverTracerSegmentLimit()) {
            if (parentTracer instanceof DataSourcePointCut.DataSourceTracer) {
                return null;
            } else {
                metricName1 = new ClassMethodMetricNameFormat(sig, dataSource);
                Object ds;
                if (dataSource instanceof DataSource) {
                    ds = (DataSource) dataSource;
                } else {
                    try {
                        ds = new ReflectiveDataSource(dataSource);
                    } catch (ClassNotFoundException var9) {
                        if (Agent.LOG.isLoggable(Level.SEVERE)) {
                            Agent.LOG.severe(dataSource.getClass().getName()
                                                     + " does not appear to be an instance of DataSource");
                        }

                        return new DataSourcePointCut.ConnectionTracer(transaction, sig, dataSource, metricName1);
                    }
                }

                return new DataSourcePointCut.DataSourceTracer(transaction, sig, (DataSource) ds, args, metricName1);
            }
        } else {
            if (parentTracer != null) {
                ClassMethodSignature metricName = parentTracer.getClassMethodSignature();
                if (metricName.getClassName().equals(sig.getClassName()) && metricName.getMethodName()
                                                                                    .equals(sig.getMethodName())) {
                    return null;
                }
            }

            metricName1 = new ClassMethodMetricNameFormat(sig, dataSource);
            return new DataSourcePointCut.ConnectionTracer(transaction, sig, dataSource, metricName1);
        }
    }

    private abstract static class AConnectionFactory implements ConnectionFactory {
        private final DataSource dataSource;
        private String url;
        private DatabaseVendor databaseVendor;

        public AConnectionFactory(DataSource dataSource) {
            this.databaseVendor = DatabaseVendor.UNKNOWN;
            this.dataSource = dataSource;
        }

        public String getUrl() {
            return this.url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        protected DataSource getDataSource() {
            return this.dataSource;
        }

        public DatabaseVendor getDatabaseVendor() {
            return this.databaseVendor;
        }

        void setDatabaseVendor(DatabaseVendor databaseVendor) {
            this.databaseVendor = databaseVendor;
        }
    }

    private static class DataSourceConnectionFactory extends DataSourcePointCut.AConnectionFactory {
        private final String password;
        private final String username;

        public DataSourceConnectionFactory(DataSource dataSource, String username, String password) {
            super(dataSource);
            this.username = username;
            this.password = password;
        }

        public Connection getConnection() throws SQLException {
            return this.getDataSource().getConnection(this.username, this.password);
        }
    }

    private static class NoArgsDataSourceConnectionFactory extends DataSourcePointCut.AConnectionFactory {
        public NoArgsDataSourceConnectionFactory(DataSource dataSource) {
            super(dataSource);
        }

        public Connection getConnection() throws SQLException {
            return this.getDataSource().getConnection();
        }
    }

    private static class DataSourceTracer extends DataSourcePointCut.ConnectionTracer {
        private DataSourcePointCut.AConnectionFactory connectionFactory;

        public DataSourceTracer(Transaction transaction, ClassMethodSignature sig, DataSource dataSource, Object[] args,
                                ClassMethodMetricNameFormat metricName) {
            super(transaction, sig, dataSource, metricName);
            if (args.length == 2) {
                this.connectionFactory =
                        new DataSourcePointCut.DataSourceConnectionFactory(dataSource, (String) args[0],
                                                                                  (String) args[1]);
            } else {
                this.connectionFactory = new DataSourcePointCut.NoArgsDataSourceConnectionFactory(dataSource);
            }

        }

        protected void doFinish(int opcode, Object connection) {
            if (connection != null) {
                DatabaseVendor databaseVendor = DatabaseUtils.getDatabaseVendor((Connection) connection);
                this.connectionFactory.setDatabaseVendor(databaseVendor);
                SqlDriverPointCut
                        .putConnectionFactory(this.getTransaction(), (Connection) connection, this.connectionFactory);
            }

        }
    }

    private static class ConnectionTracer extends DefaultTracer implements DatabaseTracer {
        private Throwable throwable;

        public ConnectionTracer(Transaction transaction, ClassMethodSignature sig, Object object,
                                MetricNameFormat metricNameFormatter) {
            super(transaction, sig, object, metricNameFormatter);
        }

        public void finish(Throwable pThrowable) {
            this.throwable = pThrowable;
            super.finish(this.throwable);
        }

        protected void doRecordMetrics(TransactionStats transactionStats) {
            transactionStats.getUnscopedStats().getResponseTimeStats("Datastore/getConnection")
                    .recordResponseTime(this.getExclusiveDuration(), TimeUnit.NANOSECONDS);
            if (this.throwable != null) {
                this.getTransactionActivity().getTransactionStats().getUnscopedStats().getStats("DatastoreErrors/all")
                        .incrementCallCount();
            }

        }
    }
}
