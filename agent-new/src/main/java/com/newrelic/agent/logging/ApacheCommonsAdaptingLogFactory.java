//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.logging;

import java.util.logging.Level;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogConfigurationException;
import org.apache.commons.logging.LogFactory;

import com.newrelic.agent.Agent;

public class ApacheCommonsAdaptingLogFactory extends LogFactory {
    public static final IAgentLogger LOG = AgentLogManager.getLogger();

    public ApacheCommonsAdaptingLogFactory() {
    }

    public Object getAttribute(String name) {
        return null;
    }

    public String[] getAttributeNames() {
        return new String[0];
    }

    public Log getInstance(Class clazz) throws LogConfigurationException {
        return new ApacheCommonsAdaptingLogFactory.LogAdapter(clazz, Agent.LOG);
    }

    public Log getInstance(String name) throws LogConfigurationException {
        return new ApacheCommonsAdaptingLogFactory.LogAdapter(name, Agent.LOG);
    }

    public void release() {
    }

    public void removeAttribute(String name) {
    }

    public void setAttribute(String name, Object value) {
    }

    private class LogAdapter implements Log {
        private final IAgentLogger logger;

        public LogAdapter(Class<?> clazz, IAgentLogger logger) {
            this.logger = logger.getChildLogger(clazz.getClass());
        }

        public LogAdapter(String name, IAgentLogger logger) {
            this.logger = logger.getChildLogger(name);
        }

        public boolean isDebugEnabled() {
            return Agent.isDebugEnabled() && this.logger.isDebugEnabled();
        }

        public boolean isErrorEnabled() {
            return this.isDebugEnabled() && this.logger.isLoggable(Level.SEVERE);
        }

        public boolean isFatalEnabled() {
            return this.isDebugEnabled() && this.logger.isLoggable(Level.SEVERE);
        }

        public boolean isInfoEnabled() {
            return this.isDebugEnabled() && this.logger.isLoggable(Level.INFO);
        }

        public boolean isTraceEnabled() {
            return this.isDebugEnabled() && this.logger.isLoggable(Level.FINEST);
        }

        public boolean isWarnEnabled() {
            return this.isDebugEnabled() && this.logger.isLoggable(Level.WARNING);
        }

        public void trace(Object message) {
            if (this.isDebugEnabled()) {
                this.logger.trace(message.toString());
            }

        }

        public void trace(Object message, Throwable t) {
            if (this.isDebugEnabled()) {
                this.logger.log(Level.FINEST, t, message.toString(), new Object[0]);
            }

        }

        public void debug(Object message) {
            if (this.isDebugEnabled()) {
                this.logger.debug(message.toString());
            }

        }

        public void debug(Object message, Throwable t) {
            if (this.isDebugEnabled()) {
                this.logger.log(Level.FINEST, "{0} : {1}", new Object[] {message, t});
            }

        }

        public void info(Object message) {
            if (this.isDebugEnabled()) {
                this.logger.info(message.toString());
            }

        }

        public void info(Object message, Throwable t) {
            if (this.isDebugEnabled()) {
                this.logger.log(Level.INFO, "{0} : {1}", new Object[] {message, t});
            }

        }

        public void warn(Object message) {
            if (this.isDebugEnabled()) {
                this.logger.warning(message.toString());
            }

        }

        public void warn(Object message, Throwable t) {
            if (this.isDebugEnabled()) {
                this.logger.log(Level.WARNING, "{0} : {1}", new Object[] {message, t});
            }

        }

        public void error(Object message) {
            if (this.isDebugEnabled()) {
                this.logger.error(message.toString());
            }

        }

        public void error(Object message, Throwable t) {
            if (this.isDebugEnabled()) {
                this.logger.log(Level.SEVERE, "{0} : {1}", new Object[] {message, t});
            }

        }

        public void fatal(Object message) {
            if (this.isDebugEnabled()) {
                this.logger.severe(message.toString());
            }

        }

        public void fatal(Object message, Throwable t) {
            if (this.isDebugEnabled()) {
                this.logger.log(Level.SEVERE, "{0} : {1}", new Object[] {message, t});
            }

        }
    }
}
