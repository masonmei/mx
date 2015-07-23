package com.newrelic.agent.service;

import com.newrelic.agent.logging.IAgentLogger;

public interface Service {
    String getName();

    void start() throws Exception;

    void stop() throws Exception;

    boolean isEnabled();

    IAgentLogger getLogger();

    boolean isStarted();

    boolean isStopped();

    boolean isStartedOrStarting();

    boolean isStoppedOrStopping();
}