package com.newrelic.agent.jmx.create;

import java.text.MessageFormat;
import java.util.logging.Level;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.newrelic.agent.Agent;

public class JmxObject {
    private final String objectNameString;
    private final ObjectName objectName;

    public JmxObject(String pObjectName, String safeName) throws MalformedObjectNameException {
        objectNameString = pObjectName;
        objectName = setObjectName(safeName);
    }

    private ObjectName setObjectName(String safeName) throws MalformedObjectNameException {
        try {
            return new ObjectName(safeName);
        } catch (MalformedObjectNameException e) {
            if (!objectNameString.equals(safeName)) {
                safeName = safeName + '(' + objectNameString + ')';
            }
            if (Agent.LOG.isFineEnabled()) {
                Agent.LOG.severe(MessageFormat.format("Skipping bad Jmx object name : {0}.  {1}",
                                                             new Object[] {safeName, e.toString()}));

                Agent.LOG.log(Level.FINER, "Jmx config error", e);
            }
            throw e;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("object_name: ").append(objectNameString);
        return sb.toString();
    }

    public String getObjectNameString() {
        return objectNameString;
    }

    public ObjectName getObjectName() {
        return objectName;
    }
}