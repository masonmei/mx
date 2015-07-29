package com.newrelic.agent.attributes;

import com.newrelic.deps.com.google.common.base.Predicate;

public interface DestinationPredicate extends Predicate<String> {
    boolean isPotentialConfigMatch(String paramString);
}