//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.logging;

import com.newrelic.agent.config.AgentConfig;

public class AgentLogManager {
    static final String ROOT_LOGGER_NAME = "com.newrelic";
    private static final IAgentLogManager INSTANCE = createAgentLogManager();
    private static final IAgentLogger ROOT_LOGGER;

    static {
        ROOT_LOGGER = INSTANCE.getRootLogger();
    }

    private AgentLogManager() {
    }

    private static IAgentLogManager createAgentLogManager() {
        return LogbackLogManager.create(ROOT_LOGGER_NAME);
    }

    public static IAgentLogger getLogger() {
        return ROOT_LOGGER;
    }

    public static String getLogFilePath() {
        return INSTANCE.getLogFilePath();
    }

    public static void configureLogger(AgentConfig agentConfig) {
        INSTANCE.configureLogger(agentConfig);
    }

    public static void addConsoleHandler() {
        INSTANCE.addConsoleHandler();
    }

    public static String getLogLevel() {
        return INSTANCE.getLogLevel();
    }

    public static void setLogLevel(String level) {
        INSTANCE.setLogLevel(level);
    }
}
