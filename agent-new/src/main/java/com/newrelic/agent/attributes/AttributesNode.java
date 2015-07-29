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
        this.original = pOriginal;
        if (this.original.endsWith("*")) {
            this.name = this.original.substring(0, this.original.length() - 1);
            this.hasEndWildcard = true;
        } else {
            this.name = pOriginal;
            this.hasEndWildcard = false;
        }
        this.includeDestination = isIncluded;
        this.destination = dest;
        this.isDefaultRule = isDefault;
        this.children = new HashSet();
        this.parent = null;
    }

    protected Boolean applyRules(String key) {
        Boolean result = null;
        if (matches(key)) {
            logMatch(key);
            result = Boolean.valueOf(this.includeDestination);

            for (AttributesNode current : this.children) {
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
                                 new Object[] {this.destination, key, this.isDefaultRule ? "default" : "config",
                                                      this.includeDestination ? "INCLUDE" : "EXCLUDE", this.original});
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
                for (AttributesNode current : this.children) {
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
        return (key != null) && (this.hasEndWildcard ? key.startsWith(this.name) : this.name.equals(key));
    }

    protected boolean mightMatch(String key) {
        return (key != null) && ((key.startsWith(this.name)) || (this.name.startsWith(key)));
    }

    protected boolean isIncludeDestination() {
        return this.includeDestination;
    }

    private boolean isSameString(AttributesNode rule) {
        return this.original.equals(rule.original);
    }

    private boolean isInputBefore(AttributesNode rule) {
        return (rule.hasEndWildcard) && (this.name.startsWith(rule.name));
    }

    private boolean isInputAfter(AttributesNode rule) {
        return (this.hasEndWildcard) && (rule.name.startsWith(this.name));
    }

    private void addNodeBeforeMe(AttributesNode rule) {
        AttributesNode rulesParent = this.parent;
        moveChildrenToRuleAsNeeded(this.parent, rule);
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
        this.children.add(rule);
    }

    protected boolean mergeIncludeExcludes(AttributesNode rule) {
        this.includeDestination = ((this.includeDestination) && (rule.includeDestination));
        return this.includeDestination;
    }

    public void printTrie() {
        StringBuilder sb = new StringBuilder("Root: ").append(this.original).append("\n");

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
            if (this.children != null) {
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
        return this.parent;
    }

    protected Set<AttributesNode> getChildren() {
        return this.children;
    }
}