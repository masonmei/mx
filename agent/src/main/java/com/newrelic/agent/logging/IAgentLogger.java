package com.newrelic.agent.logging;

import java.util.logging.Level;

import com.newrelic.api.agent.Logger;

public interface IAgentLogger extends Logger {
    void severe(String paramString);

    void error(String paramString);

    void warning(String paramString);

    void info(String paramString);

    void config(String paramString);

    void fine(String paramString);

    void finer(String paramString);

    void finest(String paramString);

    void debug(String paramString);

    void trace(String paramString);

    boolean isFineEnabled();

    boolean isFinerEnabled();

    boolean isFinestEnabled();

    boolean isDebugEnabled();

    boolean isTraceEnabled();

    void log(Level paramLevel, String paramString, Throwable paramThrowable);

    void log(Level paramLevel, String paramString);

    void log(Level paramLevel, String paramString, Object[] paramArrayOfObject, Throwable paramThrowable);

    IAgentLogger getChildLogger(Class<?> paramClass);

    IAgentLogger getChildLogger(String paramString);
}