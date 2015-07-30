package com.newrelic.agent.browser;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.BaseConfig;

public class BrowserConfig extends BaseConfig implements IBrowserConfig {
    public static final String BROWSER_KEY = "browser_key";
    public static final String BROWSER_LOADER_VERSION = "browser_monitoring.loader_version";
    public static final String JS_AGENT_LOADER = "js_agent_loader";
    public static final String JS_AGENT_FILE = "js_agent_file";
    public static final String BEACON = "beacon";
    public static final String ERROR_BEACON = "error_beacon";
    public static final String APPLICATION_ID = "application_id";
    private static final String HEADER_BEGIN = "\n<script type=\"text/javascript\">";
    private static final String HEADER_END = "</script>";
    private final BrowserFooter footer;
    private final String header;

    private BrowserConfig(String appName, Map<String, Object> props) throws Exception {
        super(props);

        this.footer = initBrowserFooter(appName);
        this.header = initBrowserHeader();
        logVersion(appName);
    }

    public static IBrowserConfig createBrowserConfig(String appName, Map<String, Object> settings) throws Exception {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new BrowserConfig(appName, settings);
    }

    private void logVersion(String appName) {
        String version = (String) getProperty(BROWSER_LOADER_VERSION);
        if (version != null) {
            Agent.LOG.log(Level.INFO, MessageFormat.format("Using RUM version {0} for application \"{1}\"", version,
                                                                  appName));
        }
    }

    private String initBrowserHeader() throws Exception {
        return HEADER_BEGIN + getRequiredProperty(JS_AGENT_LOADER) + HEADER_END;
    }

    private BrowserFooter initBrowserFooter(String appName) throws Exception {
        String beacon = getRequiredProperty(BEACON);
        String browserKey = getRequiredProperty(BROWSER_KEY);
        String errorBeacon = getRequiredProperty(ERROR_BEACON);
        String payloadScript = getRequiredProperty(JS_AGENT_FILE);
        String appId = getRequiredProperty(APPLICATION_ID);
        return new BrowserFooter(appName, beacon, browserKey, errorBeacon, payloadScript, appId);
    }

    public String getRequiredProperty(String key) throws Exception {
        Object val = getProperty(key, null);
        if (val == null) {
            String msg = MessageFormat.format("Real User Monitoring value for {0} is missing", key);
            throw new Exception(msg);
        }
        return val.toString();
    }

    public String getBrowserTimingHeader() {
        return this.header;
    }

    public String getBrowserTimingFooter(BrowserTransactionState state) {
        return this.footer.getFooter(state);
    }
}