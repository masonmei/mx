package com.newrelic.agent.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.extension.Extension;
import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.yaml.InstrumentationConstructor;
import com.newrelic.agent.instrumentation.yaml.PointCutFactory;
import com.newrelic.deps.org.yaml.snakeyaml.Loader;
import com.newrelic.deps.org.yaml.snakeyaml.Yaml;
import com.newrelic.deps.org.yaml.snakeyaml.constructor.Constructor;

public class PointCutConfig {
    private static String defaultMetricPrefix;
    private final List<ExtensionClassAndMethodMatcher> pcList = new ArrayList();
    private Yaml yaml;

    public PointCutConfig(File[] files) {
        if (null != files) {
            initYaml();
            for (File file : files) {
                try {
                    FileInputStream input = new FileInputStream(file);
                    loadYaml(input);
                    Agent.LOG.info(MessageFormat.format("Loaded custom instrumentation from {0}",
                                                               new Object[] {file.getName()}));
                } catch (FileNotFoundException e) {
                    Agent.LOG.warning(MessageFormat
                                              .format("Could not open instrumentation file {0}. Please check that the"
                                                              + " file exists and has the correct permissions. ",
                                                             new Object[] {file.getPath()}));
                } catch (Exception e) {
                    Agent.LOG.log(Level.SEVERE, MessageFormat
                                                        .format("Error loading YAML instrumentation from {0}. Please "
                                                                        + "check the file's format.",
                                                                       new Object[] {file.getName()}));

                    Agent.LOG.log(Level.FINER, "YAML error: ", e);
                }
            }
        }
    }

    public PointCutConfig(InputStream input) {
        initYaml();
        try {
            loadYaml(input);
        } catch (Exception e) {
            Agent.LOG.log(Level.SEVERE, "Error loading YAML instrumentation");
            Agent.LOG.log(Level.FINER, "Error: ", e);
        }
    }

    public static Collection<ExtensionClassAndMethodMatcher> getExtensionPointCuts(Extension extension,
                                                                                   Map instrumentation) {
        Collection list = new ArrayList();
        if (instrumentation != null) {
            list.addAll(addInstrumentation(extension, instrumentation));
        }
        if (Agent.LOG.isLoggable(Level.FINEST)) {
            for (ExtensionClassAndMethodMatcher pc : (ExtensionClassAndMethodMatcher[]) list.toArray(new ExtensionClassAndMethodMatcher[0])) {
                String msg = MessageFormat.format("Extension instrumentation point: {0} {1}",
                                                         new Object[] {pc.getClassMatcher(), pc.getMethodMatcher()});

                Agent.LOG.finest(msg);
            }
        }
        return list;
    }

    private static Collection<ExtensionClassAndMethodMatcher> addInstrumentation(Extension ext, Map instrumentation) {
        try {
            Map instrumentationMap = instrumentation;
            defaultMetricPrefix = (String) instrumentationMap.get("metric_prefix");
            defaultMetricPrefix = defaultMetricPrefix == null ? "Custom" : defaultMetricPrefix;
            Object pcConfig = instrumentationMap.get("pointcuts");

            PointCutFactory pcFactory = new PointCutFactory(ext.getClassLoader(), defaultMetricPrefix, ext.getName());
            return pcFactory.getPointCuts(pcConfig);
        } catch (Throwable t) {
            String msg = MessageFormat.format("An error occurred reading the pointcuts in extension {0} : {1}",
                                                     new Object[] {ext.getName(), t.toString()});

            Agent.LOG.severe(msg);
            Agent.LOG.log(Level.FINER, msg, t);
        }
        return Collections.emptyList();
    }

    private void initYaml() {
        Constructor constructor = new InstrumentationConstructor();
        Loader loader = new Loader(constructor);
        yaml = new Yaml(loader);
    }

    private void loadYaml(InputStream input) throws ParseException {
        if (null == input) {
            return;
        }
        Object config = yaml.load(input);
        PointCutFactory pcFactory = new PointCutFactory(getClass().getClassLoader(), "Custom", "CustomYaml");

        pcList.addAll(pcFactory.getPointCuts(config));
    }

    public List<ExtensionClassAndMethodMatcher> getPointCuts() {
        return pcList;
    }
}