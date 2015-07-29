package com.newrelic.agent.instrumentation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.logging.IAgentLogger;

public class ClassNameFilter {
    private static final String EXCLUDES_FILE = "/META-INF/excludes";
    private final List<Pattern> excludePatterns = new LinkedList<Pattern>();
    private final List<Pattern> includePatterns = new LinkedList<Pattern>();
    private final IAgentLogger logger;
    private volatile Set<String> includeClasses = new HashSet<String>();

    public ClassNameFilter(IAgentLogger logger) {
        this.logger = logger;
    }

    public boolean isExcluded(String className) {
        for (Pattern pattern : excludePatterns) {
            if (pattern.matcher(className).matches()) {
                return true;
            }
        }
        return false;
    }

    public boolean isIncluded(String className) {
        if (includeClasses.contains(className)) {
            return true;
        }
        for (Pattern pattern : includePatterns) {
            if (pattern.matcher(className).matches()) {
                return true;
            }
        }
        return false;
    }

    public void addConfigClassFilters(AgentConfig config) {
        Set<String> excludes = config.getClassTransformerConfig().getExcludes();
        if (excludes.size() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("Exclude class name filters:");
            for (String exclude : excludes) {
                sb.append("\n").append(exclude);
                addExclude(exclude);
            }
            logger.finer(sb.toString());
        }
        Set<String> includes = config.getClassTransformerConfig().getIncludes();
        for (String include : includes) {
            addInclude(include);
        }
    }

    public void addExcludeFileClassFilters() {
        InputStream iStream = getClass().getResourceAsStream(EXCLUDES_FILE);
        BufferedReader reader = new BufferedReader(new InputStreamReader(iStream));
        List<String> excludeList = new LinkedList<String>();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#")) {
                    excludeList.add(line);
                }
            }
        } catch (IOException ex) {
            logger.severe(MessageFormat
                                  .format("Unable to read the class excludes file at {0} found within the New Relic "
                                                  + "jar.", EXCLUDES_FILE));
        } finally {
            try {
                iStream.close();
            } catch (IOException e) {
            }
        }
        for (String exclude : excludeList) {
            addExclude(exclude);
        }
        logger.finer("Excludes initialized: " + excludeList);
    }

    public void addInclude(String include) {
        if (isRegex(include)) {
            addIncludeRegex(include);
        } else {
            addIncludeClass(include);
        }
    }

    public void addIncludeClass(String className) {
        String regex = classNameToRegex(className);
        addIncludeRegex(regex);
    }

    public void addIncludeRegex(String regex) {
        Pattern pattern = regexToPattern(regex);
        if (pattern != null) {
            includePatterns.add(pattern);
        }
    }

    public void addExclude(String exclude) {
        if (isRegex(exclude)) {
            addExcludeRegex(exclude);
        } else {
            addExcludeClass(exclude);
        }
    }

    public void addExcludeClass(String className) {
        String regex = classNameToRegex(className);
        addExcludeRegex(regex);
    }

    public void addExcludeRegex(String regex) {
        Pattern pattern = regexToPattern(regex);
        if (pattern != null) {
            excludePatterns.add(pattern);
        }
    }

    private String classNameToRegex(String className) {
        return "^" + className.replace("$", "\\$") + "$";
    }

    private Pattern regexToPattern(String regex) {
        try {
            return Pattern.compile(regex);
        } catch (Exception e) {
            logger.severe(MessageFormat.format("Unable to compile pattern: {0}", regex));
        }
        return null;
    }

    private boolean isRegex(String value) {
        return (value.indexOf('*') >= 0) || (value.indexOf('|') >= 0) || (value.indexOf('^') >= 0);
    }

    public void addClassMatcherIncludes(Collection<ClassMatcher> classMatchers) {
        Set<String> classNames = new HashSet<String>();
        classNames.addAll(includeClasses);

        for (ClassMatcher classMatcher : classMatchers) {
            for (String className : classMatcher.getClassNames()) {
                classNames.add(className);
            }
        }
        logger.finer("Class name inclusions: " + classNames);
        includeClasses = classNames;
    }
}