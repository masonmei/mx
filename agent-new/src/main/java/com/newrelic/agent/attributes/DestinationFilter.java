//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.attributes;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.deps.com.google.common.collect.Sets;

public class DestinationFilter {
    private final boolean isEnabled;
    private final DestinationPredicate filter;

    public DestinationFilter(String mainNameForFilter, boolean defaultInclude, AgentConfig config,
                             List<String> confixExcludes, List<String> configIncludes, boolean captureParams,
                             boolean captureMessageParams, String[] defaultExclude, String... namesForIsEnabled) {
        this.isEnabled = AttributesConfigUtil.isAttsEnabled(config, defaultInclude, namesForIsEnabled);
        Agent.LOG.log(Level.FINE, "Attributes are {0} for {1}", this.isEnabled ? "enabled" : "disabled",
                             mainNameForFilter);
        this.filter = getDestinationPredicate(this.isEnabled, config, confixExcludes, configIncludes, mainNameForFilter,
                                                     updateDefaults(captureParams, captureMessageParams,
                                                                           defaultExclude));
    }

    private static DestinationPredicate getDestinationPredicate(boolean isEnabled, AgentConfig config,
                                                                List<String> rootExcludes, List<String> rootIncludes,
                                                                String name, Set<String> defaultExclude) {
        if (isEnabled) {
            Set<String> configExclude = AttributesConfigUtil.getExcluded(config, rootExcludes, name);
            Set<String> configInclude = AttributesConfigUtil.getIncluded(config, rootIncludes, name);
            return new DefaultDestinationPredicate(name, configExclude, configInclude, defaultExclude,
                                                          getMandatoryExcludes(config.isHighSecurity()));
        } else {
            return new DisabledDestinationPredicate();
        }
    }

    private static Set<String> getMandatoryExcludes(boolean highSecurity) {
        return  (highSecurity ? Sets.newHashSet("request.parameters.*", "message.parameters.*")
                              : Collections.<String>emptySet());
    }

    private static Set<String> updateDefaults(boolean captureParams, boolean captureMessageParams,
                                              String[] defaultExclude) {
        HashSet<String> defaultExc = Sets.newHashSet(defaultExclude);
        if (!captureParams) {
            defaultExc.add("request.parameters.*");
        }

        if (!captureMessageParams) {
            defaultExc.add("message.parameters.*");
        }

        return defaultExc;
    }

    protected boolean isPotentialConfigMatch(String paramStart) {
        return this.filter.isPotentialConfigMatch(paramStart);
    }

    protected boolean isEnabled() {
        return this.isEnabled;
    }

    protected <T> Map<String, T> filterAttributes(Map<String, T> values) {
        return this.filterAttributes(values, this.filter);
    }

    private <T> Map<String, T> filterAttributes(Map<String, T> values, DestinationPredicate predicate) {
        return this.isEnabled && values != null && !values.isEmpty() ? Maps.filterKeys(values, predicate)
                       : Collections.<String, T>emptyMap();
    }
}
