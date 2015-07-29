package com.newrelic.agent.attributes;

import java.util.Map;
import java.util.logging.Level;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.deps.com.google.common.collect.Maps;

public class AttributesService extends AbstractService implements AgentConfigListener {
    private final boolean enabled;
    private final String defaultAppName;
    private final Map<String, AttributesFilter> appNamesToFilters;
    private volatile AttributesFilter defaultFilter;

    public AttributesService() {
        super(AttributesService.class.getSimpleName());
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        this.enabled = ((Boolean) config.getValue("attributes.enabled", Boolean.TRUE)).booleanValue();
        this.defaultAppName = config.getApplicationName();
        this.defaultFilter = new AttributesFilter(config);
        this.appNamesToFilters = Maps.newConcurrentMap();

        ServiceFactory.getConfigService().addIAgentConfigListener(this);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    protected void doStart() throws Exception {
        logDeprecatedSettings();
    }

    protected void doStop() throws Exception {
        ServiceFactory.getConfigService().removeIAgentConfigListener(this);
    }

    public boolean captureRequestParams(String appName) {
        return getFilter(appName).captureRequestParams();
    }

    public boolean captureMessageParams(String appName) {
        return getFilter(appName).captureMessageParams();
    }

    public boolean isAttributesEnabledForErrors(String appName) {
        return getFilter(appName).isAttributesEnabledForErrors();
    }

    public boolean isAttributesEnabledForEvents(String appName) {
        return getFilter(appName).isAttributesEnabledForEvents();
    }

    public boolean isAttributesEnabledForTraces(String appName) {
        return getFilter(appName).isAttributesEnabledForTraces();
    }

    public boolean isAttributesEnabledForBrowser(String appName) {
        return getFilter(appName).isAttributesEnabledForBrowser();
    }

    public Map<String, ? extends Object> filterErrorAttributes(String appName, Map<String, ? extends Object> values) {
        return getFilter(appName).filterErrorAttributes(values);
    }

    public Map<String, ? extends Object> filterEventAttributes(String appName, Map<String, ? extends Object> values) {
        return getFilter(appName).filterEventAttributes(values);
    }

    public Map<String, ? extends Object> filterTraceAttributes(String appName, Map<String, ? extends Object> values) {
        return getFilter(appName).filterTraceAttributes(values);
    }

    public Map<String, ? extends Object> filterBrowserAttributes(String appName, Map<String, ? extends Object> values) {
        return getFilter(appName).filterBrowserAttributes(values);
    }

    private AttributesFilter getFilter(String appName) {
        if ((appName == null) || (appName.equals(this.defaultAppName))) {
            return this.defaultFilter;
        }
        AttributesFilter filter = (AttributesFilter) this.appNamesToFilters.get(appName);
        return filter == null ? this.defaultFilter : filter;
    }

    public void configChanged(String appName, AgentConfig agentConfig) {
        if (appName != null) {
            if (appName.equals(this.defaultAppName)) {
                this.defaultFilter = new AttributesFilter(agentConfig);
            } else {
                this.appNamesToFilters.put(appName, new AttributesFilter(agentConfig));
            }
        }
    }

    private void logDeprecatedSettings() {
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();

        if (config.getValue("analytics_events.capture_attributes") != null) {
            getLogger().log(Level.INFO,
                                   "The property analytics_events.capture_attributes is deprecated. Change to "
                                           + "transaction_events.attributes.enabled.");
        }

        if (config.getValue("transaction_tracer.capture_attributes") != null) {
            getLogger().log(Level.INFO,
                                   "The property transaction_tracer.captures_attributes is deprecated. Change to "
                                           + "transaction_tracer.attributes.enabled.");
        }

        if (config.getValue("browser_monitoring.capture_attributes") != null) {
            getLogger().log(Level.INFO,
                                   "The property browser_monitoring.capture_attributes is deprecated. Change to "
                                           + "browser_monitoring.attributes.enabled.");
        }

        if (config.getValue("error_collector.capture_attributes") != null) {
            getLogger().log(Level.INFO,
                                   "The property error_collector.capture_attributes is deprecated. Change to "
                                           + "error_collector.attributes.enabled.");
        }

        if (config.getValue("capture_params") != null) {
            getLogger().log(Level.INFO,
                                   "The property capture_params is deprecated. Request parameters are off by default."
                                           + " To enable request parameters, use attributes.include = request"
                                           + ".parameters.*");
        }

        if (config.getValue("capture_messaging_params") != null) {
            getLogger().log(Level.INFO,
                                   "The property capture_messaging_params is deprecated. Message queue parameters are"
                                           + " off by default. To enable message queue parameters, use attributes.include = message.parameters.*");
        }

        if (config.getValue("ignored_params") != null) {
            getLogger().log(Level.INFO,
                                   "The property ignored_params is deprecated. Change to attributes.exclude = request.parameters.${param_name}");
        }

        if (config.getValue("ignored_messaging_params") != null) {
            getLogger().log(Level.INFO,
                                   "The property ignored_messaging_params is deprecated. Change to attributes.exclude"
                                           + " = message.parameters.${param_name}");
        }
    }
}