package com.newrelic.agent.normalization;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class NormalizerFactory {
    public static Normalizer createUrlNormalizer(String appName, List<NormalizationRule> urlRules) {
        return new UrlNormalizer(new NormalizerImpl(appName, urlRules));
    }

    public static Normalizer createTransactionNormalizer(String appName, List<NormalizationRule> transactionNameRules,
                                                         List<TransactionSegmentTerms> transactionSegmentTermRules) {
        Normalizer normalizer = new NormalizerImpl(appName, transactionNameRules);

        if (!transactionSegmentTermRules.isEmpty()) {
            normalizer = compoundNormalizer(new Normalizer[] {normalizer,
                                                                     createTransactionSegmentNormalizer
                                                                             (transactionSegmentTermRules)});
        }

        return normalizer;
    }

    private static Normalizer compoundNormalizer(final Normalizer[] normalizers) {
        final List rules = Lists.newArrayList();
        for (Normalizer n : normalizers) {
            rules.addAll(n.getRules());
        }
        return new Normalizer() {
            public String normalize(String name) {
                for (Normalizer n : normalizers) {
                    name = n.normalize(name);

                    if (name == null) {
                        return name;
                    }
                }
                return name;
            }

            public List<NormalizationRule> getRules() {
                return rules;
            }
        };
    }

    static Normalizer createTransactionSegmentNormalizer(final List<TransactionSegmentTerms>
                                                                 transactionSegmentTermRules) {
        return new Normalizer() {
            public String normalize(String name) {
                for (TransactionSegmentTerms terms : transactionSegmentTermRules) {
                    if (name.startsWith(terms.prefix)) {
                        String afterPrefix = name.substring(terms.prefix.length() + 1);
                        String[] segments = afterPrefix.split("/");
                        List keep = Lists.newArrayListWithCapacity(segments.length + 1);

                        keep.add(terms.prefix);

                        boolean discarded = false;
                        for (String segment : segments) {
                            if (terms.terms.contains(segment)) {
                                keep.add(segment);
                                discarded = false;
                            } else if (!discarded) {
                                keep.add("*");
                                discarded = true;
                            }
                        }

                        name = Joiner.on('/').join(keep);
                    }
                }

                return name;
            }

            public List<NormalizationRule> getRules() {
                return Collections.emptyList();
            }
        };
    }

    public static Normalizer createMetricNormalizer(String appName, List<NormalizationRule> metricNameRules) {
        return new NormalizerImpl(appName, metricNameRules);
    }

    private static class UrlNormalizer implements Normalizer {
        private final Normalizer normalizer;

        private UrlNormalizer(Normalizer normalizer) {
            this.normalizer = normalizer;
        }

        public String normalize(String name) {
            if (name == null) {
                return null;
            }
            String normalizedName = name;
            if (!normalizedName.startsWith("/")) {
                normalizedName = "/" + name;
            }
            return normalizer.normalize(normalizedName);
        }

        public List<NormalizationRule> getRules() {
            return normalizer.getRules();
        }
    }
}