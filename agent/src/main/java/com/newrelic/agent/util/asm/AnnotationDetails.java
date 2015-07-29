package com.newrelic.agent.util.asm;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.newrelic.agent.Agent;
import com.newrelic.deps.com.google.common.base.Supplier;
import com.newrelic.deps.com.google.common.collect.ListMultimap;
import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.deps.com.google.common.collect.Multimap;
import com.newrelic.deps.com.google.common.collect.Multimaps;
import com.newrelic.deps.org.objectweb.asm.AnnotationVisitor;

public class AnnotationDetails extends AnnotationVisitor {
    final String desc;
    private ListMultimap<String, Object> attributes;

    public AnnotationDetails(AnnotationVisitor av, String desc) {
        super(Agent.ASM_LEVEL, av);
        this.desc = desc;
    }

    public List<Object> getValues(String name) {
        if (attributes == null) {
            return Collections.emptyList();
        }
        return attributes.get(name);
    }

    public Object getValue(String name) {
        Collection values = getValues(name);
        if (values.isEmpty()) {
            return null;
        }
        return values.iterator().next();
    }

    public void visit(String name, Object value) {
        getOrCreateAttributes().put(name, value);
        super.visit(name, value);
    }

    Multimap<String, Object> getOrCreateAttributes() {
        if (attributes == null) {
            attributes = Multimaps.newListMultimap(Maps.<Object, Collection<Object>>newHashMap(), new Supplier() {
                public List<Object> get() {
                    return Lists.newArrayList();
                }
            });
        }
        return attributes;
    }

    public boolean equals(Object obj) {
        if ((obj instanceof AnnotationDetails)) {
            AnnotationDetails other = (AnnotationDetails) obj;
            if (!desc.equals(other.desc)) {
                return false;
            }
            if (((attributes == null) || (other.attributes == null)) && (attributes != other.attributes)) {
                return false;
            }

            for (Map.Entry<String, Object> entry : attributes.entries()) {
                List list = other.attributes.get(entry.getKey());
                if (!list.contains(entry.getValue())) {
                    return false;
                }
            }

            return true;
        }
        return super.equals(obj);
    }

    public String toString() {
        return "AnnotationDetails [desc=" + desc + ", attributes=" + attributes + "]";
    }
}