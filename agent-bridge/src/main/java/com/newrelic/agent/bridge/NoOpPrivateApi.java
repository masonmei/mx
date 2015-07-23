package com.newrelic.agent.bridge;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;

public class NoOpPrivateApi implements PrivateApi {
    public void setAppServerPort(int port) {
    }

    public void setInstanceName(String instanceName) {
    }

    public Closeable addSampler(Runnable sampler, int period, TimeUnit minutes) {
        return null;
    }

    public void setServerInfo(String serverInfo) {
    }

    public void setServerInfo(String dispatcherName, String version) {
    }

    public void addCustomAttribute(String key, Number value) {
    }

    public void addCustomAttribute(String key, String value) {
    }

    public void addMBeanServer(MBeanServer server) {
    }

    public void removeMBeanServer(MBeanServer serverToRemove) {
    }

    public void addTracerParameter(String key, Number value) {
    }

    public void reportHTTPError(String message, int statusCode, String uri) {
    }
}