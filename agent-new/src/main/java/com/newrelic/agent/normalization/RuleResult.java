package com.newrelic.agent.normalization;

public class RuleResult {
    public static final RuleResult NO_MATCH_RULE_RESULT = new RuleResult(false, false, null);
    public static final RuleResult IGNORE_MATCH_RULE_RESULT = new RuleResult(true, true, null);
    private final boolean isIgnore;
    private final boolean isMatch;
    private final String replacement;

    private RuleResult(boolean isIgnore, boolean isMatch, String replacement) {
        this.isIgnore = isIgnore;
        this.isMatch = isMatch;
        this.replacement = replacement;
    }

    public static RuleResult getIgnoreMatch() {
        return IGNORE_MATCH_RULE_RESULT;
    }

    public static RuleResult getNoMatch() {
        return NO_MATCH_RULE_RESULT;
    }

    public static RuleResult getMatch(String replacement) {
        return new RuleResult(false, true, replacement);
    }

    public boolean isIgnore() {
        return this.isIgnore;
    }

    public boolean isMatch() {
        return this.isMatch;
    }

    public String getReplacement() {
        return this.replacement;
    }
}