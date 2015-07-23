package com.newrelic.agent.browser;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.newrelic.agent.ConnectionListener;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;

public class BrowserServiceImpl extends AbstractService implements BrowserService, ConnectionListener {
    private final ConcurrentMap<String, IBrowserConfig> browserConfigs = new ConcurrentHashMap();
    private final String defaultAppName;
    private volatile IBrowserConfig defaultBrowserConfig = null;

    public BrowserServiceImpl() {
        super(ConnectionListener.class.getSimpleName());
        defaultAppName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
    }

    protected void doStart() throws Exception {
        ServiceFactory.getRPMServiceManager().addConnectionListener(this);
    }

    protected void doStop() throws Exception {
        ServiceFactory.getRPMServiceManager().removeConnectionListener(this);
    }

    public IBrowserConfig getBrowserConfig(String appName) {
        if ((appName == null) || (appName.equals(defaultAppName))) {
            return defaultBrowserConfig;
        }
        return (IBrowserConfig) browserConfigs.get(appName);
    }

    public boolean isEnabled() {
        return true;
    }

    public void connected(IRPMService rpmService, Map<String, Object> serverData) {
        String appName = rpmService.getApplicationName();
        IBrowserConfig browserConfig = BrowserConfigFactory.createBrowserConfig(appName, serverData);
        if ((appName == null) || (appName.equals(defaultAppName))) {
            defaultBrowserConfig = browserConfig;
        } else if (browserConfig == null) {
            browserConfigs.remove(appName);
        } else {
            browserConfigs.put(appName, browserConfig);
        }
    }

    public void disconnected(IRPMService rpmService) {
    }
}