//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.extension.util;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.agent.Agent;
import com.newrelic.agent.extension.beans.Extension;
import com.newrelic.agent.extension.beans.Extension.Instrumentation;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.ClassName;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.Method;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.Method.Parameters.Type;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.classmatchers.AllClassesMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ChildClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.AnnotationMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.tracing.ParameterAttributeName;

public final class ExtensionConversionUtility {
    public static final String DEFAULT_CONFIG_DIRECTORY = "extensions";

    private ExtensionConversionUtility() {
    }

    public static void validateExtensionAttributes(Extension extension) throws XmlException {
        if (extension == null) {
            throw new XmlException("There must be an extension to instrument new methods.\n");
        } else if (extension.getName() != null && extension.getName().length() != 0) {
            if (extension.getVersion() < 0.0D) {
                throw new XmlException(" The version number must be a double and must be greater than or equal to 0"
                                               + ".\n");
            }
        } else {
            throw new XmlException("The extension must have a name attribute.\n");
        }
    }

    private static void validateInstrument(Instrumentation instrument) throws XmlException {
        if (instrument == null) {
            throw new XmlException("In order to provide instrumentation, there must be an instrument tag.\n");
        } else {
            List pcs = instrument.getPointcut();
            if (pcs == null || pcs.isEmpty()) {
                throw new XmlException("A point cut tag is required to instrument a method.\n");
            }
        }
    }

    public static List<ExtensionClassAndMethodMatcher> convertToPointCutsForValidation(Extension ext)
            throws XmlException {
        ArrayList pointCutsOut = new ArrayList();
        Instrumentation inst = ext.getInstrumentation();
        validateExtensionAttributes(ext);
        validateInstrument(inst);
        List pcs = inst.getPointcut();
        String defaultMetricPrefix = createDefaultMetricPrefix(inst, true);
        HashMap classesToMethods = new HashMap();
        Iterator i$ = pcs.iterator();

        while (i$.hasNext()) {
            Pointcut pc = (Pointcut) i$.next();
            pointCutsOut.add(createPointCut(ext, pc, defaultMetricPrefix, ext.getName(), classesToMethods, true,
                                                   InstrumentationType.LocalCustomXml, false));
        }

        return pointCutsOut;
    }

    public static List<ExtensionClassAndMethodMatcher> convertToEnabledPointCuts(Collection<Extension> extensions,
                                                                                 boolean custom,
                                                                                 InstrumentationType type) {
        return convertToEnabledPointCuts(extensions, custom, type, true);
    }

    public static List<ExtensionClassAndMethodMatcher> convertToEnabledPointCuts(Collection<Extension> extensions,
                                                                                 boolean custom,
                                                                                 InstrumentationType type,
                                                                                 boolean isAttsEnabled) {
        ArrayList pointCutsOut = new ArrayList();
        if (extensions != null) {
            HashMap classesToMethods = new HashMap();
            Iterator i$ = extensions.iterator();

            while (i$.hasNext()) {
                Extension ext = (Extension) i$.next();
                if (ext.isEnabled()) {
                    pointCutsOut.addAll(convertToEnabledPointCuts(ext, ext.getName(), classesToMethods, custom, type,
                                                                         isAttsEnabled));
                } else {
                    Agent.LOG.log(Level.WARNING, MessageFormat.format("Extension {0} is not enabled and so will not be "
                                                                              + "instrumented.",
                                                                             new Object[] {ext.getName()}));
                }
            }
        }

        return pointCutsOut;
    }

    private static List<ExtensionClassAndMethodMatcher> convertToEnabledPointCuts(Extension extension,
                                                                                  String extensionName,
                                                                                  Map<String, MethodMapper>
                                                                                          classesToMethods,
                                                                                  boolean custom,
                                                                                  InstrumentationType type,
                                                                                  boolean isAttsEnabled) {
        ArrayList pointCutsOut = new ArrayList();
        if (extension != null) {
            String defaultMetricPrefix = createDefaultMetricPrefix(extension.getInstrumentation(), custom);
            List inCuts = extension.getInstrumentation().getPointcut();
            if (inCuts != null && !inCuts.isEmpty()) {
                Iterator msg2 = inCuts.iterator();

                while (msg2.hasNext()) {
                    Pointcut cut = (Pointcut) msg2.next();

                    try {
                        ExtensionClassAndMethodMatcher e =
                                createPointCut(extension, cut, defaultMetricPrefix, extensionName, classesToMethods,
                                                      custom, type, isAttsEnabled);
                        if (e != null) {
                            logPointCutCreation(e);
                            pointCutsOut.add(e);
                        }
                    } catch (Exception var13) {
                        String msg1 = MessageFormat
                                              .format("An error occurred reading in a pointcut in extension {0} : {1}",
                                                             new Object[] {extensionName, var13.toString()});
                        Agent.LOG.log(Level.SEVERE, msg1);
                        Agent.LOG.log(Level.FINER, msg1, var13);
                    }
                }
            } else {
                String msg = MessageFormat.format("There were no point cuts in the extension {0}.",
                                                         new Object[] {extensionName});
                Agent.LOG.log(Level.INFO, msg);
            }
        }

        return pointCutsOut;
    }

    private static String createDefaultMetricPrefix(Instrumentation instrument, boolean custom) {
        String metricPrefix = custom ? "Custom" : "Java";
        if (instrument != null) {
            String prefix = instrument.getMetricPrefix();
            if (prefix != null && prefix.length() != 0) {
                metricPrefix = prefix;
            }
        }

        return metricPrefix;
    }

    private static void logPointCutCreation(ExtensionClassAndMethodMatcher pc) {
        String msg = MessageFormat.format("Extension instrumentation point: {0} {1}",
                                                 new Object[] {pc.getClassMatcher(), pc.getMethodMatcher()});
        Agent.LOG.finest(msg);
    }

    private static ExtensionClassAndMethodMatcher createPointCut(Extension extension, Pointcut cut, String metricPrefix,
                                                                 String pName,
                                                                 Map<String, MethodMapper> classesToMethods,
                                                                 boolean custom, InstrumentationType type,
                                                                 boolean isAttsEnabled) throws XmlException {
        Object classMatcher;
        if (cut.getMethodAnnotation() != null) {
            classMatcher = new AllClassesMatcher();
        } else {
            classMatcher = createClassMatcher(cut, pName);
        }

        MethodMatcher methodMatcher = createMethodMatcher(cut, pName, classesToMethods);
        Object reportedParams = null;
        if (!isAttsEnabled) {
            reportedParams = Lists.newArrayList();
        } else {
            reportedParams = getParameterAttributeNames(cut.getMethod());
        }

        return new ExtensionClassAndMethodMatcher(extension, cut, metricPrefix, (ClassMatcher) classMatcher,
                                                         methodMatcher, custom, (List) reportedParams, type);
    }

    private static List<ParameterAttributeName> getParameterAttributeNames(List<Method> methods) {
        ArrayList reportedParams = Lists.newArrayList();
        Iterator i$ = methods.iterator();

        while (true) {
            Method m;
            do {
                do {
                    if (!i$.hasNext()) {
                        return reportedParams;
                    }

                    m = (Method) i$.next();
                } while (m.getParameters() == null);
            } while (m.getParameters().getType() == null);

            for (int i = 0; i < m.getParameters().getType().size(); ++i) {
                Type t = (Type) m.getParameters().getType().get(i);
                if (t.getAttributeName() != null) {
                    try {
                        Map<String, MethodMapper> objectObjectHashMap = Maps.newHashMap();
                        MethodMatcher e =
                                MethodMatcherUtility.createMethodMatcher("DummyClassName", m, objectObjectHashMap, "");
                        ParameterAttributeName reportedParam = new ParameterAttributeName(i, t.getAttributeName(), e);
                        reportedParams.add(reportedParam);
                    } catch (Exception var8) {
                        Agent.LOG.log(Level.FINEST, var8, var8.getMessage(), new Object[0]);
                    }
                }
            }
        }
    }

    private static MethodMatcher createMethodMatcher(Pointcut cut, String pExtName,
                                                     Map<String, MethodMapper> classesToMethods) throws XmlException {
        List methods = cut.getMethod();
        if (methods != null && !methods.isEmpty()) {
            return MethodMatcherUtility.createMethodMatcher(getClassName(cut), methods, classesToMethods, pExtName);
        } else if (cut.getMethodAnnotation() != null) {
            return new AnnotationMethodMatcher(com.newrelic.deps.org.objectweb.asm.Type
                                                       .getObjectType(cut.getMethodAnnotation().replace('.', '/')));
        } else {
            throw new XmlException(MessageFormat
                                           .format("At least one method must be specified for each point cut in the "
                                                           + "extension {0}",
                                                          new Object[] {pExtName}));
        }
    }

    static boolean isReturnTypeOkay(com.newrelic.deps.org.objectweb.asm.Type returnType) {
        return returnType.getSort() == 9 ? isReturnTypeOkay(returnType.getElementType()) : returnType.getSort() == 10;
    }

    public static String getClassName(Pointcut cut) {
        return cut.getClassName() != null ? cut.getClassName().getValue().trim()
                       : (cut.getInterfaceName() != null ? cut.getInterfaceName().trim() : null);
    }

    static ClassMatcher createClassMatcher(Pointcut pointcut, String pExtName) throws XmlException {
        ClassName className = pointcut.getClassName();
        if (className != null) {
            if (className.getValue() != null && !className.getValue().isEmpty()) {
                return (ClassMatcher) (className.isIncludeSubclasses() ? new ChildClassMatcher(className.getValue(),
                                                                                                      false)
                                               : new ExactClassMatcher(className.getValue()));
            } else {
                throw new XmlException("");
            }
        } else if (pointcut.getInterfaceName() != null) {
            return new InterfaceMatcher(pointcut.getInterfaceName());
        } else {
            throw new XmlException(MessageFormat
                                           .format("A class name, interface name, or super class name needs to be "
                                                           + "specified for every point cut in the extension {0}",
                                                          new Object[] {pExtName}));
        }
    }
}
