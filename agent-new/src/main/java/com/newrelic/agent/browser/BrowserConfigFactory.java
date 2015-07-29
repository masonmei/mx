package com.newrelic.agent.browser;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.BrowserMonitoringConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class BrowserConfigFactory
{
  public static IBrowserConfig createBrowserConfig(String appName, Map<String, Object> serverData)
  {
    try
    {
      IBrowserConfig browserConfig = createTheBrowserConfig(appName, serverData);
      AgentConfig agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();

      String autoInstrument = agentConfig.getBrowserMonitoringConfig().isAutoInstrumentEnabled() ? " with auto instrumentation" : "";

      String msg = MessageFormat.format("Real user monitoring is enabled{0} for application \"{1}\"", new Object[] { autoInstrument, appName });

      Agent.LOG.info(msg);
      return browserConfig;
    } catch (Exception e) {
      String msg = MessageFormat.format("Unable to configure application \"{0}\" for Real User Monitoring: {1}", new Object[] { appName, e });

      if (Agent.LOG.isLoggable(Level.FINEST))
        Agent.LOG.log(Level.FINEST, msg, e);
      else {
        Agent.LOG.finer(msg);
      }
      Agent.LOG.info(MessageFormat.format("Real user monitoring is not enabled for application \"{0}\"", new Object[] { appName }));
    }return null;
  }

  private static IBrowserConfig createTheBrowserConfig(String appName, Map<String, Object> serverData)
    throws Exception
  {
    Map settings = createMap();
    mergeBrowserSettings(settings, serverData);
    Map agentData = AgentConfigFactory.getAgentData(serverData);

    mergeBrowserSettings(settings, agentData);
    return BrowserConfig.createBrowserConfig(appName, settings);
  }

  private static void mergeBrowserSettings(Map<String, Object> settings, Map<String, Object> data) {
    if (data == null) {
      return;
    }
    mergeSetting("browser_key", settings, data);
    mergeSetting("browser_monitoring.loader_version", settings, data);
    mergeSetting("js_agent_loader", settings, data);
    mergeSetting("js_agent_file", settings, data);
    mergeSetting("beacon", settings, data);
    mergeSetting("error_beacon", settings, data);
    mergeSetting("application_id", settings, data);
  }

  private static void mergeSetting(String currentSetting, Map<String, Object> settings, Map<String, Object> data) {
    Object val = data.get(currentSetting);
    if (val != null)
      settings.put(currentSetting, val);
  }

  private static Map<String, Object> createMap()
  {
    return new HashMap();
  }
}