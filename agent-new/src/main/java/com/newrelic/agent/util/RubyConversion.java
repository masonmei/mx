package com.newrelic.agent.util;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.newrelic.agent.Agent;

public class RubyConversion {
    private static final Map<String, String> rubyToJavaClassMap = new HashMap() {
        private static final long serialVersionUID = -2335806597139433736L;
    };

    public static Class<Exception> rubyClassToJavaClass(String fullClassName) throws ClassNotFoundException {
        String className = (String) rubyToJavaClassMap.get(fullClassName);
        if (className != null) {
            return (Class<Exception>) Class.forName(className);
        }
        try {
            Vector<String> typeParts = new Vector<String>(Arrays.asList(fullClassName.split("::")));
            if (typeParts.size() < 1) {
                throw new ClassNotFoundException(MessageFormat.format("Unable to load class {0}",
                                                                             new Object[] {fullClassName}));
            }
            className = (String) typeParts.lastElement();
            typeParts.remove(className);
            StringBuilder packageName = new StringBuilder();
            for (String typePart : typeParts) {
                if ("NewRelic".equals(typePart)) {
                    typePart = "com.newrelic";
                }
                packageName.append(typePart).append('.');
            }
            className = packageName.toString().toLowerCase() + className;
            return (Class<Exception>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            Agent.LOG.severe(MessageFormat.format("Unable to deserialize class {0}", new Object[] {fullClassName}));
            throw e;
        }
    }
}