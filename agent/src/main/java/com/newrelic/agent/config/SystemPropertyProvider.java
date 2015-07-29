package com.newrelic.agent.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.agent.Agent;

public class SystemPropertyProvider {
    private static final String HEROKU_PREFIX = "NEW_RELIC_";
    private static final String HEROKU_LICENSE_KEY = "NEW_RELIC_LICENSE_KEY";
    private static final String HEROKU_APP_NAME = "NEW_RELIC_APP_NAME";
    private static final String HEROKU_LOG = "NEW_RELIC_LOG";
    private static final String HEROKU_HOST_DISPLAY_NAME = "NEW_RELIC_PROCESS_HOST_DISPLAY_NAME";
    private static final String LICENSE_KEY = "newrelic.config.license_key";
    private static final String APP_NAME = "newrelic.config.app_name";
    private static final String LOG_FILE_NAME = "newrelic.config.log_file_name";
    private static final String HOST_DISPLAY_NAME = "newrelic.config.process_host.display_name";
    private static final String NEW_RELIC_SYSTEM_PROPERTY_ROOT = "newrelic.";
    private final Map<String, String> herokuEnvVars;
    private final Map<String, String> herokuEnvVarsFlattenedMapping;
    private final Map<String, String> newRelicSystemProps;
    private final Map<String, Object> newRelicPropsWithoutPrefix;
    private final SystemProps systemProps;

    public SystemPropertyProvider() {
        this(SystemProps.getSystemProps());
    }

    public SystemPropertyProvider(SystemProps sysProps) {
        systemProps = sysProps;
        herokuEnvVars = initHerokuEnvVariables();
        herokuEnvVarsFlattenedMapping = initHerokuFlattenedEnvVariables();
        newRelicSystemProps = initNewRelicSystemProperties();
        newRelicPropsWithoutPrefix = createNewRelicSystemPropertiesWithoutPrefix();
    }

    private Map<String, String> initHerokuEnvVariables() {
        Map envVars = new HashMap(6);
        envVars.put("newrelic.config.license_key", getenv("NEW_RELIC_LICENSE_KEY"));
        envVars.put("newrelic.config.app_name", getenv("NEW_RELIC_APP_NAME"));
        envVars.put("newrelic.config.log_file_name", getenv("NEW_RELIC_LOG"));
        envVars.put("newrelic.config.process_host.display_name", getenv("NEW_RELIC_PROCESS_HOST_DISPLAY_NAME"));
        return envVars;
    }

    private Map<String, String> initHerokuFlattenedEnvVariables() {
        Map envVars = new HashMap(6);
        envVars.put("NEW_RELIC_LICENSE_KEY", "newrelic.config.license_key");
        envVars.put("NEW_RELIC_APP_NAME", "newrelic.config.app_name");
        envVars.put("NEW_RELIC_LOG", "newrelic.config.log_file_name");
        envVars.put("NEW_RELIC_PROCESS_HOST_DISPLAY_NAME", "newrelic.config.process_host.display_name");
        return envVars;
    }

    private Map<String, String> initNewRelicSystemProperties() {
        Map nrProps = Maps.newHashMap();
        try {
            for (Entry entry : systemProps.getAllSystemPropertes().entrySet()) {
                String key = entry.getKey().toString();
                if (key.startsWith("newrelic.")) {
                    String val = entry.getValue().toString();
                    nrProps.put(key, val);
                }
            }
        } catch (SecurityException t) {
            Agent.LOG.log(Level.FINE, "Unable to get system properties");
        }
        return Collections.unmodifiableMap(nrProps);
    }

    private Map<String, Object> createNewRelicSystemPropertiesWithoutPrefix() {
        Map nrProps = Maps.newHashMap();

        addNewRelicSystemProperties(nrProps, systemProps.getAllSystemPropertes().entrySet());
        addNewRelicEnvProperties(nrProps, systemProps.getAllEnvProperties().entrySet());

        return Collections.unmodifiableMap(nrProps);
    }

    private void addNewRelicSystemProperties(Map<String, Object> nrProps, Set<Entry<Object, Object>> entrySet) {
        for (Entry entry : entrySet) {
            String key = entry.getKey().toString();
            if (key.startsWith("newrelic.config.")) {
                addPropertyWithoutSystemPropRoot(nrProps, key, entry.getValue());
            }
        }
    }

    private void addNewRelicEnvProperties(Map<String, Object> nrProps, Set<Entry<String, String>> entrySet) {
        for (Entry entry : entrySet) {
            String key = entry.getKey().toString();
            if (key.startsWith("newrelic.config.")) {
                addPropertyWithoutSystemPropRoot(nrProps, key, entry.getValue());
            } else {
                String keyToUse = (String) herokuEnvVarsFlattenedMapping.get(key);
                if (keyToUse != null) {
                    addPropertyWithoutSystemPropRoot(nrProps, keyToUse, entry.getValue());
                }
            }
        }
    }

    private void addPropertyWithoutSystemPropRoot(Map<String, Object> nrProps, String key, Object value) {
        String val = value.toString();
        key = key.substring("newrelic.config.".length());
        nrProps.put(key, val);
    }

    public String getEnvironmentVariable(String prop) {
        String val = (String) herokuEnvVars.get(prop);
        if (val != null) {
            return val;
        }
        return getenv(prop);
    }

    public String getSystemProperty(String prop) {
        return systemProps.getSystemProperty(prop);
    }

    private String getenv(String key) {
        return systemProps.getenv(key);
    }

    public Map<String, String> getNewRelicSystemProperties() {
        return newRelicSystemProps;
    }

    public Map<String, Object> getNewRelicPropertiesWithoutPrefix() {
        return newRelicPropsWithoutPrefix;
    }

    protected static abstract class SystemProps {
        static SystemProps getSystemProps() {
            try {
                System.getProperties().get("test");
                System.getenv("test");

                return new SystemProps() {
                    String getSystemProperty(String prop) {
                        return System.getProperty(prop);
                    }

                    String getenv(String key) {
                        return System.getenv(key);
                    }

                    Properties getAllSystemPropertes() {
                        return System.getProperties();
                    }

                    Map<String, String> getAllEnvProperties() {
                        return System.getenv();
                    }
                };
            } catch (SecurityException e) {
                Agent.LOG.error("Unable to access system properties because of a security exception.");
            }
            return new SystemProps() {
                String getSystemProperty(String prop) {
                    return null;
                }

                String getenv(String key) {
                    return null;
                }

                Properties getAllSystemPropertes() {
                    return new Properties();
                }

                Map<String, String> getAllEnvProperties() {
                    return Collections.emptyMap();
                }
            };
        }

        abstract String getSystemProperty(String paramString);

        abstract String getenv(String paramString);

        abstract Properties getAllSystemPropertes();

        abstract Map<String, String> getAllEnvProperties();
    }
}