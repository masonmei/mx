package com.newrelic.agent.bridge;

public interface ObjectFieldManager {
    void initializeFields(String paramString, Object paramObject1, Object paramObject2);

    Object getFieldContainer(String paramString, Object paramObject);

    void createClassObjectFields(String paramString);
}