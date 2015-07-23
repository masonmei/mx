package com.newrelic.agent.normalization;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.newrelic.agent.ConnectionListener;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;

public class NormalizationServiceImpl extends AbstractService implements NormalizationService, ConnectionListener {
    private static final Pattern PARAMETER_DELIMITER_PATTERN = Pattern.compile("(.*?)(\\?|#|;).*", 32);
    private static final List<NormalizationRule> EMPTY_RULES = Collections.emptyList();

    private final ConcurrentMap<String, Normalizer> urlNormalizers = new ConcurrentHashMap();
    private final ConcurrentMap<String, Normalizer> transactionNormalizers = new ConcurrentHashMap();
    private final ConcurrentMap<String, Normalizer> metricNormalizers = new ConcurrentHashMap();
    private final String defaultAppName;
    private final boolean autoAppNamingEnabled;
    private volatile Normalizer defaultUrlNormalizer;
    private volatile Normalizer defaultTransactionNormalizer;
    private volatile Normalizer defaultMetricNormalizer;

    public NormalizationServiceImpl() {
        super(ConnectionListener.class.getSimpleName());
        AgentConfig defaultAgentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
        defaultAppName = defaultAgentConfig.getApplicationName();
        autoAppNamingEnabled = defaultAgentConfig.isAutoAppNamingEnabled();
        defaultUrlNormalizer = createUrlNormalizer(defaultAppName, EMPTY_RULES);
        defaultTransactionNormalizer = createTransactionNormalizer(defaultAppName, EMPTY_RULES, Collections
                                                                                                        .<TransactionSegmentTerms>emptyList());

        defaultMetricNormalizer = createMetricNormalizer(defaultAppName, EMPTY_RULES);
        ServiceFactory.getRPMServiceManager().addConnectionListener(this);
    }

    protected void doStart() throws Exception {
    }

    protected void doStop() throws Exception {
        ServiceFactory.getRPMServiceManager().removeConnectionListener(this);
    }

    public String getUrlBeforeParameters(String url) {
        Matcher paramDelimiterMatcher = PARAMETER_DELIMITER_PATTERN.matcher(url);
        if (paramDelimiterMatcher.matches()) {
            return paramDelimiterMatcher.group(1);
        }
        return url;
    }

    public Normalizer getUrlNormalizer(String appName) {
        return getOrCreateUrlNormalizer(appName);
    }

    public Normalizer getTransactionNormalizer(String appName) {
        return getOrCreateTransactionNormalizer(appName);
    }

    public Normalizer getMetricNormalizer(String appName) {
        return getOrCreateMetricNormalizer(appName);
    }

    public boolean isEnabled() {
        return true;
    }

    public void connected(IRPMService rpmService, Map<String, Object> data) {
        String appName = rpmService.getApplicationName();
        List urlRules = NormalizationRuleFactory.getUrlRules(appName, data);
        List metricNameRules = NormalizationRuleFactory.getMetricNameRules(appName, data);
        List transactionNameRules = NormalizationRuleFactory.getTransactionNameRules(appName, data);

        List transactionSegmentTermRules = NormalizationRuleFactory.getTransactionSegmentTermRules(appName, data);

        Normalizer normalizer = createUrlNormalizer(appName, urlRules);
        replaceUrlNormalizer(appName, normalizer);

        normalizer = createTransactionNormalizer(appName, transactionNameRules, transactionSegmentTermRules);
        replaceTransactionNormalizer(appName, normalizer);

        normalizer = createMetricNormalizer(appName, metricNameRules);
        replaceMetricNormalizer(appName, normalizer);
    }

    public void disconnected(IRPMService rpmService) {
    }

    private Normalizer getOrCreateUrlNormalizer(String appName) {
        Normalizer normalizer = findUrlNormalizer(appName);
        if (normalizer != null) {
            return normalizer;
        }
        normalizer = createUrlNormalizer(appName, EMPTY_RULES);
        Normalizer oldNormalizer = (Normalizer) urlNormalizers.putIfAbsent(appName, normalizer);
        return oldNormalizer == null ? normalizer : oldNormalizer;
    }

    private Normalizer findUrlNormalizer(String appName) {
        if ((!autoAppNamingEnabled) || (appName == null) || (appName.equals(defaultAppName))) {
            return defaultUrlNormalizer;
        }
        return (Normalizer) urlNormalizers.get(appName);
    }

    private void replaceUrlNormalizer(String appName, Normalizer normalizer) {
        Normalizer oldNormalizer = getUrlNormalizer(appName);
        if (oldNormalizer == defaultUrlNormalizer) {
            defaultUrlNormalizer = normalizer;
        } else {
            urlNormalizers.put(appName, normalizer);
        }
    }

    private Normalizer getOrCreateTransactionNormalizer(String appName) {
        Normalizer normalizer = findTransactionNormalizer(appName);
        if (normalizer != null) {
            return normalizer;
        }
        normalizer =
                createTransactionNormalizer(appName, EMPTY_RULES, Collections.<TransactionSegmentTerms>emptyList());

        Normalizer oldNormalizer = (Normalizer) transactionNormalizers.putIfAbsent(appName, normalizer);
        return oldNormalizer == null ? normalizer : oldNormalizer;
    }

    private Normalizer findTransactionNormalizer(String appName) {
        if ((!autoAppNamingEnabled) || (appName == null) || (appName.equals(defaultAppName))) {
            return defaultTransactionNormalizer;
        }
        return (Normalizer) transactionNormalizers.get(appName);
    }

    private void replaceTransactionNormalizer(String appName, Normalizer normalizer) {
        Normalizer oldNormalizer = getTransactionNormalizer(appName);
        if (oldNormalizer == defaultTransactionNormalizer) {
            defaultTransactionNormalizer = normalizer;
        } else {
            transactionNormalizers.put(appName, normalizer);
        }
    }

    private Normalizer getOrCreateMetricNormalizer(String appName) {
        Normalizer normalizer = findMetricNormalizer(appName);
        if (normalizer != null) {
            return normalizer;
        }
        normalizer = createMetricNormalizer(appName, EMPTY_RULES);
        Normalizer oldNormalizer = (Normalizer) metricNormalizers.putIfAbsent(appName, normalizer);
        return oldNormalizer == null ? normalizer : oldNormalizer;
    }

    private Normalizer findMetricNormalizer(String appName) {
        if ((!autoAppNamingEnabled) || (appName == null) || (appName.equals(defaultAppName))) {
            return defaultMetricNormalizer;
        }
        return (Normalizer) metricNormalizers.get(appName);
    }

    private void replaceMetricNormalizer(String appName, Normalizer normalizer) {
        Normalizer oldNormalizer = getMetricNormalizer(appName);
        if (oldNormalizer == defaultMetricNormalizer) {
            defaultMetricNormalizer = normalizer;
        } else {
            metricNormalizers.put(appName, normalizer);
        }
    }

    private Normalizer createUrlNormalizer(String appName, List<NormalizationRule> urlRules) {
        return NormalizerFactory.createUrlNormalizer(appName, urlRules);
    }

    private Normalizer createTransactionNormalizer(String appName, List<NormalizationRule> metricNameRules,
                                                   List<TransactionSegmentTerms> transactionSegmentTermRules) {
        return NormalizerFactory.createTransactionNormalizer(appName, metricNameRules, transactionSegmentTermRules);
    }

    private Normalizer createMetricNormalizer(String appName, List<NormalizationRule> metricNameRules) {
        return NormalizerFactory.createMetricNormalizer(appName, metricNameRules);
    }
}