package com.newrelic.agent.jmx;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanFeatureInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import com.newrelic.agent.Agent;

abstract class BaseMBean extends StandardMBean {
    private final ResourceBundle resourceBundle;

    protected BaseMBean(Class<?> mbeanInterface) throws NotCompliantMBeanException {
        super(mbeanInterface);

        resourceBundle = ResourceBundle.getBundle(getClass().getName());
    }

    protected String getResourceString(String key) {
        try {
            return resourceBundle.getString(key);
        } catch (MissingResourceException ex) {
            String msg = MessageFormat.format("Resource file {0} error: {1}",
                                                     new Object[] {getClass().getName() + ".properties",
                                                                          ex.toString()});

            Agent.LOG.finest(msg);
            throw ex;
        }
    }

    protected String getDescription(MBeanInfo info) {
        return getResourceString("description");
    }

    protected String getDescription(MBeanAttributeInfo info) {
        try {
            return getResourceString("attribute." + info.getName() + ".description");
        } catch (Exception ex) {
        }
        return super.getDescription(info);
    }

    protected String getDescription(MBeanFeatureInfo info) {
        try {
            return getResourceString("feature." + info.getName() + ".description");
        } catch (Exception ex) {
        }
        return super.getDescription(info);
    }

    protected String getDescription(MBeanOperationInfo op, MBeanParameterInfo param, int sequence) {
        try {
            return getResourceString("operation." + op.getName() + '.' + param.getName() + '.' + sequence
                                             + ".description");
        } catch (Exception ex) {
        }
        return super.getDescription(op, param, sequence);
    }

    protected String getDescription(MBeanOperationInfo info) {
        try {
            return getResourceString("operation." + info.getName() + ".description");
        } catch (Exception ex) {
        }
        return super.getDescription(info);
    }
}