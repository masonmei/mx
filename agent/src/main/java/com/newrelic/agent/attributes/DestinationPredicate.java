package com.newrelic.agent.attributes;

import com.google.common.base.Predicate;

public abstract interface DestinationPredicate extends Predicate<String> {
    public abstract boolean isPotentialConfigMatch(String paramString);
}