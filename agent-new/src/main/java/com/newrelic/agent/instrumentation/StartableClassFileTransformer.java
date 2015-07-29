package com.newrelic.agent.instrumentation;

import java.lang.instrument.ClassFileTransformer;

import com.newrelic.agent.InstrumentationProxy;

public abstract interface StartableClassFileTransformer extends ClassFileTransformer {
    public abstract void start(InstrumentationProxy paramInstrumentationProxy, boolean paramBoolean);
}