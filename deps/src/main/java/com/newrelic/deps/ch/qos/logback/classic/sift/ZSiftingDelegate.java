package com.newrelic.deps.ch.qos.logback.classic.sift;

import com.newrelic.deps.ch.qos.logback.core.Appender;
import com.newrelic.deps.ch.qos.logback.core.spi.ContextAwareBase;

public class ZSiftingDelegate extends ContextAwareBase implements groovy.lang.GroovyObject {
    public ZSiftingDelegate(String key, String value) {
    }

    public Appender appender(String name, Class clazz) {
        return null;
    }

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

    public String getKey() {
        return (String) null;
    }

    public void setKey(String value) {
    }

    public String getValue() {
        return (String) null;
    }

    public void setValue(String value) {
    }

    public Appender appender(String name, Class clazz, groovy.lang.Closure closure) {
        return null;
    }
}
