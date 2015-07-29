package com.newrelic.agent;

import java.util.logging.Level;

import com.newrelic.agent.bridge.NoOpMetricAggregator;
import com.newrelic.agent.bridge.NoOpTracedMethod;
import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.Insights;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;

public class AgentImpl implements com.newrelic.agent.bridge.Agent {
    private final Logger logger;

    public AgentImpl(Logger logger) {
        this.logger = logger;
    }

    public TracedMethod getTracedMethod() {
        getLogger().log(Level.FINER, "Unexpected call to Agent.getTracedMethod()", new Object[0]);
        return NoOpTracedMethod.INSTANCE;
    }

    public com.newrelic.agent.bridge.Transaction getTransaction() {
        Transaction innerTx = Transaction.getTransaction();
        if (innerTx != null) {
            return new TransactionApiImpl();
        }
        return NoOpTransaction.INSTANCE;
    }

    public Logger getLogger() {
        return this.logger;
    }

    public Config getConfig() {
        return ServiceFactory.getConfigService().getDefaultAgentConfig();
    }

    public MetricAggregator getMetricAggregator() {
        try {
            Transaction tx = Transaction.getTransaction();
            if ((null != tx) && (tx.isInProgress())) {
                return tx.getMetricAggregator();
            }
            return ServiceFactory.getStatsService().getMetricAggregator();
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINE, "getMetricAggregator() call failed : {0}", new Object[] {t.getMessage()});
            Agent.LOG.log(Level.FINEST, t, "getMetricAggregator() call failed", new Object[0]);
        }
        return NoOpMetricAggregator.INSTANCE;
    }

    public Insights getInsights() {
        return ServiceFactory.getServiceManager().getInsights();
    }
}