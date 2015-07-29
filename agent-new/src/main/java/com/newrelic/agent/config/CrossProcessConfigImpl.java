package com.newrelic.agent.config;

import com.newrelic.agent.Agent;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.util.Obfuscator;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class CrossProcessConfigImpl extends BaseConfig
  implements CrossProcessConfig
{
  public static final String CROSS_APPLICATION_TRACING = "cross_application_tracing";
  public static final String CROSS_PROCESS_ID = "cross_process_id";
  public static final String ENABLED = "enabled";
  public static final String ENCODING_KEY = "encoding_key";
  public static final String TRUSTED_ACCOUNT_IDS = "trusted_account_ids";
  public static final boolean DEFAULT_ENABLED = true;
  public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.cross_application_tracer.";
  private final boolean isCrossApplicationTracing;
  private final String crossProcessId;
  private final String encodingKey;
  private final String encodedCrossProcessId;
  private final Set<String> trustedIds;

  private CrossProcessConfigImpl(Map<String, Object> props)
  {
    super(props, "newrelic.config.cross_application_tracer.");
    this.trustedIds = Collections.unmodifiableSet(new HashSet(getUniqueStrings("trusted_account_ids")));
    this.isCrossApplicationTracing = initEnabled();
    this.crossProcessId = ((String)getProperty("cross_process_id"));
    this.encodingKey = ((String)getProperty("encoding_key"));
    this.encodedCrossProcessId = initEncodedCrossProcessId(this.crossProcessId, this.encodingKey);
  }

  private boolean initEnabled() {
    Boolean enabled = (Boolean)getProperty("enabled");
    if (enabled != null) {
      return enabled.booleanValue();
    }

    return ((Boolean)getProperty("cross_application_tracing", Boolean.valueOf(true))).booleanValue();
  }

  private String initEncodedCrossProcessId(String crossProcessId, String encodingKey) {
    if ((crossProcessId == null) || (encodingKey == null))
      return null;
    try
    {
      return Obfuscator.obfuscateNameUsingKey(crossProcessId, encodingKey);
    } catch (UnsupportedEncodingException e) {
      String msg = MessageFormat.format("Error encoding cross process id {0}: {1}", new Object[] { crossProcessId, e });
      Agent.LOG.error(msg);
    }return null;
  }

  public boolean isCrossApplicationTracing()
  {
    return this.isCrossApplicationTracing;
  }

  public String getCrossProcessId()
  {
    return this.isCrossApplicationTracing ? this.crossProcessId : null;
  }

  public String getEncodedCrossProcessId()
  {
    return this.isCrossApplicationTracing ? this.encodedCrossProcessId : null;
  }

  public String getEncodingKey()
  {
    return this.isCrossApplicationTracing ? this.encodingKey : null;
  }

  public boolean isTrustedAccountId(String accountId)
  {
    return this.trustedIds.contains(accountId);
  }

  static CrossProcessConfig createCrossProcessConfig(Map<String, Object> settings) {
    if (settings == null) {
      settings = Collections.emptyMap();
    }
    return new CrossProcessConfigImpl(settings);
  }
}