package com.newrelic.agent.browser;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.service.ServiceFactory;

public class BrowserConfigFactory {
    public static IBrowserConfig createBrowserConfig(String appName, Map<String, Object> serverData) {
        try {
            IBrowserConfig browserConfig = createTheBrowserConfig(appName, serverData);
            AgentConfig agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();

            String autoInstrument =
                    agentConfig.getBrowserMonitoringConfig().isAutoInstrumentEnabled() ? " with auto instrumentation"
                            : "";

            String msg = MessageFormat
                                 .format("Real user monitoring is enabled{0} for application \"{1}\"", autoInstrument,
                                                appName);

            Agent.LOG.info(msg);
            return browserConfig;
        } catch (Exception e) {
            String msg = MessageFormat.format("Unable to configure application \"{0}\" for Real User Monitoring: {1}",
                                                     appName, e);

            if (Agent.LOG.isLoggable(Level.FINEST)) {
                Agent.LOG.log(Level.FINEST, msg, e);
            } else {
                Agent.LOG.finer(msg);
            }
            Agent.LOG
                    .info(MessageFormat.format("Real user monitoring is not enabled for application \"{0}\"", appName));
        }
        return null;
    }

    private static IBrowserConfig createTheBrowserConfig(String appName, Map<String, Object> serverData)
            throws Exception {
        Map<String, Object> settings = createMap();
        mergeBrowserSettings(settings, serverData);
        Map agentData = AgentConfigFactory.getAgentData(serverData);

        mergeBrowserSettings(settings, agentData);
        return BrowserConfig.createBrowserConfig(appName, settings);
    }

    private static void mergeBrowserSettings(Map<String, Object> settings, Map<String, Object> data) {
        if (data == null) {
            return;
        }
        mergeSetting(BrowserConfig.BROWSER_KEY, settings, data);
        mergeSetting(BrowserConfig.BROWSER_LOADER_VERSION, settings, data);
        mergeSetting(BrowserConfig.JS_AGENT_LOADER, settings, data);
        mergeSetting(BrowserConfig.JS_AGENT_FILE, settings, data);
        mergeSetting(BrowserConfig.BEACON, settings, data);
        mergeSetting(BrowserConfig.ERROR_BEACON, settings, data);
        mergeSetting(BrowserConfig.APPLICATION_ID, settings, data);
    }

    private static void mergeSetting(String currentSetting, Map<String, Object> settings, Map<String, Object> data) {
        Object val = data.get(currentSetting);
        if (val != null) {
            settings.put(currentSetting, val);
        }
    }

    private static Map<String, Object> createMap() {
        return new HashMap<String, Object>();
    }
}