package com.newrelic.agent.attributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.newrelic.agent.config.AgentConfig;

public class AttributesFilter {
    private final boolean captureRequestParameters;
    private final boolean captureMessageParameters;
    private final DestinationFilter errorFilter;
    private final DestinationFilter eventsFilter;
    private final DestinationFilter traceFilter;
    private final DestinationFilter browserFilter;

    public AttributesFilter(AgentConfig config) {
        this(config, AttributesConfigUtil.DEFAULT_BROWSER_EXCLUDES, AttributesConfigUtil.DEFAULT_ERRORS_EXCLUDES,
                    AttributesConfigUtil.DEFAULT_EVENTS_EXCLUDES, AttributesConfigUtil.DEFAULT_TRACES_EXCLUDES);
    }

    public AttributesFilter(AgentConfig config, String[] defaultExcludeBrowser, String[] defaultExcludeErrors,
                            String[] defaultExcludeEvents, String[] defaultExcludeTraces) {
        List<String> rootExcludes = new ArrayList<String>();
        rootExcludes.addAll(AttributesConfigUtil.getBaseList(config, "attributes.exclude"));
        rootExcludes.addAll(AttributesConfigUtil.getBaseList(config, "ignored_params", "request.parameters."));

        rootExcludes.addAll(AttributesConfigUtil.getBaseList(config, "ignored_messaging_params", "message.parameters."));

        List<String> rootIncludes = AttributesConfigUtil.getBaseList(config, "attributes.include");

        boolean captureParams = AttributesConfigUtil.isCaptureAttributes(config);
        boolean captureMessageParams = AttributesConfigUtil.isCaptureMessageAttributes(config);

        errorFilter = new DestinationFilter("error_collector", true, config, rootExcludes, rootIncludes, captureParams,
                                                   captureMessageParams, defaultExcludeErrors, "error_collector");

        eventsFilter =
                new DestinationFilter("transaction_events", true, config, rootExcludes, rootIncludes, captureParams,
                                             captureMessageParams, defaultExcludeEvents, "transaction_events",
                                             "analytics_events");

        traceFilter =
                new DestinationFilter("transaction_tracer", true, config, rootExcludes, rootIncludes, captureParams,
                                             captureMessageParams, defaultExcludeTraces, "transaction_tracer");

        browserFilter =
                new DestinationFilter("browser_monitoring", false, config, rootExcludes, rootIncludes, captureParams,
                                             captureMessageParams, defaultExcludeBrowser, "browser_monitoring");

        boolean enabled = (errorFilter.isEnabled()) || (eventsFilter.isEnabled()) || (traceFilter.isEnabled());
        captureRequestParameters =
                captureAllParams(enabled, config.isHighSecurity(), captureParams, "request.parameters.");

        captureMessageParameters =
                captureAllParams(enabled, config.isHighSecurity(), captureMessageParams, "message.parameters.");
    }

    private boolean captureAllParams(boolean enabled, boolean highSecurity, boolean captureParams, String paramStart) {
        return !((!enabled) || (highSecurity)) && ((captureParams) || (errorFilter.isPotentialConfigMatch(paramStart))
                                                           || (eventsFilter.isPotentialConfigMatch(paramStart))
                                                           || (traceFilter.isPotentialConfigMatch(paramStart))
                                                           || (browserFilter.isPotentialConfigMatch(paramStart)));
    }

    public boolean captureRequestParams() {
        return captureRequestParameters;
    }

    public boolean captureMessageParams() {
        return captureMessageParameters;
    }

    public boolean isAttributesEnabledForErrors() {
        return errorFilter.isEnabled();
    }

    public boolean isAttributesEnabledForEvents() {
        return eventsFilter.isEnabled();
    }

    public boolean isAttributesEnabledForTraces() {
        return traceFilter.isEnabled();
    }

    public boolean isAttributesEnabledForBrowser() {
        return browserFilter.isEnabled();
    }

    public Map<String, ? extends Object> filterErrorAttributes(Map<String, ? extends Object> values) {
        return errorFilter.filterAttributes(values);
    }

    public Map<String, ? extends Object> filterEventAttributes(Map<String, ? extends Object> values) {
        return eventsFilter.filterAttributes(values);
    }

    public Map<String, ? extends Object> filterTraceAttributes(Map<String, ? extends Object> values) {
        return traceFilter.filterAttributes(values);
    }

    public Map<String, ? extends Object> filterBrowserAttributes(Map<String, ? extends Object> values) {
        return browserFilter.filterAttributes(values);
    }
}