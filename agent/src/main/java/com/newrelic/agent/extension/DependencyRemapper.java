package com.newrelic.agent.extension;

import java.util.Map;
import java.util.Set;

import org.objectweb.asm.commons.Remapper;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class DependencyRemapper extends Remapper {
    static final String DEPENDENCY_PREFIX = "com/newrelic/agent/deps/";
    private final Set<String> prefixes;
    private final Map<String, String> oldToNew = Maps.newHashMap();

    public DependencyRemapper(Set<String> prefixes) {
        this.prefixes = fix(prefixes);
    }

    private static Set<String> fix(Set<String> prefixes) {
        Set fixed = Sets.newHashSet();
        for (String prefix : prefixes) {
            if (prefix.startsWith("com/newrelic/agent/deps/")) {
                fixed.add(prefix.substring("com/newrelic/agent/deps/".length()));
            } else {
                fixed.add(prefix);
            }
        }
        return ImmutableSet.copyOf(fixed);
    }

    public String map(String typeName) {
        for (String prefix : prefixes) {
            if (typeName.startsWith(prefix)) {
                String newType = "com/newrelic/agent/deps/" + typeName;
                oldToNew.put(typeName, newType);
                return newType;
            }
        }
        return super.map(typeName);
    }

    public Map<String, String> getRemappings() {
        return oldToNew;
    }

    Set<String> getPrefixes() {
        return prefixes;
    }
}