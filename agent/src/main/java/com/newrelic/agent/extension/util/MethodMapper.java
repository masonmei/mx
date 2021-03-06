package com.newrelic.agent.extension.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodMapper {
    private final Map<String, List<String>> methods = new HashMap();

    public void clear() {
        methods.clear();
    }

    public void addMethod(String name, List<String> descriptors) {
        List descs = (List) methods.get(name);
        if (descs == null) {
            descs = new ArrayList(descriptors);
            methods.put(name, descs);
        } else {
            descs.addAll(descriptors);
        }
    }

    public boolean addIfNotPresent(String name, String descriptor) {
        List descs = (List) methods.get(name);
        if (descs == null) {
            descs = new ArrayList();
            methods.put(name, descs);
        }

        if (!descs.contains(descriptor)) {
            descs.add(descriptor);
            return true;
        }

        return false;
    }
}