package com.newrelic.agent.jmx.values;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.jmx.create.JmxAttributeFilter;
import com.newrelic.agent.jmx.metrics.BaseJmxValue;
import com.newrelic.agent.jmx.metrics.DataSourceJmxMetricGenerator;
import com.newrelic.agent.jmx.metrics.EjbPoolJmxMetricGenerator;
import com.newrelic.agent.jmx.metrics.EjbTransactionJmxMetricGenerator;
import com.newrelic.agent.jmx.metrics.JMXMetricType;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.jmx.metrics.JtaJmxMetricGenerator;
import com.newrelic.agent.jmx.metrics.ServerJmxMetricGenerator;

public class WeblogicJmxValues extends JmxFrameworkValues {
    private static final int METRIC_COUNT = 1;
    private static final List<BaseJmxValue> METRICS = new ArrayList(1);
    private static final JmxAttributeFilter FILTER = new JmxAttributeFilter() {
        public boolean keepMetric(String rootMetricName) {
            if (rootMetricName.contains("/uuid-")) {
                Agent.LOG.log(Level.FINER,
                                     "Weblogic JMX metric {0} is being ignored because it appears to be an instance.",
                                     new Object[] {rootMetricName});

                return false;
            }
            return true;
        }
    };
    private static String PREFIX = "com.bea";

    static {
        METRICS.add(new BaseJmxValue("com.bea:ServerRuntime=*,Name=ThreadPoolRuntime,Type=ThreadPoolRuntime",
                                            "JmxBuiltIn/ThreadPool/{Name}/",
                                            new JmxMetric[] {ServerJmxMetricGenerator.ACTIVE_THREAD_POOL_COUNT
                                                                     .createMetric("HoggingThreadCount"),
                                                                    ServerJmxMetricGenerator.IDLE_THREAD_POOL_COUNT
                                                                            .createMetric("ExecuteThreadIdleCount"),
                                                                    ServerJmxMetricGenerator.STANDBY_THREAD_POOL_COUNT
                                                                            .createMetric("StandbyThreadCount")}));

        METRICS.add(new BaseJmxValue("com.bea:ServerRuntime=*,Name=*,Type=JDBCDataSourceRuntime",
                                            "JmxBuiltIn/DataSources/{Name}/",
                                            new JmxMetric[] {DataSourceJmxMetricGenerator.CONNECTIONS_AVAILABLE
                                                                     .createMetric(new String[] {"NumAvailable"}),
                                                                    DataSourceJmxMetricGenerator.CONNECTIONS_POOL_SIZE
                                                                            .createMetric(new String[]
                                                                                                  {"CurrCapacity"}),
                                                                    DataSourceJmxMetricGenerator.CONNECTIONS_CREATED
                                                                            .createMetric(new String[]
                                                                                                  {"ConnectionsTotalCount"}),
                                                                    DataSourceJmxMetricGenerator.CONNECTIONS_ACTIVE
                                                                            .createMetric(new String[]
                                                                                                  {"ActiveConnectionsCurrentCount"}),
                                                                    DataSourceJmxMetricGenerator.CONNECTIONS_LEAKED
                                                                            .createMetric(new String[]
                                                                                                  {"LeakedConnectionCount"}),
                                                                    DataSourceJmxMetricGenerator.CONNECTIONS_CACHE_SIZE
                                                                            .createMetric(new String[]
                                                                                                  {"PrepStmtCacheCurrentSize"}),
                                                                    DataSourceJmxMetricGenerator
                                                                            .CONNECTION_REQUEST_WAITING_COUNT
                                                                            .createMetric(new String[]
                                                                                                  {"WaitingForConnectionCurrentCount"}),
                                                                    DataSourceJmxMetricGenerator
                                                                            .CONNECTION_REQUEST_TOTAL_COUNT
                                                                            .createMetric(new String[]
                                                                                                  {"WaitingForConnectionTotal"}),
                                                                    DataSourceJmxMetricGenerator
                                                                            .CONNECTION_REQUEST_SUCCESS
                                                                            .createMetric(new String[]
                                                                                                  {"WaitingForConnectionSuccessTotal"}),
                                                                    DataSourceJmxMetricGenerator
                                                                            .CONNECTION_REQUEST_FAILURE
                                                                            .createMetric(new String[]
                                                                                                  {"WaitingForConnectionFailureTotal"})}));

        METRICS.add(new BaseJmxValue("com.bea:ServerRuntime=*,Name=*,ApplicationRuntime=*,Type=EJBPoolRuntime,"
                                             + "EJBComponentRuntime=*,*",
                                            "JmxBuiltIn/EJB/Pool/Bean/{ApplicationRuntime}/{EJBComponentRuntime"
                                                    + "}/{Name}/",
                                            FILTER, new JmxMetric[] {EjbPoolJmxMetricGenerator.SUCCESS
                                                                             .createMetric(new String[]
                                                                                                   {"AccessTotalCount",
                                                                                                                "MissTotalCount"}),
                                                                            EjbPoolJmxMetricGenerator.FAILURE
                                                                                    .createMetric(new String[]
                                                                                                          {"MissTotalCount"}),
                                                                            EjbPoolJmxMetricGenerator.THREADS_WAITING
                                                                                    .createMetric(new String[]
                                                                                                          {"WaiterCurrentCount"}),
                                                                            EjbPoolJmxMetricGenerator.DESTROY
                                                                                    .createMetric(new String[]
                                                                                                          {"DestroyedTotalCount"}),
                                                                            EjbPoolJmxMetricGenerator.ACTIVE
                                                                                    .createMetric(new String[]
                                                                                                          {"BeansInUseCurrentCount"}),
                                                                            EjbPoolJmxMetricGenerator.AVAILABLE
                                                                                    .createMetric(new String[] {"PooledBeansCurrentCount"})}));

        METRICS.add(new BaseJmxValue("com.bea:ServerRuntime=*,Name=*,ApplicationRuntime=*,Type=EJBTransactionRuntime,EJBComponentRuntime=*,*",
                                            "JmxBuiltIn/EJB/Transactions/Application/{ApplicationRuntime}/", FILTER,
                                            null, JMXMetricType.SUM_ALL_BEANS,
                                            new JmxMetric[] {EjbTransactionJmxMetricGenerator.COUNT
                                                                     .createMetric(new String[] {"TransactionsCommittedTotalCount",
                                                                                                        "TransactionsRolledBackTotalCount",
                                                                                                        "TransactionsTimedOutTotalCount"}),
                                                                    EjbTransactionJmxMetricGenerator.COMMIT
                                                                            .createMetric(new String[] {"TransactionsCommittedTotalCount"}),
                                                                    EjbTransactionJmxMetricGenerator.ROLLBACK
                                                                            .createMetric(new String[] {"TransactionsRolledBackTotalCount"}),
                                                                    EjbTransactionJmxMetricGenerator.TIMEOUT
                                                                            .createMetric(new String[] {"TransactionsTimedOutTotalCount"})}));

        METRICS.add(new BaseJmxValue("com.bea:ServerRuntime=*,Name=*,ApplicationRuntime=*,Type=EJBTransactionRuntime,EJBComponentRuntime=*,*",
                                            "JmxBuiltIn/EJB/Transactions/Module/{ApplicationRuntime}/{EJBComponentRuntime}/",
                                            FILTER, null, JMXMetricType.SUM_ALL_BEANS,
                                            new JmxMetric[] {EjbTransactionJmxMetricGenerator.COUNT
                                                                     .createMetric(new String[] {"TransactionsCommittedTotalCount",
                                                                                                        "TransactionsRolledBackTotalCount",
                                                                                                        "TransactionsTimedOutTotalCount"}),
                                                                    EjbTransactionJmxMetricGenerator.COMMIT
                                                                            .createMetric(new String[] {"TransactionsCommittedTotalCount"}),
                                                                    EjbTransactionJmxMetricGenerator.ROLLBACK
                                                                            .createMetric(new String[] {"TransactionsRolledBackTotalCount"}),
                                                                    EjbTransactionJmxMetricGenerator.TIMEOUT
                                                                            .createMetric(new String[] {"TransactionsTimedOutTotalCount"})}));

        METRICS.add(new BaseJmxValue("com.bea:ServerRuntime=*,Name=*,ApplicationRuntime=*,Type=EJBTransactionRuntime,EJBComponentRuntime=*,*",
                                            "JmxBuiltIn/EJB/Transactions/Bean/{ApplicationRuntime}/{EJBComponentRuntime}/{Name}/",
                                            FILTER, new JmxMetric[] {EjbTransactionJmxMetricGenerator.COUNT
                                                                             .createMetric(new String[] {"TransactionsCommittedTotalCount",
                                                                                                                "TransactionsRolledBackTotalCount",
                                                                                                                "TransactionsTimedOutTotalCount"}),
                                                                            EjbTransactionJmxMetricGenerator.COMMIT
                                                                                    .createMetric(new String[] {"TransactionsCommittedTotalCount"}),
                                                                            EjbTransactionJmxMetricGenerator.ROLLBACK
                                                                                    .createMetric(new String[] {"TransactionsRolledBackTotalCount"}),
                                                                            EjbTransactionJmxMetricGenerator.TIMEOUT
                                                                                    .createMetric(new String[] {"TransactionsTimedOutTotalCount"})}));

        METRICS.add(new BaseJmxValue("com.bea:ServerRuntime=*,Name=JTARuntime,Type=JTARuntime",
                                            "JmxBuiltIn/JTA/{Name}/", FILTER,
                                            new JmxMetric[] {JtaJmxMetricGenerator.COUNT
                                                                     .createMetric(new String[] {"TransactionTotalCount"}),
                                                                    JtaJmxMetricGenerator.COMMIT
                                                                            .createMetric(new String[] {"TransactionCommittedTotalCount"}),
                                                                    JtaJmxMetricGenerator.ROLLBACK
                                                                            .createMetric(new String[] {"TransactionRolledBackTotalCount"}),
                                                                    JtaJmxMetricGenerator.ABANDONDED
                                                                            .createMetric(new String[] {"TransactionAbandonedTotalCount"})}));
    }

    public List<BaseJmxValue> getFrameworkMetrics() {
        return METRICS;
    }

    public String getPrefix() {
        return PREFIX;
    }
}