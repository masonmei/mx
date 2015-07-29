package com.newrelic.agent.sql;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.TransactionStats;

public class SqlTraceServiceImpl extends AbstractService implements SqlTraceService, TransactionListener,
                                                                            HarvestListener {
    private static final SqlTracerListener NOP_SQL_TRACER_LISTENER = new NopSqlTracerListener();

    private final ConcurrentMap<String, SqlTracerAggregator> sqlTracerAggregators = new ConcurrentHashMap();
    private final SqlTracerAggregator defaultSqlTracerAggregator;
    private final String defaultAppName;

    public SqlTraceServiceImpl() {
        super(HarvestListener.class.getSimpleName());
        this.defaultAppName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
        this.defaultSqlTracerAggregator = createSqlTracerAggregator();
    }

    private boolean isEnabled(AgentConfig agentConfig) {
        if (!agentConfig.getSqlTraceConfig().isEnabled()) {
            return false;
        }
        TransactionTracerConfig ttConfig = agentConfig.getTransactionTracerConfig();
        if ("off".equals(ttConfig.getRecordSql())) {
            return false;
        }
        if (ttConfig.isLogSql()) {
            return false;
        }
        return ttConfig.isEnabled();
    }

    public boolean isEnabled() {
        return true;
    }

    protected void doStart() {
        ServiceFactory.getTransactionService().addTransactionListener(this);
        ServiceFactory.getHarvestService().addHarvestListener(this);
    }

    protected void doStop() {
        ServiceFactory.getTransactionService().removeTransactionListener(this);
        ServiceFactory.getHarvestService().removeHarvestListener(this);
    }

    public void dispatcherTransactionFinished(TransactionData td, TransactionStats transactionStats) {
        SqlTracerAggregator aggregator = getOrCreateSqlTracerAggregator(td.getApplicationName());
        aggregator.addSqlTracers(td);
    }

    public SqlTracerListener getSqlTracerListener(String appName) {
        AgentConfig agentConfig = ServiceFactory.getConfigService().getAgentConfig(appName);
        if (isEnabled(agentConfig)) {
            double threshold = agentConfig.getTransactionTracerConfig().getExplainThresholdInMillis();
            return new DefaultSqlTracerListener(threshold);
        }
        return NOP_SQL_TRACER_LISTENER;
    }

    public void afterHarvest(String appName) {
    }

    public void beforeHarvest(String appName, StatsEngine statsEngine) {
        SqlTracerAggregator aggregator = getOrCreateSqlTracerAggregator(appName);
        List sqlTraces = aggregator.getAndClearSqlTracers();
        try {
            ServiceFactory.getRPMService(appName).sendSqlTraceData(sqlTraces);
        } catch (Exception e) {
            String msg = MessageFormat.format("Error sending sql traces for {0}: {1}", new Object[] {appName, e});
            if (getLogger().isLoggable(Level.FINEST)) {
                getLogger().log(Level.FINEST, msg, e);
            } else {
                getLogger().fine(msg);
            }
        }
    }

    private SqlTracerAggregator getOrCreateSqlTracerAggregator(String appName) {
        SqlTracerAggregator sqlTracerAggregator = getSqlTracerAggregator(appName);
        if (sqlTracerAggregator != null) {
            return sqlTracerAggregator;
        }
        sqlTracerAggregator = createSqlTracerAggregator();
        SqlTracerAggregator oldSqlTracerAggregator =
                (SqlTracerAggregator) this.sqlTracerAggregators.putIfAbsent(appName, sqlTracerAggregator);
        return oldSqlTracerAggregator == null ? sqlTracerAggregator : oldSqlTracerAggregator;
    }

    private SqlTracerAggregator getSqlTracerAggregator(String appName) {
        if ((appName == null) || (appName.equals(this.defaultAppName))) {
            return this.defaultSqlTracerAggregator;
        }
        return (SqlTracerAggregator) this.sqlTracerAggregators.get(appName);
    }

    private SqlTracerAggregator createSqlTracerAggregator() {
        return new SqlTracerAggregatorImpl();
    }
}