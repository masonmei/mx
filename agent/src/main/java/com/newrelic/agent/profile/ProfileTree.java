//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.profile;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.newrelic.deps.org.json.simple.JSONArray;
import com.newrelic.deps.org.json.simple.JSONStreamAware;

import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.deps.com.google.common.collect.Maps;

public class ProfileTree implements JSONStreamAware {
    private final Map<ProfiledMethod, ProfileSegment> rootSegments = Maps.newIdentityHashMap();
    private final Map<StackTraceElement, ProfiledMethod> profiledMethods = Maps.newHashMap();
    private long cpuTime;

    public ProfileTree() {
    }

    private ProfileSegment add(StackTraceElement stackTraceElement, ProfileSegment parent, boolean runnable) {
        ProfiledMethod method = (ProfiledMethod) this.profiledMethods.get(stackTraceElement);
        if (method == null) {
            method = ProfiledMethod.newProfiledMethod(stackTraceElement);
            if (method != null) {
                this.profiledMethods.put(stackTraceElement, method);
            }
        }

        return method == null ? parent : this.add(method, parent, runnable);
    }

    private ProfileSegment add(ProfiledMethod method, ProfileSegment parent, boolean runnable) {
        ProfileSegment result = this.add(method, parent);
        if (runnable) {
            result.incrementRunnableCallCount();
        } else {
            result.incrementNonRunnableCallCount();
        }

        return result;
    }

    private ProfileSegment add(ProfiledMethod method, ProfileSegment parent) {
        ProfileSegment result;
        if (parent == null) {
            result = (ProfileSegment) this.rootSegments.get(method);
            if (result == null) {
                result = ProfileSegment.newProfileSegment(method);
                this.rootSegments.put(method, result);
            }
        } else {
            result = parent.addChild(method);
        }

        return result;
    }

    public int getCallCount(StackTraceElement stackElement) {
        ProfiledMethod method = ProfiledMethod.newProfiledMethod(stackElement);
        if (method == null) {
            return 0;
        } else {
            int count = 0;

            ProfileSegment segment;
            for (Iterator i$ = this.rootSegments.values().iterator(); i$.hasNext();
                 count += segment.getCallCount(method)) {
                segment = (ProfileSegment) i$.next();
            }

            return count;
        }
    }

    public int getCallSiteCount() {
        int count = 0;

        ProfileSegment segment;
        for (Iterator i$ = this.rootSegments.values().iterator(); i$.hasNext(); count += segment.getCallSiteCount()) {
            segment = (ProfileSegment) i$.next();
        }

        return count;
    }

    public Collection<ProfileSegment> getRootSegments() {
        return this.rootSegments.values();
    }

    public int getRootCount() {
        return this.getRootSegments().size();
    }

    public int getMethodCount() {
        HashSet methodNames = new HashSet();
        Iterator i$ = this.rootSegments.values().iterator();

        while (i$.hasNext()) {
            ProfileSegment segment = (ProfileSegment) i$.next();
            methodNames.addAll(segment.getMethods());
        }

        return methodNames.size();
    }

    public void addStackTrace(List<StackTraceElement> stackTraceList, boolean runnable) {
        ProfileSegment parent = null;

        StackTraceElement methodCall;
        for (Iterator i$ = stackTraceList.iterator(); i$.hasNext(); parent = this.add(methodCall, parent, runnable)) {
            methodCall = (StackTraceElement) i$.next();
        }

    }

    public void writeJSONString(Writer out) throws IOException {
        Collection rootSegments = this.getRootSegments();
        ArrayList list = Lists.newArrayListWithCapacity(rootSegments.size() + 1);
        list.add(this.getExtraData());
        list.addAll(rootSegments);
        JSONArray.writeJSONString(list, out);
    }

    private Map<String, Object> getExtraData() {
        HashMap data = new HashMap();
        data.put("cpu_time", Long.valueOf(this.cpuTime));
        return data;
    }

    public void incrementCpuTime(long cpuTime) {
        this.cpuTime += cpuTime;
    }

    public long getCpuTime() {
        return this.cpuTime;
    }

    public void setMethodDetails(Map<String, Class<?>> classMap) {
        Iterator i$ = this.profiledMethods.values().iterator();

        while (i$.hasNext()) {
            ProfiledMethod method = (ProfiledMethod) i$.next();
            method.setMethodDetails(classMap);
        }

    }
}
