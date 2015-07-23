package com.newrelic.agent.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.yaml.snakeyaml.Loader;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.SequenceNode;

import com.newrelic.agent.Agent;
import com.newrelic.agent.errors.ExceptionHandlerSignature;
import com.newrelic.agent.instrumentation.methodmatchers.InvalidMethodDescriptor;

public class AgentConfigHelper {
    public static final String NEWRELIC_ENVIRONMENT = "newrelic.environment";
    private static final String JAVA_ENVIRONMENT = "JAVA_ENV";
    private static final String PRODUCTION_ENVIRONMENT = "production";

    public static Map<String, Object> getConfigurationFileSettings(File configFile) throws Exception {
        InputStream is = null;
        try {
            is = new FileInputStream(configFile);
            return parseConfiguration(is);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
            }
        }
    }

    private static Map<String, Object> parseConfiguration(InputStream is) throws Exception {
        String env = getEnvironment();
        try {
            Map allConfig = (Map) createYaml().load(is);
            if (allConfig == null) {
                Agent.LOG.info("The configuration file is empty");
                return Collections.emptyMap();
            }
            Map props = (Map) allConfig.get(env);
            if (props == null) {
                props = (Map) allConfig.get("common");
            }
            if (props == null) {
                throw new Exception(MessageFormat.format("Unable to find configuration named {0}", new Object[] {env}));
            }
            return props;
        } catch (Exception e) {
            Agent.LOG.log(Level.SEVERE, MessageFormat.format("Unable to parse configuration file. Please validate the "
                                                                     + "yaml: {0}", new Object[] {e.toString()}), e);

            throw e;
        }
    }

    private static String getEnvironment() {
        try {
            String env = System.getProperty(NEWRELIC_ENVIRONMENT);
            env = env == null ? System.getenv(JAVA_ENVIRONMENT) : env;
            return env == null ? PRODUCTION_ENVIRONMENT : env;
        } catch (Throwable t) {
        }
        return PRODUCTION_ENVIRONMENT;
    }

    private static Yaml createYaml() {
        Constructor constructor = new ExtensionConstructor();
        Loader loader = new Loader(constructor);
        return new Yaml(loader);
    }

    private static class ExtensionConstructor extends Constructor {
        public ExtensionConstructor() {
            yamlConstructors.put("!exception_handler", new Construct() {
                public Object construct(Node node) {
                    List args = constructSequence((SequenceNode) node);
                    try {
                        return new ExceptionHandlerSignature((String) args.get(0), (String) args.get(1),
                                                                    (String) args.get(2));
                    } catch (InvalidMethodDescriptor e) {
                        return e;
                    }
                }

                public void construct2ndStep(Node node, Object o) {
                }
            });
        }
    }
}