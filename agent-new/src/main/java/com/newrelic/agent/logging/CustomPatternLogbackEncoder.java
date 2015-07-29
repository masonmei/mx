package com.newrelic.agent.logging;

import com.newrelic.deps.ch.qos.logback.classic.PatternLayout;
import com.newrelic.deps.ch.qos.logback.classic.spi.ILoggingEvent;
import com.newrelic.deps.ch.qos.logback.core.pattern.PatternLayoutEncoderBase;

class CustomPatternLogbackEncoder extends PatternLayoutEncoderBase<ILoggingEvent> {
    public CustomPatternLogbackEncoder(String pPattern) {
        setPattern(pPattern);
    }

    public void start() {
        PatternLayout patternLayout = new CustomPatternLogbackLayout(getPattern());
        patternLayout.setContext(this.context);
        patternLayout.setOutputPatternAsHeader(this.outputPatternAsHeader);
        patternLayout.start();
        this.layout = patternLayout;
        super.start();
    }
}