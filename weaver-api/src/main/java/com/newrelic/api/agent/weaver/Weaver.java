package com.newrelic.api.agent.weaver;

public final class Weaver {
    private Weaver() {
    }

    public static final <T> T callOriginal() {
        return null;
    }
}
