//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.jmx.create;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.newrelic.agent.Agent;
import com.newrelic.agent.extension.Extension;
import com.newrelic.agent.jmx.JmxType;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.stats.StatsEngine;

public abstract class JmxGet extends JmxObject {
    private static final Pattern TYPE_QUERY_PATTERN = Pattern.compile(",(.*?)=");
    private static final Pattern PULL_VALUE_PATTERN = Pattern.compile("\\{(.*?)\\}");
    private static final Pattern PULL_ATTRIBUTE_PATTERN = Pattern.compile("\\:(.*?)\\:");
    private final String rootMetricName;
    private final boolean isPattern;
    private final Set<String> attributes;
    private final List<JmxMetric> metrics;
    private final Extension origin;
    private final JmxAttributeFilter attributeFilter;
    private final JmxMetricModifier modifier;

    public JmxGet(String pObjectName, String rootMetricName, String safeName,
                  Map<JmxType, List<String>> pAttributesToType, Extension origin) throws MalformedObjectNameException {
        super(pObjectName, safeName);
        this.origin = origin;
        this.attributeFilter = null;
        this.modifier = null;
        this.rootMetricName = this.getRootMetricName(rootMetricName);
        this.isPattern = isPattern(rootMetricName);
        this.attributes = new HashSet();
        this.metrics = new ArrayList();
        Iterator i$ = pAttributesToType.entrySet().iterator();

        while (i$.hasNext()) {
            Entry current = (Entry) i$.next();
            JmxType type = (JmxType) current.getKey();
            List attrs = (List) current.getValue();
            Iterator i$1 = attrs.iterator();

            while (i$1.hasNext()) {
                String att = (String) i$1.next();
                this.attributes.add(att);
                this.metrics.add(JmxMetric.create(att, type));
            }
        }

    }

    public JmxGet(String pObjectName, String safeName, String pRootMetric, List<JmxMetric> pMetrics,
                  JmxAttributeFilter attributeFilter, JmxMetricModifier pModifier) throws MalformedObjectNameException {
        super(pObjectName, safeName);
        this.origin = null;
        this.attributeFilter = attributeFilter;
        this.modifier = pModifier;
        this.rootMetricName = this.getRootMetricName(pRootMetric);
        this.isPattern = isPattern(this.rootMetricName);
        if (pMetrics == null) {
            this.metrics = new ArrayList();
        } else {
            this.metrics = pMetrics;
        }

        this.attributes = new HashSet();
        Iterator i$ = this.metrics.iterator();

        while (i$.hasNext()) {
            JmxMetric m = (JmxMetric) i$.next();
            this.attributes.addAll(Arrays.asList(m.getAttributes()));
        }

    }

    private static boolean isPattern(String rootMetricName) {
        return rootMetricName != null ? rootMetricName.contains("{") : false;
    }

    protected static String cleanValue(String value) {
        value = value.trim();
        return value.length() > 0 && value.charAt(0) == 47 ? value.substring(1) : value;
    }

    private static String formatSegment(String metricSegment) {
        return metricSegment.length() > 0 && metricSegment.charAt(0) == 47 ? metricSegment.substring(1) : metricSegment;
    }

    public abstract void recordStats(StatsEngine var1, Map<ObjectName, Map<String, Float>> var2, MBeanServer var3);

    private String getRootMetricName(String root) {
        if (root != null) {
            if (!root.endsWith("/")) {
                root = root + "/";
            }

            if (!root.startsWith("JMX/") && !root.startsWith("JmxBuiltIn")) {
                root = "JMX/" + root;
            }
        }

        return root;
    }

    public Collection<String> getAttributes() {
        return this.attributes;
    }

    public String getRootMetricName(ObjectName actualName, MBeanServer server) {
        return this.rootMetricName != null ? this.pullAttValuesFromName(actualName, server)
                       : this.getDefaultName(actualName);
    }

    private String pullAttValuesFromName(ObjectName actualName, MBeanServer server) {
        if (!this.isPattern) {
            return this.rootMetricName;
        } else {
            StringBuffer sb = new StringBuffer();
            Matcher m = PULL_VALUE_PATTERN.matcher(this.rootMetricName);
            Hashtable keyProperties = actualName.getKeyPropertyList();
            String value = null;

            while (m.find()) {
                String key = m.group(1);
                Matcher attributeMatcher = PULL_ATTRIBUTE_PATTERN.matcher(key);
                if (attributeMatcher.matches()) {
                    key = attributeMatcher.group(1);

                    try {
                        value = server.getAttribute(actualName, key).toString();
                    } catch (Throwable var10) {
                        Agent.LOG.log(Level.FINEST, var10, var10.getMessage(), new Object[0]);
                    }
                } else {
                    value = (String) keyProperties.get(key);
                }

                if (value != null) {
                    m.appendReplacement(sb, cleanValue(value));
                } else {
                    m.appendReplacement(sb, "");
                }
            }

            m.appendTail(sb);
            if (sb.charAt(sb.length() - 1) != 47) {
                sb.append('/');
            }

            if (this.modifier == null) {
                return sb.toString();
            } else {
                return this.modifier.getMetricName(sb.toString());
            }
        }
    }

    private String getDefaultName(ObjectName actualName) {
        Hashtable keyProperties = actualName.getKeyPropertyList();
        String type = (String) keyProperties.remove("type");
        StringBuilder rootPath = (new StringBuilder("JMX")).append('/');
        if (actualName.getDomain() != null) {
            rootPath.append(actualName.getDomain()).append('/');
        }

        rootPath.append(type);
        if (keyProperties.size() > 1) {
            String str = this.getObjectNameString();
            Matcher matcher = TYPE_QUERY_PATTERN.matcher(str);

            while (matcher.find()) {
                String group = matcher.group(1);
                String val = (String) keyProperties.remove(group);
                if (val != null) {
                    rootPath.append('/');
                    rootPath.append(formatSegment(val));
                }
            }
        }

        if (keyProperties.size() == 1) {
            rootPath.append('/');
            rootPath.append(formatSegment((String) ((Entry) keyProperties.entrySet().iterator().next()).getValue()));
        }

        rootPath.append('/');
        return rootPath.toString();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("object_name: ").append(this.getObjectNameString());
        sb.append(" attributes: [");
        Iterator it = this.metrics.iterator();

        while (it.hasNext()) {
            JmxMetric metric = (JmxMetric) it.next();
            sb.append(metric.getAttributeMetricName()).append(" type: ").append(metric.getType().getYmlName());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }

        sb.append("]");
        return sb.toString();
    }

    public Extension getOrigin() {
        return this.origin;
    }

    protected JmxAttributeFilter getJmxAttributeFilter() {
        return this.attributeFilter;
    }

    protected List<JmxMetric> getJmxMetrics() {
        return this.metrics;
    }
}
