package com.newrelic.agent.profile;

import com.newrelic.deps.org.json.simple.JSONStreamAware;

public interface IProfile extends JSONStreamAware {
    void start();

    void end();

    void beforeSampling();

    void addStackTrace(long paramLong, boolean paramBoolean, ThreadType paramThreadType,
                       StackTraceElement[] paramArrayOfStackTraceElement);

    ProfilerParameters getProfilerParameters();

    int getSampleCount();

    Long getProfileId();

    ProfileTree getProfileTree(ThreadType paramThreadType);

    int trimBy(int paramInt);

    long getStartTimeMillis();

    long getEndTimeMillis();

    void markInstrumentedMethods();
}