package com.newrelic.agent.attributes;

public class DisabledDestinationPredicate implements DestinationPredicate {
    public boolean apply(String input) {
        return false;
    }

    public boolean isPotentialConfigMatch(String key) {
        return false;
    }
}