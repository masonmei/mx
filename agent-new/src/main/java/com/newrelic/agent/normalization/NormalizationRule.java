package com.newrelic.agent.normalization;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class NormalizationRule {
    private static final Pattern SEGMENT_SEPARATOR_PATTERN = Pattern.compile("/");
    private static final Pattern BACKREFERENCE_PATTERN = Pattern.compile("\\\\(\\d)");
    private static final String BACKREFERENCE_REPLACEMENT = "\\$$1";
    private final Pattern pattern;
    private final boolean ignore;
    private final boolean terminateChain;
    private final int order;
    private final boolean eachSegment;
    private final boolean replaceAll;
    private final String replaceRegex;
    private final ReplacementFormatter formatter;

    public NormalizationRule(String matchExp, String replacement, boolean ignore, int order, boolean terminateChain,
                             boolean eachSegment, boolean replaceAll) throws PatternSyntaxException {
        this.ignore = ignore;
        this.order = order;
        this.terminateChain = terminateChain;
        this.eachSegment = eachSegment;
        this.replaceAll = replaceAll;
        this.pattern = Pattern.compile(matchExp, 34);

        if ((replacement == null) || (replacement.length() == 0)) {
            this.replaceRegex = null;
        } else {
            Matcher backReferenceMatcher = BACKREFERENCE_PATTERN.matcher(replacement);
            this.replaceRegex = backReferenceMatcher.replaceAll("\\$$1");
        }

        if (ignore) {
            this.formatter = new IgnoreReplacementFormatter();
        } else {
            this.formatter = new FancyReplacementFormatter();
        }
    }

    public boolean isTerminateChain() {
        return this.terminateChain;
    }

    public RuleResult normalize(String name) {
        return this.formatter.getRuleResult(name);
    }

    public boolean isIgnore() {
        return this.ignore;
    }

    public boolean isReplaceAll() {
        return this.replaceAll;
    }

    public boolean isEachSegment() {
        return this.eachSegment;
    }

    public String getReplacement() {
        return this.replaceRegex;
    }

    public int getOrder() {
        return this.order;
    }

    public String getMatchExpression() {
        return this.pattern.pattern();
    }

    public String toString() {
        return MessageFormat
                       .format("match_expression: {0} replacement: {1} eval_order: {2} each_segment: {3} ignore: {4} "
                                       + "terminate_chain: {5} replace_all: {6}", this.pattern.pattern(),
                                      this.replaceRegex, this.order, this.eachSegment, this.ignore, this.terminateChain,
                                      this.replaceAll);
    }

    private interface ReplacementFormatter {
        RuleResult getRuleResult(String paramString);
    }

    private class FancyReplacementFormatter implements ReplacementFormatter {
        private FancyReplacementFormatter() {
        }

        public RuleResult getRuleResult(String url) {
            if (NormalizationRule.this.eachSegment) {
                return forEachSegment(url);
            }
            return forEntireUrl(url);
        }

        private RuleResult forEachSegment(String url) {
            boolean isMatch = false;
            String[] segments = NormalizationRule.SEGMENT_SEPARATOR_PATTERN.split(url);
            for (int i = 1; i < segments.length; i++) {
                String segment = segments[i];
                if ((segment != null) && (segment.length() != 0)) {
                    RuleResult result = forEntireUrl(segment);
                    if (result.isMatch()) {
                        isMatch = true;
                        segments[i] = result.getReplacement();
                    }
                }
            }
            if (!isMatch) {
                return RuleResult.getNoMatch();
            }
            StringBuilder path = new StringBuilder();
            for (int i = 1; i < segments.length; i++) {
                String segment = segments[i];
                if ((segment != null) && (segment.length() != 0)) {
                    path.append('/').append(segment);
                }
            }
            return RuleResult.getMatch(path.toString());
        }

        private RuleResult forEntireUrl(String url) {
            Matcher matcher = NormalizationRule.this.pattern.matcher(url);
            if (matcher.find()) {
                String replacement;
                if ((NormalizationRule.this.replaceRegex == null) || (NormalizationRule.this.replaceRegex.length()
                                                                              == 0)) {
                    replacement = null;
                } else {
                    if (NormalizationRule.this.replaceAll) {
                        replacement = matcher.replaceAll(NormalizationRule.this.replaceRegex);
                    } else {
                        replacement = matcher.replaceFirst(NormalizationRule.this.replaceRegex);
                    }
                }
                return RuleResult.getMatch(replacement);
            }
            return RuleResult.getNoMatch();
        }
    }

    private class IgnoreReplacementFormatter implements ReplacementFormatter {
        private IgnoreReplacementFormatter() {
        }

        public RuleResult getRuleResult(String url) {
            if (NormalizationRule.this.eachSegment) {
                return forEachSegment(url);
            }
            return forEntireUrl(url);
        }

        private RuleResult forEachSegment(String url) {
            String[] segments = NormalizationRule.SEGMENT_SEPARATOR_PATTERN.split(url);
            for (int i = 1; i < segments.length; i++) {
                String segment = segments[i];
                if ((segment != null) && (segment.length() != 0)) {
                    Matcher matcher = NormalizationRule.this.pattern.matcher(segment);
                    if (matcher.find()) {
                        return RuleResult.getIgnoreMatch();
                    }
                }
            }
            return RuleResult.getNoMatch();
        }

        private RuleResult forEntireUrl(String url) {
            Matcher matcher = NormalizationRule.this.pattern.matcher(url);
            return matcher.find() ? RuleResult.getIgnoreMatch() : RuleResult.getNoMatch();
        }
    }
}