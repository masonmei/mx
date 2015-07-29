package com.newrelic.agent.profile;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.newrelic.deps.org.json.simple.JSONArray;
import com.newrelic.deps.org.json.simple.JSONStreamAware;

import com.newrelic.deps.com.google.common.collect.Maps;

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
        JSONArray.writeJSONString(Arrays.asList(new Object[] {method, Integer.valueOf(runnableCallCount),
                                                                     Integer.valueOf(nonrunnableCallCount),
                                                                     new ArrayList(children.values())}), out);
    }

    public String toString() {
        return method.toString();
    }

    public ProfiledMethod getMethod() {
        return method;
    }

    protected int getRunnableCallCount() {
        return runnableCallCount;
    }

    public void incrementRunnableCallCount() {
        runnableCallCount += 1;
    }

    public void incrementNonRunnableCallCount() {
        nonrunnableCallCount += 1;
    }

    Collection<ProfileSegment> getChildren() {
        return children.values();
    }

    Map<ProfiledMethod, ProfileSegment> getChildMap() {
        return children;
    }

    ProfileSegment addChild(ProfiledMethod method) {
        ProfileSegment result = (ProfileSegment) children.get(method);
        if (result == null) {
            result = newProfileSegment(method);
            children.put(method, result);
        }
        return result;
    }

    void removeChild(ProfiledMethod method) {
        children.remove(method);
    }

    public int getCallSiteCount() {
        int count = 1;

        for (ProfileSegment segment : children.values()) {
            count += segment.getCallSiteCount();
        }

        return count;
    }

    public int getCallCount(ProfiledMethod method) {
        int count = method.equals(getMethod()) ? runnableCallCount : 0;
        for (ProfileSegment kid : children.values()) {
            count += kid.getCallCount(method);
        }

        return count;
    }

    public Set<ProfiledMethod> getMethods() {
        Set methods = new HashSet();
        methods.add(getMethod());
        for (ProfileSegment kid : children.values()) {
            methods.addAll(kid.getMethods());
        }
        return methods;
    }
}