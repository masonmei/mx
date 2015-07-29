//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.jmx.create;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.jmx.JmxType;

public class JmxYmlParser implements JmxConfiguration {
  private static final String YML_PROP_OBJECT_NAME = "object_name";
  private static final String YML_PROP_ROOT_METRIC_NAME = "root_metric_name";
  private static final String YML_PROP_ENABLED = "enabled";
  private static final String YML_PROP_METRICS = "metrics";
  private static final String YML_PROP_ATTRS = "attributes";
  private static final String YML_PROP_ATT = "attribute";
  private static final String YML_PROP_TYPE = "type";
  private final Map<?, ?> jmxConfig;

  public JmxYmlParser(Map<?, ?> pJmxConfig) {
    this.jmxConfig = pJmxConfig;
  }

  private static JmxType findType(Map<?, ?> metricMap) {
    String type = (String) metricMap.get("type");
    if (type != null && !type.equals(JmxType.MONOTONICALLY_INCREASING.getYmlName())) {
      if (type.equals(JmxType.SIMPLE.getYmlName())) {
        return JmxType.SIMPLE;
      } else {
        String msg = MessageFormat.format("Unknown JMX metric type: {0}.  Using default type: {1}",
                                                 new Object[] {type, JmxType.MONOTONICALLY_INCREASING});
        Agent.LOG.warning(msg);
        return JmxType.MONOTONICALLY_INCREASING;
      }
    } else {
      return JmxType.MONOTONICALLY_INCREASING;
    }
  }

  private static List<String> findAttributes(Map<?, ?> metricMap) {
    ArrayList result = new ArrayList();
    String attributes = (String) metricMap.get("attributes");
    String attribute1;
    if (attributes != null) {
      String[] arr$ = attributes.split(",");
      int len$ = arr$.length;

      for (int i$ = 0; i$ < len$; ++i$) {
        String attribute = arr$[i$];
        attribute1 = attribute.trim();
        if (attribute1.length() != 0) {
          result.add(attribute1);
        }
      }
    } else {
      attribute1 = (String) metricMap.get("attribute");
      if (attribute1 != null && attribute1.trim().length() > 0) {
        result.add(attribute1.trim());
      }
    }

    return result;
  }

  public String getObjectName() {
    return (String) this.jmxConfig.get("object_name");
  }

  public String getRootMetricName() {
    return (String) this.jmxConfig.get("root_metric_name");
  }

  public boolean getEnabled() {
    Boolean isEnabled = (Boolean) this.jmxConfig.get("enabled");
    return isEnabled == null || isEnabled.booleanValue();
  }

  public Map<JmxType, List<String>> getAttrs() {
    List metrics = (List) this.jmxConfig.get("metrics");
    if (metrics == null) {
      Agent.LOG.log(Level.WARNING,
                           "There is no \'metric\' property in the JMX configuration file. Please verify the "
                                   + "format of your yml file.");
      return null;
    } else {
      HashMap attrs = new HashMap(3);
      Iterator i$ = metrics.iterator();

      while (i$.hasNext()) {
        Map metric = (Map) i$.next();
        JmxType type = findType(metric);
        List attList = findAttributes(metric);
        if (attList.size() > 0) {
          List alreadyAdded = (List) attrs.get(type);
          if (alreadyAdded == null) {
            attrs.put(type, attList);
          } else {
            alreadyAdded.addAll(attList);
          }
        }
      }

      return attrs;
    }
  }
}
