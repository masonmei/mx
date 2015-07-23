package com.newrelic.api.agent.weaver;

public enum MatchType {
    ExactClass(true),

    BaseClass(false),

    Interface(false);

    private final boolean exactMatch;

    private MatchType(boolean exact) {
        exactMatch = exact;
    }

    public boolean isExactMatch() {
        return exactMatch;
    }
}