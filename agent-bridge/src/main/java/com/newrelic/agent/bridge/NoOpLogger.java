package com.newrelic.agent.bridge;

import java.util.logging.Level;

import com.newrelic.api.agent.Logger;

class NoOpLogger implements Logger {
    static final Logger INSTANCE = new NoOpLogger();

    public boolean isLoggable(Level level) {
        return false;
    }

    public void log(Level level, String pattern, Object[] parts) {
    }

    public void log(Level level, Throwable t, String pattern, Object[] msg) {
    }

    public void logToChild(String childName, Level level, String pattern, Object[] parts) {
    }
}