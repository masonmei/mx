package com.newrelic.agent.extension;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Loader;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.newrelic.agent.extension.dom.ExtensionDomParser;

public class ExtensionParsers {
    private final ExtensionParser yamlParser;
    private final ExtensionParser xmlParser;

    public ExtensionParsers(final List<ConfigurationConstruct> constructs) {
        Constructor constructor = new Constructor() {
        };
        Loader loader = new Loader(constructor);
        final Yaml yaml = new Yaml(loader);

        yamlParser = new ExtensionParser() {
            public Extension parse(ClassLoader classloader, InputStream inputStream, boolean custom) throws Exception {
                Object config = yaml.load(inputStream);
                if ((config instanceof Map)) {
                    return new YamlExtension(classloader, (Map) config, custom);
                }
                throw new Exception("Invalid yaml extension");
            }
        };
        xmlParser = new ExtensionParser() {
            public Extension parse(ClassLoader classloader, InputStream inputStream, boolean custom) throws Exception {
                com.newrelic.agent.extension.beans.Extension ext = ExtensionDomParser.readFile(inputStream);
                return new XmlExtension(getClass().getClassLoader(), ext.getName(), ext, custom);
            }
        };
    }

    public ExtensionParser getParser(String fileName) {
        if (fileName.endsWith(".yml")) {
            return yamlParser;
        }
        return xmlParser;
    }

    public ExtensionParser getXmlParser() {
        return xmlParser;
    }

    public ExtensionParser getYamlParser() {
        return yamlParser;
    }

    public static abstract interface ExtensionParser {
        public abstract Extension parse(ClassLoader paramClassLoader, InputStream paramInputStream,
                                        boolean paramBoolean) throws Exception;
    }
}