package com.newrelic.agent.logging;

import ch.qos.logback.classic.PatternLayout;

class CustomPatternLogbackLayout extends PatternLayout {
    private static final String THREAD_ID_CHAR = "i";
    private static final String MARKER_LEVEL_ID = "ml";
    private static final String PROCESS_ID = "pid";

    public CustomPatternLogbackLayout(String pPattern) {
        defaultConverterMap.put("i", ThreadIdLogbackConverter.class.getName());
        defaultConverterMap.put("ml", MarkerLevelConverter.class.getName());
        defaultConverterMap.put("pid", ProcessIdLogbackConverter.class.getName());

        setPattern(pPattern);
    }
}