package com.newrelic.agent.logging;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentJarHelper;
import com.newrelic.deps.ch.qos.logback.classic.Level;

class LogbackLogManager implements IAgentLogManager {
    private static final String CONFIG_FILE_PROP = "logback.configurationFile";
    private static final String CONTEXT_SELECT_PROP = "logback.ContextSelector";
    private static final String STATUS_LIST_PROP = "logback.statusListenerClass";
    private final LogbackLogger rootLogger;
    private volatile String logFilePath;

    private LogbackLogManager(String name) {
        rootLogger = initializeRootLogger(name);
    }

    public static LogbackLogManager create(String name) {
        return new LogbackLogManager(name);
    }

    private LogbackLogger createRootLogger(String name) {
        LogbackLogger logger = LogbackLogger.create(name, true);
        String logLevel = getStartupLogLevel();
        logger.setLevel(logLevel);
        logger.addConsoleAppender();
        return logger;
    }

    private String getStartupLogLevel() {
        String propName = "newrelic.config.startup_log_level";
        String logLevel = System.getProperty(propName);
        if (logLevel == null) {
            return Level.INFO.levelStr.toLowerCase();
        }
        return logLevel.toLowerCase();
    }

    private LogbackLogger initializeRootLogger(String name) {
        LogbackLogger logger = null;
        Map systemProps = new HashMap();
        try {
            String jarFileName = AgentJarHelper.getAgentJarFileName();
            if (jarFileName == null) {
                logger = LogbackLogger.create(name, true);
            } else {
                clearAllLogbackSystemProperties(systemProps);
                System.setProperty("logback.configurationFile", jarFileName);
                try {
                    logger = createRootLogger(name);
                } finally {
                    System.getProperties().remove("logback.configurationFile");
                    applyOriginalSystemProperties(systemProps, logger);
                }
            }
        } catch (Exception e) {
            if (logger == null) {
                logger = createRootLogger(name);
            }
            String msg =
                    MessageFormat.format("Error setting logback.configurationFile property: {0}", new Object[] {e});
            logger.warning(msg);
        }
        return logger;
    }

    private void clearAllLogbackSystemProperties(Map<String, String> storedSystemProps) {
        clearLogbackSystemProperty("logback.configurationFile", storedSystemProps);
        clearLogbackSystemProperty("logback.ContextSelector", storedSystemProps);
        clearLogbackSystemProperty("logback.statusListenerClass", storedSystemProps);
    }

    private void clearLogbackSystemProperty(String prop, Map<String, String> storedSystemProps) {
        String old = System.clearProperty(prop);
        if (old != null) {
            storedSystemProps.put(prop, old);
        }
    }

    private void applyOriginalSystemProperties(Map<String, String> storedSystemProps, LogbackLogger logger) {
        for (Entry currentProp : storedSystemProps.entrySet()) {
            try {
                System.setProperty((String) currentProp.getKey(), (String) currentProp.getValue());
            } catch (Exception e) {
                String msg = MessageFormat.format("Error setting logback property {0} back to {1}. Error: {2}",
                                                         new Object[] {currentProp.getKey(), currentProp.getValue(),
                                                                              e});

                logger.warning(msg);
            }
        }
    }

    public IAgentLogger getRootLogger() {
        return rootLogger;
    }

    public String getLogFilePath() {
        return logFilePath;
    }

    public void configureLogger(AgentConfig pAgentConfig) {
        configureLogLevel(pAgentConfig);
        configureConsoleHandler(pAgentConfig);
        configureFileHandler(pAgentConfig);
    }

    private void configureFileHandler(AgentConfig agentConfig) {
        String logFileName = getLogFileName(agentConfig);
        if (logFileName == null) {
            return;
        }
        try {
            configureFileHandler(logFileName, agentConfig);
            logFilePath = logFileName;
            String msg = MessageFormat.format("Writing to New Relic log file: {0}", new Object[] {logFileName});
            rootLogger.info(msg);
            rootLogger.info(MessageFormat.format("JRE vendor {0} version {1}",
                                                        new Object[] {System.getProperty("java.vendor"),
                                                                             System.getProperty("java.version")}));

            rootLogger.info(MessageFormat.format("JVM vendor {0} {1} version {2}",
                                                        new Object[] {System.getProperty("java.vm.vendor"),
                                                                             System.getProperty("java.vm.name"),
                                                                             System.getProperty("java.vm.version")}));

            rootLogger.fine(MessageFormat.format("JVM runtime version {0}",
                                                        new Object[] {System.getProperty("java.runtime.version")}));
            rootLogger.info(MessageFormat.format("OS {0} version {1} arch {2}",
                                                        new Object[] {System.getProperty("os.name"),
                                                                             System.getProperty("os.version"),
                                                                             System.getProperty("os.arch")}));
        } catch (IOException e) {
            String msg = MessageFormat.format("Unable to configure newrelic log file: {0}", new Object[] {logFileName});
            rootLogger.error(msg);
            addConsoleHandler();
        }
    }

    private String getLogFileName(AgentConfig agentConfig) {
        File logFile = LogFileHelper.getLogFile(agentConfig);
        return logFile == null ? null : logFile.getPath();
    }

    private void configureLogLevel(AgentConfig agentConfig) {
        if (agentConfig.isDebugEnabled()) {
            rootLogger.setLevel(Level.TRACE.levelStr.toLowerCase());
        } else {
            rootLogger.setLevel(agentConfig.getLogLevel());
        }
    }

    private void configureConsoleHandler(AgentConfig agentConfig) {
        if ((agentConfig.isDebugEnabled()) || (agentConfig.isLoggingToStdOut())) {
            addConsoleHandler();
        } else {
            rootLogger.removeConsoleAppender();
        }
    }

    private String configureFileHandler(String logFileName, AgentConfig agentConfig) throws IOException {
        rootLogger.addConsoleAppender();

        if (canWriteLogFile(logFileName)) {
            rootLogger.info(MessageFormat
                                    .format("New Relic Agent: Writing to log file: {0}", new Object[] {logFileName}));
        } else {
            rootLogger.warning(MessageFormat.format("New Relic Agent: Unable to write log file: {0}. Please check "
                                                            + "permissions on the file and directory.",
                                                           new Object[] {logFileName}));
        }

        rootLogger.removeConsoleAppender();

        int limit = agentConfig.getLogLimit() * 1024;
        int fileCount = Math.max(1, agentConfig.getLogFileCount());
        boolean isDaily = agentConfig.isLogDaily();
        rootLogger.addFileAppender(logFileName, limit, fileCount, isDaily);
        return logFileName;
    }

    private boolean canWriteLogFile(String logFileName) {
        try {
            File logFile = new File(logFileName);
            if (!logFile.exists()) {
                if (null != logFile.getParentFile()) {
                    logFile.getParentFile().mkdirs();
                }
                logFile.createNewFile();
            }
            return logFile.canWrite();
        } catch (Exception e) {
        }
        return false;
    }

    public void addConsoleHandler() {
        rootLogger.addConsoleAppender();
    }

    public String getLogLevel() {
        return rootLogger.getLevel();
    }

    public void setLogLevel(String pLevel) {
        rootLogger.setLevel(pLevel);
    }
}