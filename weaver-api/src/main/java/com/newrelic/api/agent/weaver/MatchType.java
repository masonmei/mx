//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.api.agent.weaver;

public enum MatchType {
    ExactClass(true),
    BaseClass(false),
    Interface(false);

    private final boolean exactMatch;

    private MatchType(boolean exact) {
        this.exactMatch = exact;
    }

    public boolean isExactMatch() {
        return this.exactMatch;
    }
}
