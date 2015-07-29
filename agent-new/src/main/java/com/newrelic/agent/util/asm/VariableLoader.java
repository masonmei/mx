package com.newrelic.agent.util.asm;

import com.newrelic.deps.org.objectweb.asm.commons.GeneratorAdapter;

public abstract interface VariableLoader {
    public abstract void load(Object paramObject, GeneratorAdapter paramGeneratorAdapter);
}