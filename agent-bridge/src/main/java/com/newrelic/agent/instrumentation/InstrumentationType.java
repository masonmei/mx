package com.newrelic.agent.instrumentation;

public enum InstrumentationType {
    RemoteCustomXml,

    LocalCustomXml,

    CustomYaml,

    Pointcut,

    TracedWeaveInstrumentation,

    WeaveInstrumentation,

    TraceAnnotation,

    BuiltIn,

    Unknown;
}