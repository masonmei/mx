package com.newrelic.agent.bridge;

class NoOpObjectFieldManager implements ObjectFieldManager {
    public void initializeFields(String className, Object target, Object fieldContainer) {
    }

    public Object getFieldContainer(String className, Object target) {
        return null;
    }

    public void createClassObjectFields(String className) {
    }
}