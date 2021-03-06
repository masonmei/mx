package com.newrelic.agent.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class BrowserMonitoringConfigImpl extends BaseConfig implements BrowserMonitoringConfig {
    public static final String AUTO_INSTRUMENT = "auto_instrument";
    public static final String DISABLE_AUTO_PAGES = "disabled_auto_pages";
    public static final String SSL_FOR_HTTP = "ssl_for_http";
    public static final String LOADER_TYPE = "loader";
    public static final String DEBUG = "debug";
    public static final String ALLOW_MULTIPLE_FOOTERS = "allow_multiple_footers";
    public static final boolean DEFAULT_AUTO_INSTRUMENT = true;
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.browser_monitoring.";
    public static final String DEFAULT_LOADER_TYPE = "rum";
    public static final boolean DEFAULT_DEBUG = false;
    public static final boolean DEFAULT_SSL_FOR_HTTP = true;
    public static final boolean DEFAULT_ALLOW_MULTIPLE_FOOTERS = false;
    private final boolean auto_instrument;
    private final Set<String> disabledAutoPages;
    private final String loaderType;
    private final boolean debug;
    private final boolean sslForHttp;
    private final boolean isSslForHttpSet;
    private final boolean multipleFooters;

    private BrowserMonitoringConfigImpl(Map<String, Object> props) {
        super(props, "newrelic.config.browser_monitoring.");
        auto_instrument = ((Boolean) getProperty("auto_instrument", Boolean.valueOf(true))).booleanValue();
        disabledAutoPages = Collections.unmodifiableSet(new HashSet(getUniqueStrings("disabled_auto_pages")));
        loaderType = ((String) getProperty("loader", "rum"));
        debug = ((Boolean) getProperty("debug", Boolean.valueOf(false))).booleanValue();
        Boolean sslForHttpTmp = (Boolean) getProperty("ssl_for_http");
        isSslForHttpSet = (sslForHttpTmp != null);
        sslForHttp = (isSslForHttpSet ? sslForHttpTmp.booleanValue() : true);
        multipleFooters = ((Boolean) getProperty("allow_multiple_footers", Boolean.valueOf(false))).booleanValue();
    }

    static BrowserMonitoringConfig createBrowserMonitoringConfig(Map<String, Object> settings) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new BrowserMonitoringConfigImpl(settings);
    }

    public boolean isAutoInstrumentEnabled() {
        return auto_instrument;
    }

    public Set<String> getDisabledAutoPages() {
        return disabledAutoPages;
    }

    public String getLoaderType() {
        return loaderType;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isSslForHttp() {
        return sslForHttp;
    }

    public boolean isSslForHttpSet() {
        return isSslForHttpSet;
    }

    public boolean isAllowMultipleFooters() {
        return multipleFooters;
    }
}