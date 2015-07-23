package com.newrelic.api.agent;

/**
 * Provides access to agent configuration settings. The key names are flattened representations of values in the
 * configuration file.
 * 
 * Example keys: <br/>
 * transaction_tracer.enabled<br />
 * instrumentation.hibernate.stat_sampler.enabled
 * 
 * @author sdaubin
 * 
 */
public interface Config {

    /**
     * Get the value of a setting.
     * 
     * @param key The flattened configuration setting key.
     * @return The value of the property or null if the value is absent.
     * @since 3.9.0
     */
    <T> T getValue(String key);

    /**
     * Get the value of a setting, returning the default if the value is not present.
     * 
     * @param key The flattened configuration setting key.
     * @param defaultVal The default value to return if the given key is not present.
     * @return The value of the property or defaultVal if the value is absent.
     * @since 3.9.0
     * 
     */
    <T> T getValue(String key, T defaultVal);
}
