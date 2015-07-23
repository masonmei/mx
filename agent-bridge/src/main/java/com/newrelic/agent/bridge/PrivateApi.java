package com.newrelic.agent.bridge;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;

public interface PrivateApi {
    void setAppServerPort(int paramInt);

    void setInstanceName(String paramString);

    Closeable addSampler(Runnable paramRunnable, int paramInt, TimeUnit paramTimeUnit);

    void setServerInfo(String paramString1, String paramString2);

    void setServerInfo(String paramString);

    void addCustomAttribute(String paramString, Number paramNumber);

    void addCustomAttribute(String paramString1, String paramString2);

    void addTracerParameter(String paramString, Number paramNumber);

    void addMBeanServer(MBeanServer paramMBeanServer);

    void removeMBeanServer(MBeanServer paramMBeanServer);

    void reportHTTPError(String paramString1, int paramInt, String paramString2);
}