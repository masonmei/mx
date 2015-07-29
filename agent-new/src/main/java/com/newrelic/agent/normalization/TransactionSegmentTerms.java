package com.newrelic.agent.normalization;

import java.util.Set;

public class TransactionSegmentTerms {
    final String prefix;
    final Set<String> terms;

    public TransactionSegmentTerms(String prefix, Set<String> terms) {
        this.prefix = (prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix);
        this.terms = terms;
    }

    public String toString() {
        return "TransactionSegmentTerms [prefix=" + this.prefix + ", terms=" + this.terms + "]";
    }
}