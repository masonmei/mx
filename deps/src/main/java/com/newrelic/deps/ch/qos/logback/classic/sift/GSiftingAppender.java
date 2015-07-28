package com.newrelic.deps.ch.qos.logback.classic.sift;

import java.util.Map;

import com.newrelic.deps.ch.qos.logback.classic.gaffer.ConfigurationContributor;
import com.newrelic.deps.ch.qos.logback.classic.spi.ILoggingEvent;
import com.newrelic.deps.ch.qos.logback.core.Appender;
import com.newrelic.deps.ch.qos.logback.core.AppenderBase;
import com.newrelic.deps.ch.qos.logback.core.helpers.NOPAppender;
import com.newrelic.deps.ch.qos.logback.core.sift.AppenderTracker;
import com.newrelic.deps.ch.qos.logback.core.sift.Discriminator;

public class GSiftingAppender extends AppenderBase implements ConfigurationContributor, groovy.lang.GroovyObject {
    protected com.newrelic.deps.ch.qos.logback.core.sift.AppenderTracker<ILoggingEvent> appenderTracker;

    public groovy.lang.MetaClass getMetaClass() {
        return (groovy.lang.MetaClass) null;
    }

    public void setMetaClass(groovy.lang.MetaClass mc) {
    }

    public Object invokeMethod(String method, Object arguments) {
        return null;
    }

    public Object getProperty(String property) {
        return null;
    }

    public void setProperty(String property, Object value) {
    }

    public Discriminator<ILoggingEvent> getDiscriminator() {
        return null;
    }

    public void setDiscriminator(Discriminator<ILoggingEvent> value) {
    }

    public groovy.lang.Closure getBuilderClosure() {
        return (groovy.lang.Closure) null;
    }

    public void setBuilderClosure(groovy.lang.Closure value) {
    }

    public int getNopaWarningCount() {
        return (int) 0;
    }

    public void setNopaWarningCount(int value) {
    }

    public Map<String, String> getMappings() {
        return (Map) null;
    }

    @Override()
    public void start() {
    }

    @Override()
    public void stop() {
    }

    protected long getTimestamp(ILoggingEvent event) {
        return (long) 0;
    }

    public Appender buildAppender(String value) {
        return null;
    }

    @Override()
    public void append(Object object) {
    }

    public NOPAppender<ILoggingEvent> buildNOPAppender(String discriminatingValue) {
        return null;
    }

    public void build() {
    }

    public void sift(groovy.lang.Closure clo) {
    }

    public AppenderTracker getAppenderTracker() {
        return null;
    }

    public String getDiscriminatorKey() {
        return (String) null;
    }
}
