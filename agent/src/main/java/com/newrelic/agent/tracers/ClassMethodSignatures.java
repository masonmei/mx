package com.newrelic.agent.tracers;

import com.newrelic.agent.util.InsertOnlyArray;

public class ClassMethodSignatures {
    private static final ClassMethodSignatures INSTANCE = new ClassMethodSignatures();
    private final InsertOnlyArray<ClassMethodSignature> signatures;

    ClassMethodSignatures() {
        this(1000);
    }

    ClassMethodSignatures(int capacity) {
        signatures = new InsertOnlyArray(capacity);
    }

    public static ClassMethodSignatures get() {
        return INSTANCE;
    }

    public ClassMethodSignature get(int index) {
        return (ClassMethodSignature) signatures.get(index);
    }

    public int add(ClassMethodSignature signature) {
        return signatures.add(signature);
    }

    public int getIndex(ClassMethodSignature signature) {
        return signatures.getIndex(signature);
    }
}