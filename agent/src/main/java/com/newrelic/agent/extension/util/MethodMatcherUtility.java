//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.extension.util;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.newrelic.agent.Agent;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.Method;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.Method.Parameters;
import com.newrelic.agent.extension.beans.MethodParameters;
import com.newrelic.agent.instrumentation.methodmatchers.ExactParamsMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactReturnTypeMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.NameMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.util.asm.Utils;
import com.newrelic.deps.org.objectweb.asm.Type;

public class MethodMatcherUtility {
    public MethodMatcherUtility() {
    }

    public static MethodMatcher createMethodMatcher(String className, List<Method> methods,
                                                    Map<String, MethodMapper> classesToMethods, String extName)
            throws XmlException {
        LinkedList matchers = new LinkedList();
        Iterator i$ = methods.iterator();

        while (i$.hasNext()) {
            Method method = (Method) i$.next();

            try {
                matchers.add(createMethodMatcher(className, method, classesToMethods, extName));
            } catch (NoSuchMethodException var8) {
                Agent.LOG.warning(var8.getMessage());
            }
        }

        if (matchers.size() > 1) {
            return OrMethodMatcher.getMethodMatcher(matchers);
        } else if (matchers.size() == 1) {
            return (MethodMatcher) matchers.get(0);
        } else {
            throw new XmlException("All methods for " + className + " have already been added.");
        }
    }

    public static MethodMatcher createMethodMatcher(String className, Method method,
                                                    Map<String, MethodMapper> classesToMethods, String extName)
            throws NoSuchMethodException, XmlException {
        if (method == null) {
            throw new XmlException("A method must be specified for a point cut in the extension.");
        } else if (method.getReturnType() != null) {
            if (Utils.isPrimitiveType(method.getReturnType())) {
                throw new XmlException("The return type \'" + method.getReturnType()
                                               + "\' is not valid.  Primitive types are not allowed.");
            } else {
                Type methodName1 = Type.getObjectType(method.getReturnType().replace('.', '/'));
                if (!ExtensionConversionUtility.isReturnTypeOkay(methodName1)) {
                    throw new XmlException("The return type \'" + methodName1.getClassName()
                                                   + "\' is not valid.  Primitive types are not allowed.");
                } else {
                    return new ExactReturnTypeMethodMatcher(methodName1);
                }
            }
        } else {
            validateMethod(method, extName);
            String methodName = method.getName();
            if (methodName == null) {
                throw new XmlException("A method name must be specified for a point cut in the extension.");
            } else {
                methodName = methodName.trim();
                if (methodName.length() == 0) {
                    throw new XmlException("A method must be specified for a point cut in the extension.");
                } else {
                    Parameters mParams = method.getParameters();
                    if (mParams != null && mParams.getType() != null) {
                        String descriptor = MethodParameters.getDescriptor(mParams);
                        if (descriptor == null) {
                            throw new XmlException("Descriptor not being calculated correctly.");
                        } else {
                            String mDescriptor = descriptor.trim();
                            if (!isDuplicateMethod(className, methodName, mDescriptor, classesToMethods)) {
                                return ExactParamsMethodMatcher
                                               .createExactParamsMethodMatcher(methodName, descriptor, className);
                            } else {
                                throw new NoSuchMethodException("Method " + methodName
                                                                        + " has already been added to a point cut and"
                                                                        + " will " + "not be added again.");
                            }
                        }
                    } else if (!isDuplicateMethod(className, methodName, (String) null, classesToMethods)) {
                        return new NameMethodMatcher(methodName);
                    } else {
                        throw new NoSuchMethodException("Method " + methodName
                                                                + " has already been added to a point cut and will "
                                                                + "not be added again.");
                    }
                }
            }
        }
    }

    private static void validateMethod(Method m, String extName) throws XmlException {
        if (m == null) {
            throw new XmlException(MessageFormat
                                           .format("At least one method must be specified for each point cut in the "
                                                           + "extension {0}", new Object[] {extName}));
        } else {
            String mName = m.getName();
            if (mName == null || mName.trim().length() == 0) {
                throw new XmlException(MessageFormat
                                               .format("A method name must be specified for each method in the "
                                                               + "extension {0}",
                                                              new Object[] {extName}));
            }
        }
    }

    private static boolean isDuplicateMethod(String className, String methodName, String descriptor,
                                             Map<String, MethodMapper> classesToMethods) {
        if (className != null) {
            String name = Type.getObjectType(className).getClassName();
            MethodMapper mapper = (MethodMapper) classesToMethods.get(name);
            if (mapper == null) {
                mapper = new MethodMapper();
                classesToMethods.put(className, mapper);
            }

            return !mapper.addIfNotPresent(methodName, descriptor);
        } else {
            return true;
        }
    }
}
