package com.newrelic.agent.attributes;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import com.newrelic.deps.com.google.common.cache.CacheBuilder;
import com.newrelic.deps.com.google.common.cache.CacheLoader;
import com.newrelic.deps.com.google.common.cache.LoadingCache;
import com.newrelic.agent.Agent;

public class DefaultDestinationPredicate implements DestinationPredicate {
    private static final long MAX_CACHE_SIZE_BUFFER = 200L;
    private final RootConfigAttributesNode mandatoryExcludeTrie;
    private final RootConfigAttributesNode configTrie;
    private final AttributesNode defaultExcludeTrie;
    private final LoadingCache<String, Boolean> cache;
    private final String destination;

    public DefaultDestinationPredicate(String dest, Set<String> exclude, Set<String> include,
                                       Set<String> defaultExcludes, Set<String> mandatoryExclude) {
        mandatoryExcludeTrie = generateExcludeConfigTrie(dest, mandatoryExclude);
        configTrie = generateConfigTrie(dest, exclude, include);
        defaultExcludeTrie = generateDefaultTrie(dest, defaultExcludes);
        destination = dest;
        cache = CacheBuilder.newBuilder().maximumSize(200L).build(new CacheLoader<String, Boolean>() {
            public Boolean load(String key) throws Exception {
                return DefaultDestinationPredicate.this.isIncluded(key);
            }
        });
    }

    protected static AttributesNode generateDefaultTrie(String dest, Set<String> defaultExcludes) {
        AttributesNode root = new AttributesNode("*", true, dest, true);
        for (String current : defaultExcludes) {
            root.addNode(new AttributesNode(current, false, dest, true));
        }
        return root;
    }

    protected static RootConfigAttributesNode generateExcludeConfigTrie(String dest, Set<String> exclude) {
        RootConfigAttributesNode root = new RootConfigAttributesNode(dest);
        addSpecifcInOrEx(root, false, exclude, dest, true);
        return root;
    }

    protected static RootConfigAttributesNode generateConfigTrie(String dest, Set<String> exclude,
                                                                 Set<String> include) {
        RootConfigAttributesNode root = new RootConfigAttributesNode(dest);
        addSpecifcInOrEx(root, false, exclude, dest, false);
        addSpecifcInOrEx(root, true, include, dest, false);
        return root;
    }

    private static void addSpecifcInOrEx(AttributesNode root, boolean isInclude, Set<String> inOrEx, String dest,
                                         boolean isDefault) {
        for (String current : inOrEx) {
            root.addNode(new AttributesNode(current, isInclude, dest, isDefault));
        }
    }

    private Boolean isIncluded(String key) {
        Boolean output = mandatoryExcludeTrie.applyRules(key);
        if (output == null) {
            output = configTrie.applyRules(key);
        }

        if (output == null) {
            output = defaultExcludeTrie.applyRules(key);
        }
        return output;
    }

    public boolean apply(String key) {
        try {
            return changeToPrimitiveAndLog(key, (Boolean) cache.get(key));
        } catch (ExecutionException e) {
        }
        return changeToPrimitiveAndLog(key, isIncluded(key));
    }

    private void logOutput(String key, boolean value) {
        if (Agent.LOG.isFineEnabled()) {
            Agent.LOG.log(Level.FINER, "{0}: Attribute {1} is {2}",
                                 new Object[] {destination, key, value ? "enabled" : "disabled"});
        }
    }

    private boolean changeToPrimitiveAndLog(String key, Boolean value) {
        boolean out = value == null ? true : value.booleanValue();
        logOutput(key, out);
        return out;
    }

    public boolean isPotentialConfigMatch(String key) {
        List queue = new LinkedList();
        queue.addAll(configTrie.getChildren());

        while (!queue.isEmpty()) {
            AttributesNode node = (AttributesNode) queue.remove(0);
            queue.addAll(node.getChildren());
            if ((node.isIncludeDestination()) && (node.mightMatch(key))) {
                return true;
            }
        }
        return false;
    }
}