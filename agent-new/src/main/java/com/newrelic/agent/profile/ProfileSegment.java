package com.newrelic.agent.profile;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.deps.org.json.simple.JSONArray;
import com.newrelic.deps.org.json.simple.JSONStreamAware;

public class ProfileSegment implements JSONStreamAware {
    private final ProfiledMethod method;
    private final Map<ProfiledMethod, ProfileSegment> children = Maps.newIdentityHashMap();
    private int runnableCallCount = 0;
    private int nonrunnableCallCount = 0;

    private ProfileSegment(ProfiledMethod method) {
        this.method = method;
    }

    public static ProfileSegment newProfileSegment(ProfiledMethod method) {
        if (method == null) {
            return null;
        }

        return new ProfileSegment(method);
    }

    public void writeJSONString(Writer out) throws IOException {
        JSONArray.writeJSONString(Arrays.asList(new Object[] {this.method, Integer.valueOf(this.runnableCallCount),
                                                                     Integer.valueOf(this.nonrunnableCallCount),
                                                                     new ArrayList(this.children.values())}), out);
    }

    public String toString() {
        return this.method.toString();
    }

    public ProfiledMethod getMethod() {
        return this.method;
    }

    protected int getRunnableCallCount() {
        return this.runnableCallCount;
    }

    public void incrementRunnableCallCount() {
        this.runnableCallCount += 1;
    }

    public void incrementNonRunnableCallCount() {
        this.nonrunnableCallCount += 1;
    }

    Collection<ProfileSegment> getChildren() {
        return this.children.values();
    }

    Map<ProfiledMethod, ProfileSegment> getChildMap() {
        return this.children;
    }

    ProfileSegment addChild(ProfiledMethod method) {
        ProfileSegment result = (ProfileSegment) this.children.get(method);
        if (result == null) {
            result = newProfileSegment(method);
            this.children.put(method, result);
        }
        return result;
    }

    void removeChild(ProfiledMethod method) {
        this.children.remove(method);
    }

    public int getCallSiteCount() {
        int count = 1;

        for (ProfileSegment segment : this.children.values()) {
            count += segment.getCallSiteCount();
        }

        return count;
    }

    public int getCallCount(ProfiledMethod method) {
        int count = method.equals(getMethod()) ? this.runnableCallCount : 0;
        for (ProfileSegment kid : this.children.values()) {
            count += kid.getCallCount(method);
        }

        return count;
    }

    public Set<ProfiledMethod> getMethods() {
        Set methods = new HashSet();
        methods.add(getMethod());
        for (ProfileSegment kid : this.children.values()) {
            methods.addAll(kid.getMethods());
        }
        return methods;
    }
}