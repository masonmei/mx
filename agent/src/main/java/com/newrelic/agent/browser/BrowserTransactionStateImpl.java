package com.newrelic.agent.browser;

import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.ITransaction;
import com.newrelic.agent.attributes.AttributesUtils;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.deps.com.google.common.collect.Maps;

public class BrowserTransactionStateImpl implements BrowserTransactionState {
    private final Object lock = new Object();
    private final ITransaction tx;
    private boolean browserHeaderRendered;
    private boolean browserFooterRendered;

    protected BrowserTransactionStateImpl(ITransaction tx) {
        this.tx = tx;
    }

    public static BrowserTransactionState create(ITransaction tx) {
        return tx == null ? null : new BrowserTransactionStateImpl(tx);
    }

    public String getBrowserTimingHeaderForJsp() {
        synchronized(lock) {
            if (!canRenderHeaderForJsp()) {
                return "";
            }
            return getBrowserTimingHeader2();
        }
    }

    public String getBrowserTimingHeader() {
        synchronized(lock) {
            if (!canRenderHeader()) {
                return "";
            }
            return getBrowserTimingHeader2();
        }
    }

    private String getBrowserTimingHeader2() {
        IBrowserConfig config = getBeaconConfig();
        if (config == null) {
            Agent.LOG.finer("Real user monitoring is disabled");
            return "";
        }
        String header = config.getBrowserTimingHeader();
        browserHeaderRendered = true;
        return header;
    }

    public String getBrowserTimingFooter() {
        synchronized(lock) {
            if (!canRenderFooter()) {
                return "";
            }
            return getBrowserTimingFooter2();
        }
    }

    private String getBrowserTimingFooter2() {
        IBrowserConfig config = getBeaconConfig();
        if (config == null) {
            Agent.LOG.finer("Real user monitoring is disabled");
            return "";
        }

        tx.freezeTransactionName();
        if (tx.isIgnore()) {
            Agent.LOG.finer("Unable to get browser timing footer: transaction is ignore");
            return "";
        }
        String footer = config.getBrowserTimingFooter(this);
        if (!footer.isEmpty()) {
            browserFooterRendered = true;
        }
        return footer;
    }

    private boolean canRenderHeader() {
        if (!tx.isInProgress()) {
            Agent.LOG.finer("Unable to get browser timing header: transaction has no tracers");
            return false;
        }
        if (tx.isIgnore()) {
            Agent.LOG.finer("Unable to get browser timing header: transaction is ignore");
            return false;
        }
        if (browserHeaderRendered) {
            Agent.LOG.finer("browser timing header already rendered");
            return false;
        }
        return true;
    }

    private boolean canRenderHeaderForJsp() {
        if (!canRenderHeader()) {
            return false;
        }
        Dispatcher dispatcher = tx.getDispatcher();
        if ((dispatcher == null) || (!dispatcher.isWebTransaction())) {
            Agent.LOG.finer("Unable to get browser timing header: transaction is not a web transaction");
            return false;
        }
        try {
            String contentType = dispatcher.getResponse().getContentType();
            if (!isHtml(contentType)) {
                String msg = MessageFormat
                                     .format("Unable to inject browser timing header in a JSP: bad content type: {0}",
                                                    new Object[] {contentType});

                Agent.LOG.finer(msg);
                return false;
            }
        } catch (Exception e) {
            String msg = MessageFormat
                                 .format("Unable to inject browser timing header in a JSP: exception getting content "
                                                 + "type: {0}", new Object[] {e});

            if (Agent.LOG.isLoggable(Level.FINEST)) {
                Agent.LOG.log(Level.FINEST, msg, e);
            } else if (Agent.LOG.isLoggable(Level.FINER)) {
                Agent.LOG.finer(msg);
            }
            return false;
        }
        return true;
    }

    private boolean isHtml(String contentType) {
        return (contentType != null) && ((contentType.startsWith("text/html")) || (contentType
                                                                                           .startsWith("text/xhtml")));
    }

    private boolean canRenderFooter() {
        if (!tx.isInProgress()) {
            Agent.LOG.finer("Unable to get browser timing footer: transaction has no tracers");
            return false;
        }
        if (tx.isIgnore()) {
            Agent.LOG.finer("Unable to get browser timing footer: transaction is ignore");
            return false;
        }
        if ((browserFooterRendered) && (!tx.getAgentConfig().getBrowserMonitoringConfig().isAllowMultipleFooters())) {
            Agent.LOG.finer("browser timing footer already rendered");
            return false;
        }
        if (browserHeaderRendered) {
            return true;
        }
        IBrowserConfig config = getBeaconConfig();
        if (config == null) {
            Agent.LOG.finer("Real user monitoring is disabled");
            return false;
        }
        Agent.LOG.finer("getBrowserTimingFooter() was invoked without a call to getBrowserTimingHeader()");
        return false;
    }

    protected IBrowserConfig getBeaconConfig() {
        String appName = tx.getApplicationName();
        return ServiceFactory.getBeaconService().getBrowserConfig(appName);
    }

    public long getDurationInMilliseconds() {
        return TimeUnit.MILLISECONDS.convert(tx.getRunningDurationInNanos(), TimeUnit.NANOSECONDS);
    }

    public long getExternalTimeInMilliseconds() {
        return tx.getExternalTime();
    }

    public String getTransactionName() {
        return tx.getPriorityTransactionName().getName();
    }

    public Map<String, Object> getUserAttributes() {
        return tx.getUserAttributes();
    }

    public Map<String, Object> getAgentAttributes() {
        synchronized(lock) {
            Map atts = Maps.newHashMap();
            atts.putAll(tx.getAgentAttributes());
            atts.putAll(AttributesUtils.appendAttributePrefixes(tx.getPrefixedAgentAttributes()));
            return atts;
        }
    }

    public String getAppName() {
        return tx.getApplicationName();
    }
}