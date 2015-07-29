//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.context;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import com.newrelic.deps.org.objectweb.asm.commons.Method;

import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.deps.com.google.common.collect.Sets;
import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.agent.instrumentation.tracing.TraceDetailsBuilder;

public class TraceInformation {
    private Map<Method, TraceDetails> traces;
    private Set<Method> ignoreApdexMethods;
    private Set<Method> ignoreTransactionMethods;

    public TraceInformation() {
    }

    public Map<Method, TraceDetails> getTraceAnnotations() {
        return this.traces == null ? Collections.<Method, TraceDetails>emptyMap()
                       : Collections.unmodifiableMap(this.traces);
    }

    void pullAll(Map<Method, TraceDetails> tracedMethods) {
        if (this.traces == null) {
            this.traces = Maps.newHashMap(tracedMethods);
        } else {
            for (Entry<Method, TraceDetails> entry : tracedMethods.entrySet()) {
                this.putTraceAnnotation(entry.getKey(), entry.getValue());
            }
        }

    }

    void putTraceAnnotation(Method method, TraceDetails trace) {
        if (this.traces == null) {
            this.traces = Maps.newHashMap();
        } else {
            TraceDetails existing = this.traces.get(method);
            if (existing != null) {
                Agent.LOG
                        .log(Level.FINEST, "Merging trace details {0} and {1} for method {2}", existing, trace, method);
                trace = TraceDetailsBuilder.merge(existing, trace);
            }
        }

        this.traces.put(method, trace);
    }

    public Set<Method> getIgnoreApdexMethods() {
        return this.ignoreApdexMethods == null ? Collections.<Method>emptySet() : this.ignoreApdexMethods;
    }

    public Set<Method> getIgnoreTransactionMethods() {
        return this.ignoreTransactionMethods == null ? Collections.<Method>emptySet() : this.ignoreTransactionMethods;
    }

    public void addIgnoreApdexMethod(String methodName, String methodDesc) {
        if (this.ignoreApdexMethods == null) {
            this.ignoreApdexMethods = Sets.newHashSet();
        }

        this.ignoreApdexMethods.add(new Method(methodName, methodDesc));
    }

    public void addIgnoreTransactionMethod(String methodName, String methodDesc) {
        if (this.ignoreTransactionMethods == null) {
            this.ignoreTransactionMethods = Sets.newHashSet();
        }

        this.ignoreTransactionMethods.add(new Method(methodName, methodDesc));
    }

    public void addIgnoreTransactionMethod(Method m) {
        if (this.ignoreTransactionMethods == null) {
            this.ignoreTransactionMethods = Sets.newHashSet();
        }

        this.ignoreTransactionMethods.add(m);
    }

    public boolean isMatch() {
        return !this.getTraceAnnotations().isEmpty() || !this.getIgnoreApdexMethods().isEmpty()
                       || !this.getIgnoreTransactionMethods().isEmpty();
    }
}
