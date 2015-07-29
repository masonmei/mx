package com.newrelic.agent.instrumentation;

import com.newrelic.agent.InstrumentationProxy;
import java.lang.instrument.ClassFileTransformer;

public abstract interface StartableClassFileTransformer extends ClassFileTransformer
{
  public abstract void start(InstrumentationProxy paramInstrumentationProxy, boolean paramBoolean);
}