//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.normalization;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.transport.DataSenderWriter;
import com.newrelic.deps.com.google.common.collect.ImmutableSet;
import com.newrelic.deps.com.google.common.collect.Lists;

public class NormalizationRuleFactory {
    public static final String URL_RULES_KEY = "url_rules";
    public static final String METRIC_NAME_RULES_KEY = "metric_name_rules";
    public static final String TRANSACTION_NAME_RULES_KEY = "transaction_name_rules";
    private static final String TRANSACTION_SEGMENT_TERMS_KEY = "transaction_segment_terms";
    private static final List<Map<String, Object>> EMPTY_RULES_DATA = Collections.emptyList();
    private static final List<NormalizationRule> EMPTY_RULES = Collections.emptyList();

    public NormalizationRuleFactory() {
    }

    public static List<NormalizationRule> getUrlRules(String appName, Map<String, Object> data) {
        try {
            List e = getUrlRulesData(appName, data);
            List msg2 = createRules(appName, e);
            if (msg2.isEmpty()) {
                Agent.LOG.warning("The agent did not receive any url rules from the New Relic server.");
            } else {
                String msg1 = MessageFormat.format("Received {0} url rule(s) for {1}",
                                                          new Object[] {Integer.valueOf(msg2.size()), appName});
                Agent.LOG.fine(msg1);
            }

            return msg2;
        } catch (Exception var5) {
            String msg = MessageFormat.format("An error occurred getting url rules for {0} from the New Relic server."
                                                      + " This can indicate a problem with the agent rules supplied "
                                                      + "by the New Relic server.: {1}", new Object[] {appName, var5});
            if (Agent.LOG.isLoggable(Level.FINER)) {
                Agent.LOG.log(Level.FINER, msg, var5);
            } else {
                Agent.LOG.log(Level.INFO, msg);
            }

            return EMPTY_RULES;
        }
    }

    public static List<NormalizationRule> getMetricNameRules(String appName, Map<String, Object> data) {
        try {
            List e = getMetricNameRulesData(appName, data);
            List msg2 = createRules(appName, e);
            String msg1 = MessageFormat.format("Received {0} metric name rule(s) for {1}",
                                                      new Object[] {Integer.valueOf(msg2.size()), appName});
            Agent.LOG.fine(msg1);
            return msg2;
        } catch (Exception var5) {
            String msg = MessageFormat.format("An error occurred getting metric name rules for {0} from the New Relic "
                                                      + "server. This can indicate a problem with the agent rules "
                                                      + "supplied" + " by the New Relic server.: {1}",
                                                     new Object[] {appName, var5});
            if (Agent.LOG.isLoggable(Level.FINER)) {
                Agent.LOG.log(Level.FINER, msg, var5);
            } else {
                Agent.LOG.log(Level.INFO, msg);
            }

            return EMPTY_RULES;
        }
    }

    public static List<NormalizationRule> getTransactionNameRules(String appName, Map<String, Object> data) {
        try {
            List e = getTransactionNameRulesData(appName, data);
            List msg2 = createRules(appName, e);
            String msg1 = MessageFormat.format("Received {0} transaction name rule(s) for {1}",
                                                      new Object[] {Integer.valueOf(msg2.size()), appName});
            Agent.LOG.fine(msg1);
            return msg2;
        } catch (Exception var5) {
            String msg = MessageFormat
                                 .format("An error occurred getting transaction name rules for {0} from the New Relic"
                                                 + " server. This can indicate a problem with the agent rules "
                                                 + "supplied by the New Relic server.: {1}",
                                                new Object[] {appName, var5});
            if (Agent.LOG.isLoggable(Level.FINER)) {
                Agent.LOG.log(Level.FINER, msg, var5);
            } else {
                Agent.LOG.log(Level.INFO, msg);
            }

            return EMPTY_RULES;
        }
    }

    private static List<Map<String, Object>> getUrlRulesData(String appName, Map<String, Object> data) {
        String msg;
        try {
            Object e = data.get("url_rules");
            if (e != null && !DataSenderWriter.nullValue().equals(e)) {
                if (!(e instanceof List)) {
                    msg = MessageFormat.format("Unexpected url rules data for {1}: {2}", new Object[] {appName, e});
                    Agent.LOG.finer(msg);
                    return EMPTY_RULES_DATA;
                } else {
                    return (List) e;
                }
            } else {
                return EMPTY_RULES_DATA;
            }
        } catch (Exception var4) {
            msg = MessageFormat
                          .format("An error occurred getting url rules data for {1} from the New Relic server. This "
                                          + "can indicate a problem with the agent rules supplied by the New Relic "
                                          + "server.: {2}", new Object[] {appName, var4});
            if (Agent.LOG.isLoggable(Level.FINER)) {
                Agent.LOG.log(Level.FINER, msg, var4);
            } else {
                Agent.LOG.log(Level.INFO, msg);
            }

            return EMPTY_RULES_DATA;
        }
    }

    private static List<Map<String, Object>> getMetricNameRulesData(String appName, Map<String, Object> data) {
        String msg;
        try {
            Object e = data.get("metric_name_rules");
            if (e != null && !DataSenderWriter.nullValue().equals(e)) {
                if (!(e instanceof List)) {
                    msg = MessageFormat
                                  .format("Unexpected metric name rules data for {1}: {2}", new Object[] {appName, e});
                    Agent.LOG.finer(msg);
                    return EMPTY_RULES_DATA;
                } else {
                    return (List) e;
                }
            } else {
                return EMPTY_RULES_DATA;
            }
        } catch (Exception var4) {
            msg = MessageFormat
                          .format("An error occurred getting metric name rules data for {1} from the New Relic server"
                                          + ". This can indicate a problem with the agent rules supplied by the New "
                                          + "Relic server.: {2}", new Object[] {appName, var4});
            if (Agent.LOG.isLoggable(Level.FINER)) {
                Agent.LOG.log(Level.FINER, msg, var4);
            } else {
                Agent.LOG.log(Level.INFO, msg);
            }

            return EMPTY_RULES_DATA;
        }
    }

    private static List<Map<String, Object>> getTransactionNameRulesData(String appName, Map<String, Object> data) {
        String msg;
        try {
            Object e = data.get("transaction_name_rules");
            if (e != null && !DataSenderWriter.nullValue().equals(e)) {
                if (!(e instanceof List)) {
                    msg = MessageFormat.format("Unexpected transaction name rules data for {1}: {2}",
                                                      new Object[] {appName, e});
                    Agent.LOG.finer(msg);
                    return EMPTY_RULES_DATA;
                } else {
                    return (List) e;
                }
            } else {
                return EMPTY_RULES_DATA;
            }
        } catch (Exception var4) {
            msg = MessageFormat
                          .format("An error occurred getting transaction name rules data for {1} from the New Relic "
                                          + "server. This can indicate a problem with the agent rules supplied by the"
                                          + " New Relic server.: {2}", new Object[] {appName, var4});
            if (Agent.LOG.isLoggable(Level.FINER)) {
                Agent.LOG.log(Level.FINER, msg, var4);
            } else {
                Agent.LOG.log(Level.INFO, msg);
            }

            return EMPTY_RULES_DATA;
        }
    }

    private static List<NormalizationRule> createRules(String appName, List<Map<String, Object>> rulesData)
            throws Exception {
        ArrayList rules = new ArrayList();

        for (int i = 0; i < rulesData.size(); ++i) {
            Map ruleData = (Map) rulesData.get(i);
            NormalizationRule rule = createRule(ruleData);
            if (rule != null) {
                if (Agent.LOG.isLoggable(Level.FINER)) {
                    String msg = MessageFormat.format("Adding rule for \"{0}\": \"{1}\"", new Object[] {appName, rule});
                    Agent.LOG.finer(msg);
                }

                rules.add(rule);
            }
        }

        sortRules(rules);
        return rules;
    }

    private static void sortRules(List<NormalizationRule> rules) {
        Collections.sort(rules, new Comparator<NormalizationRule>() {
            public int compare(NormalizationRule lhs, NormalizationRule rhs) {
                Integer lhsOrder = Integer.valueOf(lhs.getOrder());
                Integer rhsOrder = Integer.valueOf(rhs.getOrder());
                return lhsOrder.compareTo(rhsOrder);
            }
        });
    }

    private static NormalizationRule createRule(Map<String, Object> ruleData) {
        Boolean eachSegment = (Boolean) ruleData.get("each_segment");
        if (eachSegment == null) {
            eachSegment = Boolean.FALSE;
        }

        Boolean replaceAll = (Boolean) ruleData.get("replace_all");
        if (replaceAll == null) {
            replaceAll = Boolean.FALSE;
        }

        Boolean ignore = (Boolean) ruleData.get("ignore");
        if (ignore == null) {
            ignore = Boolean.FALSE;
        }

        Boolean terminateChain = (Boolean) ruleData.get("terminate_chain");
        if (terminateChain == null) {
            terminateChain = Boolean.TRUE;
        }

        int order = ((Number) ruleData.get("eval_order")).intValue();
        String matchExpression = (String) ruleData.get("match_expression");
        String replacement = (String) ruleData.get("replacement");
        return new NormalizationRule(matchExpression, replacement, ignore.booleanValue(), order,
                                            terminateChain.booleanValue(), eachSegment.booleanValue(),
                                            replaceAll.booleanValue());
    }

    public static List<TransactionSegmentTerms> getTransactionSegmentTermRules(String appName,
                                                                               Map<String, Object> data) {
        List segmentTerms = (List) data.get("transaction_segment_terms");
        Object list;
        if (segmentTerms == null) {
            list = Collections.emptyList();
        } else {
            list = Lists.newArrayList();
            Iterator i$ = segmentTerms.iterator();

            while (i$.hasNext()) {
                Map map = (Map) i$.next();
                List terms = (List) map.get("terms");
                String prefix = (String) map.get("prefix");
                TransactionSegmentTerms tst = new TransactionSegmentTerms(prefix, ImmutableSet.copyOf(terms));
                ((List) list).add(tst);
            }
        }

        Agent.LOG.log(Level.FINE, "Received {0} transaction segment rule(s) for {1}",
                             new Object[] {Integer.valueOf(((List) list).size()), appName});
        return (List) list;
    }
}
