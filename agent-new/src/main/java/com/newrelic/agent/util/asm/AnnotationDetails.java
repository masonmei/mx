package com.newrelic.agent.util.asm;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

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
        super(327680, av);
        this.desc = desc;
    }

    public List<Object> getValues(String name) {
        if (this.attributes == null) {
            return Collections.emptyList();
        }
        return this.attributes.get(name);
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
        if (this.attributes == null) {
            this.attributes = Multimaps.newListMultimap(Maps.<String, Collection<Object>>newHashMap(),
                                                               new Supplier<List<Object>>() {
                                                                   public List<Object> get() {
                                                                       return Lists.newArrayList();
                                                                   }
                                                               });
        }
        return this.attributes;
    }

    public boolean equals(Object obj) {
        if ((obj instanceof AnnotationDetails)) {
            AnnotationDetails other = (AnnotationDetails) obj;
            if (!this.desc.equals(other.desc)) {
                return false;
            }
            if (((this.attributes == null) || (other.attributes == null)) && (this.attributes != other.attributes)) {
                return false;
            }

            for (Entry<String, Object> entry : attributes.entries()) {
                List<Object> list = other.attributes.get(entry.getKey());
                if (!list.contains(entry.getValue())) {
                    return false;
                }
            }

            return true;
        }
        return super.equals(obj);
    }

    public String toString() {
        return "AnnotationDetails [desc=" + this.desc + ", attributes=" + this.attributes + "]";
    }
}