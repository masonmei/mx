package com.newrelic.agent;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;

import com.newrelic.agent.attributes.AgentAttributeSender;
import com.newrelic.agent.attributes.AttributeSender;
import com.newrelic.agent.bridge.PrivateApi;
import com.newrelic.agent.environment.Environment;
import com.newrelic.agent.errors.ErrorService;
import com.newrelic.agent.jmx.JmxApiImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.Logger;

public class PrivateApiImpl implements PrivateApi {
    private final AttributeSender attributeSender = new AgentAttributeSender();

    public static void initialize(Logger logger) {
        PrivateApiImpl api = new PrivateApiImpl();
        com.newrelic.agent.bridge.AgentBridge.privateApi = api;
        com.newrelic.agent.bridge.AgentBridge.asyncApi = new AsyncApiImpl(logger);
        com.newrelic.agent.bridge.AgentBridge.jmxApi = new JmxApiImpl();
        com.newrelic.agent.bridge.AgentBridge.agent = new AgentImpl(logger);
    }

    public void setAppServerPort(int port) {
        ServiceFactory.getEnvironmentService().getEnvironment().setServerPort(Integer.valueOf(port));
    }

    public void setInstanceName(String instanceName) {
        ServiceFactory.getEnvironmentService().getEnvironment().setInstanceName(instanceName);
    }

    public Closeable addSampler(Runnable sampler, int period, TimeUnit timeUnit) {
        return ServiceFactory.getSamplerService().addSampler(sampler, period, timeUnit);
    }

    public void setServerInfo(String dispatcherName, String version) {
        ServiceFactory.getEnvironmentService().getEnvironment().setServerInfo(dispatcherName, version);
    }

    public void setServerInfo(String serverInfo) {
        Environment env = ServiceFactory.getEnvironmentService().getEnvironment();
        if (!env.getAgentIdentity().isServerInfoSet()) {
            env.setServerInfo(serverInfo);
        }
    }

    public void addMBeanServer(MBeanServer server) {
        ServiceFactory.getJmxService().setJmxServer(server);
    }

    public void removeMBeanServer(MBeanServer serverToRemove) {
        ServiceFactory.getJmxService().removeJmxServer(serverToRemove);
    }

    public void addCustomAttribute(String key, String value) {
        attributeSender.addAttribute(key, value, "addCustomAttribute");
    }

    public void addCustomAttribute(String key, Number value) {
        attributeSender.addAttribute(key, value, "addCustomAttribute");
    }

    public void addTracerParameter(String key, Number value) {
        if (Transaction.getTransaction().isInProgress()) {
            Transaction.getTransaction().getTransactionActivity().getLastTracer().setAttribute(key, value);
        }
    }

    public void reportHTTPError(String message, int statusCode, String uri) {
        ErrorService.reportHTTPError(message, statusCode, uri);
    }
}