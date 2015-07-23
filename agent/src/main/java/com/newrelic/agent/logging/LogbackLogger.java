package com.newrelic.agent.logging;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.newrelic.agent.Agent;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.rolling.TriggeringPolicy;

class LogbackLogger implements IAgentLogger {
    private static final boolean PRUDENT_VALUE = false;
    private static final int MIN_FILE_COUNT = 1;
    private static final String CONSOLE_APPENDER_NAME = "Console";
    private static final String FILE_APPENDER_NAME = "File";
    private static final boolean APPEND_TO_FILE = true;
    private static final String CONVERSION_PATTERN = "%d{\"MMM d, yyyy HH:mm:ss ZZZZ\"} [%pid %i] %logger %ml: %m%n";
    private static final String SYSTEM_OUT = "System.out";
    private final Logger logger;
    private Map<String, IAgentLogger> childLoggers = Maps.newConcurrentMap();

    private LogbackLogger(String name, boolean isAgentRoot) {
        logger = ((Logger) LoggerFactory.getLogger(name));

        if (isAgentRoot) {
            logger.setAdditive(false);
            FineFilter.getFineFilter().start();
        }
    }

    public static LogbackLogger create(String name, boolean isAgentRoot) {
        return new LogbackLogger(name, isAgentRoot);
    }

    public void severe(String pMessage) {
        logger.error(pMessage);
    }

    public void error(String pMessage) {
        logger.error(pMessage);
    }

    public void warning(String pMessage) {
        logger.warn(pMessage);
    }

    public void info(String pMessage) {
        logger.info(pMessage);
    }

    public void config(String pMessage) {
        logger.info(pMessage);
    }

    public void fine(String pMessage) {
        logger.debug(LogbackMarkers.FINE_MARKER, pMessage);
    }

    public void finer(String pMessage) {
        logger.debug(LogbackMarkers.FINER_MARKER, pMessage);
    }

    public void finest(String pMessage) {
        logger.trace(LogbackMarkers.FINEST_MARKER, pMessage);
    }

    public void debug(String pMessage) {
        logger.debug(pMessage);
    }

    public void trace(String pMessage) {
        logger.trace(pMessage);
    }

    public boolean isFineEnabled() {
        return (logger.isDebugEnabled()) && (FineFilter.getFineFilter().isEnabledFor(java.util.logging.Level.FINE));
    }

    public boolean isFinerEnabled() {
        return (logger.isDebugEnabled()) && (FineFilter.getFineFilter().isEnabledFor(java.util.logging.Level.FINER));
    }

    public boolean isFinestEnabled() {
        return logger.isTraceEnabled();
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    public boolean isLoggable(java.util.logging.Level pLevel) {
        LogbackLevel level = LogbackLevel.getLevel(pLevel);
        return level != null;
    }

    public void log(java.util.logging.Level pLevel, final String pMessage, final Throwable pThrowable) {
        if (isLoggable(pLevel)) {
            final LogbackLevel level = LogbackLevel.getLevel(pLevel);

            AccessController.doPrivileged(new PrivilegedAction() {
                public Void run() {
                    logger.log(level.getMarker(), Logger.FQCN,
                                      level.getLogbackLevel().toLocationAwareLoggerInteger(level.getLogbackLevel()),
                                      pMessage, null, pThrowable);

                    return null;
                }
            });
        }
    }

    public void log(java.util.logging.Level pLevel, String pMessage) {
        LogbackLevel level = LogbackLevel.getLevel(pLevel);
        logger.log(level.getMarker(), Logger.FQCN,
                          level.getLogbackLevel().toLocationAwareLoggerInteger(level.getLogbackLevel()), pMessage, null,
                          null);
    }

    public void log(java.util.logging.Level pLevel, String pMessage, Object[] pArgs, Throwable pThorwable) {
        LogbackLevel level = LogbackLevel.getLevel(pLevel);
        logger.log(level.getMarker(), Logger.FQCN,
                          level.getLogbackLevel().toLocationAwareLoggerInteger(level.getLogbackLevel()), pMessage,
                          pArgs, pThorwable);
    }

    public IAgentLogger getChildLogger(Class<?> pClazz) {
        return getChildLogger(pClazz.getName());
    }

    public IAgentLogger getChildLogger(String pFullName) {
        IAgentLogger logger = create(pFullName, false);

        childLoggers.put(pFullName, logger);
        return logger;
    }

    public String getLevel() {
        if (logger.getLevel() == ch.qos.logback.classic.Level.DEBUG) {
            return FineFilter.getFineFilter().getLevel().toString();
        }
        return logger.getLevel().toString();
    }

    public void setLevel(String level) {
        LogbackLevel newLevel = LogbackLevel.getLevel(level, LogbackLevel.INFO);
        logger.setLevel(newLevel.getLogbackLevel());
        FineFilter.getFineFilter().setLevel(newLevel.getJavaLevel());
    }

    public void removeConsoleAppender() {
        logger.detachAppender("Console");
    }

    public void addConsoleAppender() {
        if (logger.getAppender("Console") != null) {
            return;
        }
        ConsoleAppender consoleAppender = new ConsoleAppender();
        consoleAppender.setName("Console");
        consoleAppender.setTarget("System.out");
        consoleAppender.setEncoder(getEncoder(logger.getLoggerContext()));
        consoleAppender.setContext(logger.getLoggerContext());
        consoleAppender.addFilter(FineFilter.getFineFilter());
        consoleAppender.start();
        logger.addAppender(consoleAppender);
    }

    public void addFileAppender(String fileName, long logLimit, int fileCount, boolean isDaily) throws IOException {
        if (logger.getAppender("File") != null) {
            return;
        }

        FileAppender fileAppender = createFileAppender(fileCount, logLimit, fileName, isDaily);
        fileAppender.addFilter(FineFilter.getFineFilter());
        fileAppender.setEncoder(getEncoder(logger.getLoggerContext()));
        fileAppender.start();
        logger.addAppender(fileAppender);
    }

    private FileAppender<ILoggingEvent> createDailyAppender(int fileCount, String fileName) {
        RollingFileAppender fileAppender = new RollingFileAppender();

        fileAppender.setContext(logger.getLoggerContext());
        fileAppender.setName("File");
        fileAppender.setFile(fileName);
        fileAppender.setAppend(true);
        fileAppender.setPrudent(false);

        TimeBasedRollingPolicy timePolicy = new TimeBasedRollingPolicy();
        timePolicy.setFileNamePattern(fileName + ".%d{yyyy-MM-dd}");
        timePolicy.setContext(logger.getLoggerContext());
        timePolicy.setMaxHistory(fileCount);
        timePolicy.setParent(fileAppender);
        fileAppender.setRollingPolicy(timePolicy);
        timePolicy.start();

        return fileAppender;
    }

    private FileAppender<ILoggingEvent> createFileAppender(int fileCount, long logLimit, String fileName,
                                                           boolean isDaily) {
        if (isDaily) {
            return createDailyAppender(fileCount, fileName);
        }

        if (fileCount <= 1) {
            FileAppender fileAppender = new FileAppender();
            fileAppender.setName("File");
            fileAppender.setFile(fileName);
            fileAppender.setAppend(true);
            fileAppender.setPrudent(false);
            fileAppender.setContext(logger.getLoggerContext());
            return fileAppender;
        }

        RollingFileAppender fileAppender = new RollingFileAppender();

        fileAppender.setContext(logger.getLoggerContext());
        fileAppender.setName("File");
        fileAppender.setFile(fileName);
        fileAppender.setAppend(true);
        fileAppender.setPrudent(false);

        FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
        rollingPolicy.setContext(logger.getLoggerContext());
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setMinIndex(1);
        rollingPolicy.setMaxIndex(fileCount - 1);
        rollingPolicy.setFileNamePattern(fileName + ".%i");
        fileAppender.setRollingPolicy(rollingPolicy);

        TriggeringPolicy triggerPolicy = new SizeBasedTriggeringPolicy(String.valueOf(logLimit));

        fileAppender.setTriggeringPolicy(triggerPolicy);

        triggerPolicy.start();
        rollingPolicy.start();

        return fileAppender;
    }

    private Encoder<ILoggingEvent> getEncoder(Context context) {
        CustomPatternLogbackEncoder encoder =
                new CustomPatternLogbackEncoder("%d{\"MMM d, yyyy HH:mm:ss ZZZZ\"} [%pid %i] %logger %ml: %m%n");
        encoder.setContext(context);
        encoder.start();
        return encoder;
    }

    public void log(java.util.logging.Level level, String pattern, Object[] parts) {
        if (isLoggable(level)) {
            log(level, getMessage(pattern, parts));
        }
    }

    public void log(java.util.logging.Level level, Throwable t, String pattern, Object[] parts) {
        log(level, getMessage(pattern, parts), t);
    }

    private String getMessage(String pattern, Object[] parts) {
        return parts == null ? pattern : MessageFormat.format(pattern, formatValues(parts));
    }

    private Object[] formatValues(Object[] parts) {
        Object[] strings = new Object[parts.length];
        for (int i = 0; i < parts.length; i++) {
            strings[i] = formatValue(parts[i]);
        }
        return strings;
    }

    private Object formatValue(Object obj) {
        if ((obj instanceof Class)) {
            return ((Class) obj).getName();
        }
        if ((obj instanceof Throwable)) {
            return obj.toString();
        }
        return obj;
    }

    public void logToChild(String childName, java.util.logging.Level level, String pattern, Object[] parts) {
        if (isLoggable(level)) {
            IAgentLogger logger = (IAgentLogger) childLoggers.get(childName);
            if (logger == null) {
                logger = Agent.LOG;
            }
            logger.log(level, pattern, parts);
        }
    }
}