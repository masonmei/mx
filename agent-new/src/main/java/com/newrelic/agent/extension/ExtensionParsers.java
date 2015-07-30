package com.newrelic.agent.extension;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.newrelic.agent.extension.dom.ExtensionDomParser;
import com.newrelic.deps.org.yaml.snakeyaml.Loader;
import com.newrelic.deps.org.yaml.snakeyaml.Yaml;
import com.newrelic.deps.org.yaml.snakeyaml.constructor.Constructor;

public class ExtensionParsers {
    private final ExtensionParser yamlParser;
    private final ExtensionParser xmlParser;

    public ExtensionParsers(final List<ConfigurationConstruct> constructs) {
        Constructor constructor = new Constructor() {
            {
                for (ConfigurationConstruct construct : constructs) {
                    this.yamlConstructors.put(construct.getName(), construct);
                }
            }
        };
        Loader loader = new Loader(constructor);
        final Yaml yaml = new Yaml(loader);

        this.yamlParser = new ExtensionParser() {
            public Extension parse(ClassLoader classloader, InputStream inputStream, boolean custom) throws Exception {
                Object config = yaml.load(inputStream);
                if ((config instanceof Map)) {
                    return new YamlExtension(classloader, (Map) config, custom);
                }
                throw new Exception("Invalid yaml extension");
            }
        };
        this.xmlParser = new ExtensionParser() {
            public Extension parse(ClassLoader classloader, InputStream inputStream, boolean custom) throws Exception {
                com.newrelic.agent.extension.beans.Extension ext = ExtensionDomParser.readFile(inputStream);
                return new XmlExtension(getClass().getClassLoader(), ext.getName(), ext, custom);
            }
        };
    }

    public ExtensionParser getParser(String fileName) {
        if (fileName.endsWith(".yml")) {
            return this.yamlParser;
        }
        return this.xmlParser;
    }

    public ExtensionParser getXmlParser() {
        return this.xmlParser;
    }

    public ExtensionParser getYamlParser() {
        return this.yamlParser;
    }

    public interface ExtensionParser {
        Extension parse(ClassLoader paramClassLoader, InputStream paramInputStream, boolean paramBoolean)
                throws Exception;
    }
}