package com.newrelic.agent.attributes;

import com.newrelic.agent.config.AgentConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AttributesFilter
{
  private final boolean captureRequestParameters;
  private final boolean captureMessageParameters;
  private final DestinationFilter errorFilter;
  private final DestinationFilter eventsFilter;
  private final DestinationFilter traceFilter;
  private final DestinationFilter browserFilter;

  public AttributesFilter(AgentConfig config)
  {
    this(config, AttributesConfigUtil.DEFAULT_BROWSER_EXCLUDES, AttributesConfigUtil.DEFAULT_ERRORS_EXCLUDES, AttributesConfigUtil.DEFAULT_EVENTS_EXCLUDES, AttributesConfigUtil.DEFAULT_TRACES_EXCLUDES);
  }

  public AttributesFilter(AgentConfig config, String[] defaultExcludeBrowser, String[] defaultExcludeErrors, String[] defaultExcludeEvents, String[] defaultExcludeTraces)
  {
    List rootExcludes = new ArrayList();
    rootExcludes.addAll(AttributesConfigUtil.getBaseList(config, "attributes.exclude"));
    rootExcludes.addAll(AttributesConfigUtil.getBaseList(config, "ignored_params", "request.parameters."));

    rootExcludes.addAll(AttributesConfigUtil.getBaseList(config, "ignored_messaging_params", "message.parameters."));

    List rootIncludes = AttributesConfigUtil.getBaseList(config, "attributes.include");

    boolean captureParams = AttributesConfigUtil.isCaptureAttributes(config);
    boolean captureMessageParams = AttributesConfigUtil.isCaptureMessageAttributes(config);

    this.errorFilter = new DestinationFilter("error_collector", true, config, rootExcludes, rootIncludes, captureParams, captureMessageParams, defaultExcludeErrors, new String[] { "error_collector" });

    this.eventsFilter = new DestinationFilter("transaction_events", true, config, rootExcludes, rootIncludes, captureParams, captureMessageParams, defaultExcludeEvents, new String[] { "transaction_events", "analytics_events" });

    this.traceFilter = new DestinationFilter("transaction_tracer", true, config, rootExcludes, rootIncludes, captureParams, captureMessageParams, defaultExcludeTraces, new String[] { "transaction_tracer" });

    this.browserFilter = new DestinationFilter("browser_monitoring", false, config, rootExcludes, rootIncludes, captureParams, captureMessageParams, defaultExcludeBrowser, new String[] { "browser_monitoring" });

    boolean enabled = (this.errorFilter.isEnabled()) || (this.eventsFilter.isEnabled()) || (this.traceFilter.isEnabled());
    this.captureRequestParameters = captureAllParams(enabled, config.isHighSecurity(), captureParams, "request.parameters.");

    this.captureMessageParameters = captureAllParams(enabled, config.isHighSecurity(), captureMessageParams, "message.parameters.");
  }

  private boolean captureAllParams(boolean enabled, boolean highSecurity, boolean captureParams, String paramStart)
  {
    if ((!enabled) || (highSecurity)) {
      return false;
    }
    return (captureParams) || (this.errorFilter.isPotentialConfigMatch(paramStart)) || (this.eventsFilter.isPotentialConfigMatch(paramStart)) || (this.traceFilter.isPotentialConfigMatch(paramStart)) || (this.browserFilter.isPotentialConfigMatch(paramStart));
  }

  public boolean captureRequestParams()
  {
    return this.captureRequestParameters;
  }

  public boolean captureMessageParams() {
    return this.captureMessageParameters;
  }

  public boolean isAttributesEnabledForErrors() {
    return this.errorFilter.isEnabled();
  }

  public boolean isAttributesEnabledForEvents() {
    return this.eventsFilter.isEnabled();
  }

  public boolean isAttributesEnabledForTraces() {
    return this.traceFilter.isEnabled();
  }

  public boolean isAttributesEnabledForBrowser() {
    return this.browserFilter.isEnabled();
  }

  public Map<String, ? extends Object> filterErrorAttributes(Map<String, ? extends Object> values) {
    return this.errorFilter.filterAttributes(values);
  }

  public Map<String, ? extends Object> filterEventAttributes(Map<String, ? extends Object> values) {
    return this.eventsFilter.filterAttributes(values);
  }

  public Map<String, ? extends Object> filterTraceAttributes(Map<String, ? extends Object> values) {
    return this.traceFilter.filterAttributes(values);
  }

  public Map<String, ? extends Object> filterBrowserAttributes(Map<String, ? extends Object> values) {
    return this.browserFilter.filterAttributes(values);
  }
}