package com.newrelic.agent.attributes;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import com.newrelic.agent.Agent;

public class AttributesNode {
    private static final String END_WILDCARD = "*";
    private final String original;
    private final String name;
    private final boolean hasEndWildcard;
    private final Set<AttributesNode> children;
    private final String destination;
    private final boolean isDefaultRule;
    private AttributesNode parent;
    private boolean includeDestination;

    public AttributesNode(String pOriginal, boolean isIncluded, String dest, boolean isDefault) {
        original = pOriginal;
        if (original.endsWith("*")) {
            name = original.substring(0, original.length() - 1);
            hasEndWildcard = true;
        } else {
            name = pOriginal;
            hasEndWildcard = false;
        }
        includeDestination = isIncluded;
        destination = dest;
        isDefaultRule = isDefault;
        children = new HashSet();
        parent = null;
    }

    protected Boolean applyRules(String key) {
        Boolean result = null;
        if (matches(key)) {
            logMatch(key);
            result = Boolean.valueOf(includeDestination);

            for (AttributesNode current : children) {
                Boolean tmp = current.applyRules(key);
                if (tmp != null) {
                    result = tmp;
                    break;
                }
            }
        }
        return result;
    }

    private void logMatch(String key) {
        if (Agent.LOG.isFinerEnabled()) {
            Agent.LOG.log(Level.FINEST, "{0}: Attribute key \"{1}\" matched {2} {3} rule \"{4}\"",
                                 new Object[] {destination, key, isDefaultRule ? "default" : "config",
                                                      includeDestination ? "INCLUDE" : "EXCLUDE", original});
        }
    }

    public boolean addNode(AttributesNode rule) {
        if (rule != null) {
            if (isSameString(rule)) {
                mergeIncludeExcludes(rule);
                return true;
            }
            if (isInputBefore(rule)) {
                addNodeBeforeMe(rule);
                return true;
            }
            if (isInputAfter(rule)) {
                for (AttributesNode current : children) {
                    if (current.addNode(rule)) {
                        return true;
                    }
                }

                addNodeToMe(rule);
                return true;
            }
        }
        return false;
    }

    protected boolean matches(String key) {
        return (key != null) && (hasEndWildcard ? key.startsWith(name) : name.equals(key));
    }

    protected boolean mightMatch(String key) {
        return (key != null) && ((key.startsWith(name)) || (name.startsWith(key)));
    }

    protected boolean isIncludeDestination() {
        return includeDestination;
    }

    private boolean isSameString(AttributesNode rule) {
        return original.equals(rule.original);
    }

    private boolean isInputBefore(AttributesNode rule) {
        return (rule.hasEndWildcard) && (name.startsWith(rule.name));
    }

    private boolean isInputAfter(AttributesNode rule) {
        return (hasEndWildcard) && (rule.name.startsWith(name));
    }

    private void addNodeBeforeMe(AttributesNode rule) {
        AttributesNode rulesParent = parent;
        moveChildrenToRuleAsNeeded(parent, rule);
        rulesParent.addNodeToMe(rule);
    }

    private void moveChildrenToRuleAsNeeded(AttributesNode parent, AttributesNode rule) {
        Iterator it = parent.children.iterator();

        while (it.hasNext()) {
            AttributesNode ar = (AttributesNode) it.next();
            if (ar.isInputBefore(rule)) {
                ar.parent = rule;

                it.remove();

                rule.children.add(ar);
            }
        }
    }

    protected void addNodeToMe(AttributesNode rule) {
        rule.parent = this;
        children.add(rule);
    }

    protected boolean mergeIncludeExcludes(AttributesNode rule) {
        includeDestination = ((includeDestination) && (rule.includeDestination));
        return includeDestination;
    }

    public void printTrie() {
        StringBuilder sb = new StringBuilder("Root: ").append(original).append("\n");

        Queue q = new LinkedBlockingQueue();
        AttributesNode ar = this;
        while (ar != null) {
            sb.append("Parent: ");
            if (ar.parent != null) {
                sb.append(ar.parent.original);
            } else {
                sb.append("null");
            }
            sb.append(" This: ").append(ar.original).append(" Children: ");
            if (children != null) {
                for (AttributesNode c : ar.children) {
                    sb.append(" ").append(c.original);
                    q.add(c);
                }
            }
            ar = (AttributesNode) q.poll();
            sb.append("\n");
        }
        System.out.println(sb.toString());
    }

    protected AttributesNode getParent() {
        return parent;
    }

    protected Set<AttributesNode> getChildren() {
        return children;
    }
}