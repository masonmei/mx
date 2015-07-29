package com.newrelic.agent.jmx;

import com.newrelic.agent.Agent;
import com.newrelic.agent.IAgent;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.logging.AgentLogManager;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import java.text.MessageFormat;
import javax.management.NotCompliantMBeanException;

public class AgentMBeanImpl extends BaseMBean
  implements AgentMBean
{
  protected AgentMBeanImpl()
    throws NotCompliantMBeanException
  {
    super(AgentMBean.class);
  }

  public boolean shutdown()
  {
    Agent.LOG.info("AgentMBean is shutting down the Agent");
    getAgent().shutdown();
    return true;
  }

  public boolean reconnect()
  {
    try {
      IRPMService rpmService = ServiceFactory.getRPMService();
      Agent.LOG.info(MessageFormat.format("AgentMBean is reconnecting {0}", new Object[] { rpmService.getApplicationName() }));
      rpmService.reconnect();
      return true; } catch (Exception ex) {
    }
    return false;
  }

  private IAgent getAgent()
  {
    return ServiceFactory.getAgent();
  }

  public boolean isStarted()
  {
    return getAgent().isStarted();
  }

  public boolean isConnected()
  {
    return ServiceFactory.getRPMService().isConnected();
  }

  public boolean connect()
  {
    try {
      IRPMService rpmService = ServiceFactory.getRPMService();
      Agent.LOG.info(MessageFormat.format("AgentMBean is connecting {0}", new Object[] { rpmService.getApplicationName() }));
      rpmService.launch();
      return true;
    } catch (Exception ex) {
      Agent.LOG.severe("Connect error: " + ex.getMessage());
    }
    return false;
  }

  public String setLogLevel(String level)
  {
    AgentLogManager.setLogLevel(level);
    Agent.LOG.info(MessageFormat.format("AgentMBean is setting log level to {0}", new Object[] { level }));
    return level;
  }

  public String getLogLevel()
  {
    return AgentLogManager.getLogLevel();
  }
}