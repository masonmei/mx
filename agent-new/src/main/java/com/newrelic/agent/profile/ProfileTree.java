package com.newrelic.agent.profile;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.deps.org.json.simple.JSONArray;
import com.newrelic.deps.org.json.simple.JSONStreamAware;

public class ProfileTree implements JSONStreamAware {
    private final Map<ProfiledMethod, ProfileSegment> rootSegments = Maps.newIdentityHashMap();

    private final Map<StackTraceElement, ProfiledMethod> profiledMethods = Maps.newHashMap();
    private long cpuTime;

    private ProfileSegment add(StackTraceElement stackTraceElement, ProfileSegment parent, boolean runnable) {
        ProfiledMethod method = (ProfiledMethod) this.profiledMethods.get(stackTraceElement);
        if (method == null) {
            method = ProfiledMethod.newProfiledMethod(stackTraceElement);
            if (method != null) {
                this.profiledMethods.put(stackTraceElement, method);
            }
        }

        if (method == null) {
            return parent;
        }

        return add(method, parent, runnable);
    }

    private ProfileSegment add(ProfiledMethod method, ProfileSegment parent, boolean runnable) {
        ProfileSegment result = add(method, parent);
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
            result = this.rootSegments.get(method);
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
        }

        int count = 0;
        for (ProfileSegment segment : this.rootSegments.values()) {
            count += segment.getCallCount(method);
        }
        return count;
    }

    public int getCallSiteCount() {
        int count = 0;
        for (ProfileSegment segment : this.rootSegments.values()) {
            count += segment.getCallSiteCount();
        }
        return count;
    }

    public Collection<ProfileSegment> getRootSegments() {
        return this.rootSegments.values();
    }

    public int getRootCount() {
        return getRootSegments().size();
    }

    public int getMethodCount() {
        Set methodNames = new HashSet();
        for (ProfileSegment segment : this.rootSegments.values()) {
            methodNames.addAll(segment.getMethods());
        }
        return methodNames.size();
    }

    public void addStackTrace(List<StackTraceElement> stackTraceList, boolean runnable) {
        ProfileSegment parent = null;
        for (StackTraceElement methodCall : stackTraceList) {
            parent = add(methodCall, parent, runnable);
        }
    }

    public void writeJSONString(Writer out) throws IOException {
        Collection rootSegments = getRootSegments();
        ArrayList list = Lists.newArrayListWithCapacity(rootSegments.size() + 1);
        list.add(getExtraData());
        list.addAll(rootSegments);
        JSONArray.writeJSONString(list, out);
    }

    private Map<String, Object> getExtraData() {
        Map data = new HashMap();

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
        for (ProfiledMethod method : this.profiledMethods.values()) {
            method.setMethodDetails(classMap);
        }
    }
}