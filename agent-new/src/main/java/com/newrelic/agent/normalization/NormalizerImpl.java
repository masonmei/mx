package com.newrelic.agent.normalization;

import com.newrelic.agent.Agent;
import com.newrelic.agent.logging.IAgentLogger;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class NormalizerImpl
  implements Normalizer
{
  private final List<NormalizationRule> rules;
  private final String appName;

  public NormalizerImpl(String appName, List<NormalizationRule> rules)
  {
    this.appName = appName;
    this.rules = Collections.unmodifiableList(rules);
  }

  public String normalize(String name)
  {
    if (name == null) {
      return null;
    }

    String normalizedName = name;
    for (NormalizationRule rule : this.rules) {
      RuleResult result = rule.normalize(normalizedName);
      if (result.isMatch())
      {
        if (rule.isIgnore()) {
          if (Agent.LOG.isLoggable(Level.FINER)) {
            String msg = MessageFormat.format("Ignoring \"{0}\" for \"{1}\" because it matched rule \"{2}\"", new Object[] { name, this.appName, rule });

            Agent.LOG.finer(msg);
          }
          return null;
        }
        String replacement = result.getReplacement();
        if (replacement != null) {
          if (Agent.LOG.isLoggable(Level.FINER)) {
            String msg = MessageFormat.format("Normalized \"{0}\" to \"{2}\" for \"{1}\" using rule \"{3}\"", new Object[] { name, this.appName, replacement, rule });

            Agent.LOG.finer(msg);
          }
          normalizedName = replacement;
        }
        if (rule.isTerminateChain())
          break;
      }
    }
    return normalizedName;
  }

  public List<NormalizationRule> getRules()
  {
    return this.rules;
  }
}