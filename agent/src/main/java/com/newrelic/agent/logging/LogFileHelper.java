package com.newrelic.agent.logging;

import java.io.File;
import java.text.MessageFormat;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigFileHelper;

class LogFileHelper {
    private static final String NEW_RELIC_LOG_FILE = "newrelic.logfile";
    private static final String LOGS_DIRECTORY = "logs";

    public static File getLogFile(AgentConfig agentConfig) {
        if (agentConfig.isLoggingToStdOut()) {
            return null;
        }
        File f = getLogFileFromProperty();
        if (f != null) {
            return f;
        }
        return getLogFileFromConfig(agentConfig);
    }

    private static File getLogFileFromProperty() {
        String logFileName = System.getProperty("newrelic.logfile");
        if (logFileName == null) {
            return null;
        }
        File f = new File(logFileName);
        try {
            f.createNewFile();
            return f;
        } catch (Exception e) {
            String msg = MessageFormat
                                 .format("Unable to create log file {0}. Check permissions on the directory. - {1}",
                                                new Object[] {logFileName, e});

            Agent.LOG.warning(msg);
        }
        return null;
    }

    private static File getLogFileFromConfig(AgentConfig agentConfig) {
        String logFileName = agentConfig.getLogFileName();
        File logsDirectory = getLogsDirectory(agentConfig);
        return new File(logsDirectory, logFileName);
    }

    private static File getLogsDirectory(AgentConfig agentConfig) {
        File f = getLogsDirectoryFromConfig(agentConfig);
        if (f != null) {
            return f;
        }

        f = getNewRelicLogsDirectory();
        if (f != null) {
            return f;
        }

        f = new File("logs");
        if (f.exists()) {
            return f;
        }

        return new File(".");
    }

    private static File getLogsDirectoryFromConfig(AgentConfig agentConfig) {
        String logFilePath = agentConfig.getLogFilePath();
        if (logFilePath == null) {
            return null;
        }
        File f = new File(logFilePath);
        if (f.exists()) {
            return f;
        }
        String msg = MessageFormat.format("The log_file_path {0} specified in newrelic.yml does not exist",
                                                 new Object[] {logFilePath});

        Agent.LOG.config(msg);

        return null;
    }

    private static File getNewRelicLogsDirectory() {
        File nrDir = ConfigFileHelper.getNewRelicDirectory();
        if (nrDir != null) {
            File logs = new File(nrDir, "logs");
            logs.mkdir();
            return logs;
        }
        return null;
    }
}