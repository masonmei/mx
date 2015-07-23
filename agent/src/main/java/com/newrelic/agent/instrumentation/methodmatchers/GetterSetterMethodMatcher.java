package com.newrelic.agent.instrumentation.methodmatchers;

import java.util.Set;
import java.util.regex.Pattern;

import org.objectweb.asm.commons.Method;

public class GetterSetterMethodMatcher implements MethodMatcher {
    static final Pattern GETTER_METHOD_PATTERN = Pattern.compile("^get[A-Z][a-zA-Z0-9_]*$");
    static final Pattern IS_METHOD_PATTERN = Pattern.compile("^is[A-Z][a-zA-Z0-9_]*$");
    static final Pattern SETTER_METHOD_PATTERN = Pattern.compile("^set[A-Z][a-zA-Z0-9_]*$");
    static final Pattern GETTER_DESCRIPTION_PATTERN = Pattern.compile("^\\(\\)[^V].*$");
    static final Pattern IS_DESCRIPTION_PATTERN = Pattern.compile("^\\(\\)(?:Z|Ljava/lang/Boolean;)$");
    static final Pattern SETTER_DESCRIPTION_PATTERN = Pattern.compile("^\\(\\[?[A-Z][a-zA-Z0-9_/;]*\\)V$");
    private static GetterSetterMethodMatcher matcher = new GetterSetterMethodMatcher();

    public static GetterSetterMethodMatcher getGetterSetterMethodMatcher() {
        return matcher;
    }

    public boolean matches(int access, String name, String desc, Set<String> annotations) {
        if (GETTER_METHOD_PATTERN.matcher(name).matches()) {
            return GETTER_DESCRIPTION_PATTERN.matcher(desc).matches();
        }
        if (IS_METHOD_PATTERN.matcher(name).matches()) {
            return IS_DESCRIPTION_PATTERN.matcher(desc).matches();
        }
        if (SETTER_METHOD_PATTERN.matcher(name).matches()) {
            return SETTER_DESCRIPTION_PATTERN.matcher(desc).matches();
        }
        return false;
    }

    public Method[] getExactMethods() {
        return null;
    }

    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    public int hashCode() {
        return super.hashCode();
    }
}