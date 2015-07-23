package com.newrelic.agent.logging;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.PatternLayoutEncoderBase;

class CustomPatternLogbackEncoder extends PatternLayoutEncoderBase<ILoggingEvent> {
    public CustomPatternLogbackEncoder(String pPattern) {
        setPattern(pPattern);
    }

    public void start() {
        PatternLayout patternLayout = new CustomPatternLogbackLayout(getPattern());
        patternLayout.setContext(context);
        patternLayout.setOutputPatternAsHeader(outputPatternAsHeader);
        patternLayout.start();
        layout = patternLayout;
        super.start();
    }
}