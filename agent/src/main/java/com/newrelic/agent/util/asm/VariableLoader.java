package com.newrelic.agent.util.asm;

import com.newrelic.deps.org.objectweb.asm.commons.GeneratorAdapter;

public interface VariableLoader {
    void load(Object paramObject, GeneratorAdapter paramGeneratorAdapter);
}