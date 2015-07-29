package com.newrelic.agent.extension;

import java.util.Map;
import java.util.Set;

import com.newrelic.deps.org.objectweb.asm.commons.Remapper;

import com.newrelic.deps.com.google.common.collect.ImmutableSet;
import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.deps.com.google.common.collect.Sets;

public class DependencyRemapper extends Remapper {
    static final String DEPENDENCY_PREFIX = "com/newrelic/deps/";
    private final Set<String> prefixes;
    private final Map<String, String> oldToNew = Maps.newHashMap();

    public DependencyRemapper(Set<String> prefixes) {
        this.prefixes = fix(prefixes);
    }

    private static Set<String> fix(Set<String> prefixes) {
        Set<String> fixed = Sets.newHashSet();
        for (String prefix : prefixes) {
            if (prefix.startsWith(DEPENDENCY_PREFIX)) {
                fixed.add(prefix.substring(DEPENDENCY_PREFIX.length()));
            } else {
                fixed.add(prefix);
            }
        }
        return ImmutableSet.copyOf(fixed);
    }

    public String map(String typeName) {
        for (String prefix : prefixes) {
            if (typeName.startsWith(prefix)) {
                String newType = DEPENDENCY_PREFIX + typeName;
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