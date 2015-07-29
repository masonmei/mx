package com.newrelic.agent.config;

import com.newrelic.agent.Agent;
import com.newrelic.agent.logging.IAgentLogger;
import java.io.File;
import java.text.MessageFormat;
import java.util.Map;

public class ConfigServiceFactory
{
  public static ConfigService createConfigService(AgentConfig config, Map<String, Object> localSettings)
  {
    return new ConfigServiceImpl(config, null, localSettings);
  }

  public static ConfigService createConfigServiceUsingSettings(Map<String, Object> settings)
  {
    return new ConfigServiceImpl(AgentConfigImpl.createAgentConfig(settings), null, settings);
  }

  public static ConfigService createConfigService() throws ConfigurationException {
    File configFile = getConfigFile();
    Map configSettings = getConfigurationFileSettings(configFile);
    AgentConfig config = AgentConfigImpl.createAgentConfig(configSettings);
    validateConfig(config);
    return new ConfigServiceImpl(config, configFile, configSettings);
  }

  public static Map<String, Object> getConfigurationFileSettings(File configFile) throws ConfigurationException {
    String msg = MessageFormat.format("New Relic Agent: Loading configuration file \"{0}\"", new Object[] { configFile.getPath() });
    Agent.LOG.info(msg);
    try {
      return AgentConfigHelper.getConfigurationFileSettings(configFile);
    } catch (Exception e) {
      msg = MessageFormat.format("An error occurred reading the configuration file {0}. Check the permissions and format of the file. - {1}", new Object[] { configFile.getAbsolutePath(), e.toString() });

      throw new ConfigurationException(msg, e);
    }
  }

  private static File getConfigFile()
    throws ConfigurationException
  {
    File configFile = ConfigFileHelper.findConfigFile();
    if (configFile == null) {
      throw new ConfigurationException("Failed to find the configuration file");
    }
    return configFile;
  }

  private static void validateConfig(AgentConfig config) throws ConfigurationException {
    if (config.getApplicationName() == null)
      throw new ConfigurationException("The agent requires an application name.  Check the app_name setting in newrelic.yml");
  }
}