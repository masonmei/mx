package com.newrelic.agent.normalization;

import java.util.List;

public abstract interface Normalizer {
    public abstract String normalize(String paramString);

    public abstract List<NormalizationRule> getRules();
}