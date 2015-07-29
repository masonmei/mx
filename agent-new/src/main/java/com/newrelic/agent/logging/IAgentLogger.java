package com.newrelic.agent.logging;

import java.util.logging.Level;

import com.newrelic.api.agent.Logger;

public abstract interface IAgentLogger extends Logger {
    public abstract void severe(String paramString);

    public abstract void error(String paramString);

    public abstract void warning(String paramString);

    public abstract void info(String paramString);

    public abstract void config(String paramString);

    public abstract void fine(String paramString);

    public abstract void finer(String paramString);

    public abstract void finest(String paramString);

    public abstract void debug(String paramString);

    public abstract void trace(String paramString);

    public abstract boolean isFineEnabled();

    public abstract boolean isFinerEnabled();

    public abstract boolean isFinestEnabled();

    public abstract boolean isDebugEnabled();

    public abstract boolean isTraceEnabled();

    public abstract void log(Level paramLevel, String paramString, Throwable paramThrowable);

    public abstract void log(Level paramLevel, String paramString);

    public abstract void log(Level paramLevel, String paramString, Object[] paramArrayOfObject,
                             Throwable paramThrowable);

    public abstract IAgentLogger getChildLogger(Class<?> paramClass);

    public abstract IAgentLogger getChildLogger(String paramString);
}