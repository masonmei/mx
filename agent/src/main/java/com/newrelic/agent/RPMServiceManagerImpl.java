package com.newrelic.agent;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.newrelic.agent.application.PriorityApplicationName;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;

public class RPMServiceManagerImpl extends AbstractService implements RPMServiceManager {
    private final IRPMService defaultRPMService;
    private final Map<String, IRPMService> appNameToRPMService = new ConcurrentHashMap();
    private final List<ConnectionListener> connectionListeners = new CopyOnWriteArrayList();
    private final ConnectionListener connectionListener;
    private volatile List<IRPMService> rpmServices;

    public RPMServiceManagerImpl() {
        super(RPMServiceManager.class.getSimpleName());
        connectionListener = new ConnectionListener() {
            public void connected(IRPMService rpmService, Map<String, Object> connectionInfo) {
                for (ConnectionListener each : connectionListeners) {
                    each.connected(rpmService, connectionInfo);
                }
            }

            public void disconnected(IRPMService rpmService) {
                for (ConnectionListener each : connectionListeners) {
                    each.disconnected(rpmService);
                }
            }
        };
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        String host = config.getHost();
        String port = Integer.toString(config.getPort());
        getLogger().config(MessageFormat
                                   .format("Configured to connect to New Relic at {0}:{1}", new Object[] {host, port}));
        defaultRPMService = createRPMService(config.getApplicationNames(), connectionListener);
        List list = new ArrayList(1);
        list.add(defaultRPMService);
        rpmServices = Collections.unmodifiableList(list);
    }

    protected synchronized void doStart() throws Exception {
        for (IRPMService rpmService : rpmServices) {
            rpmService.start();
        }
    }

    protected synchronized void doStop() throws Exception {
        for (IRPMService rpmService : rpmServices) {
            rpmService.stop();
        }
    }

    public boolean isEnabled() {
        return true;
    }

    public void addConnectionListener(ConnectionListener listener) {
        connectionListeners.add(listener);
    }

    public void removeConnectionListener(ConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    public IRPMService getRPMService() {
        return defaultRPMService;
    }

    public IRPMService getRPMService(String appName) {
        if ((appName == null) || (defaultRPMService.getApplicationName().equals(appName))) {
            return defaultRPMService;
        }
        return (IRPMService) appNameToRPMService.get(appName);
    }

    public IRPMService getOrCreateRPMService(PriorityApplicationName appName) {
        IRPMService rpmService = getRPMService(appName.getName());
        if (rpmService != null) {
            return rpmService;
        }
        return createRPMServiceForAppName(appName.getName(), appName.getNames());
    }

    public IRPMService getOrCreateRPMService(String appName) {
        IRPMService rpmService = getRPMService(appName);
        if (rpmService != null) {
            return rpmService;
        }
        List appNames = new ArrayList(1);
        appNames.add(appName);
        return createRPMServiceForAppName(appName, appNames);
    }

    private synchronized IRPMService createRPMServiceForAppName(String appName, List<String> appNames) {
        IRPMService rpmService = getRPMService(appName);
        if (rpmService == null) {
            rpmService = createRPMService(appNames, connectionListener);
            appNameToRPMService.put(appName, rpmService);
            List list = new ArrayList(appNameToRPMService.size() + 1);
            list.addAll(appNameToRPMService.values());
            list.add(defaultRPMService);
            rpmServices = Collections.unmodifiableList(list);
            if (isStarted()) {
                try {
                    rpmService.start();
                } catch (Exception e) {
                    String msg = MessageFormat.format("Error starting New Relic Service for {0}: {1}",
                                                             new Object[] {rpmService.getApplicationName(), e});

                    getLogger().severe(msg);
                }
            }
        }
        return rpmService;
    }

    protected IRPMService createRPMService(List<String> appNames, ConnectionListener listener) {
        return new RPMService(appNames, listener);
    }

    public List<IRPMService> getRPMServices() {
        return rpmServices;
    }
}