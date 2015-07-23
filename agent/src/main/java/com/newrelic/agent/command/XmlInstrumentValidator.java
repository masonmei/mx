package com.newrelic.agent.command;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.objectweb.asm.Type;
import org.xml.sax.SAXException;

import com.newrelic.agent.extension.beans.Extension;
import com.newrelic.agent.extension.dom.ExtensionDomParser;
import com.newrelic.agent.extension.util.ExtensionConversionUtility;
import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;

public class XmlInstrumentValidator {
    public static void validateInstrumentation(CommandLine cmd) {
        XmlInstrumentParams params = new XmlInstrumentParams();
        if (cmd == null) {
            printMessage("There were no command line parameters.");
        } else {
            try {
                if (verifyAndSetParameters(cmd, params)) {
                    validateInstrumentation(params);
                    printMessage(MessageFormat.format("PASS: The extension at {0} was successfully validated.",
                                                             new Object[] {params.getFile().getAbsoluteFile()}));
                }
            } catch (IOException e) {
                printMessage(MessageFormat
                                     .format("FAIL: The extension at {0} failed validation. \nReason: {1} \nNOTE: set"
                                                     + " debug to true for more information.",
                                                    new Object[] {params.getFile().getAbsoluteFile(), e.getMessage()}));
            } catch (SAXException e) {
                printMessage(MessageFormat
                                     .format("FAIL: The extension at {0} failed validation. \nReason: {1} \nNOTE: set"
                                                     + " debug to true for more information.",
                                                    new Object[] {params.getFile().getAbsoluteFile(), e.getMessage()}));
            } catch (IllegalArgumentException e) {
                File file = params.getFile();
                String fileName = null;
                if (file != null) {
                    fileName = file.getAbsolutePath();
                }
                printMessage(MessageFormat
                                     .format("FAIL: The extension at {0} failed validation. \n Reason: {1} \n Note: "
                                                     + "Set debug to true for more information.",
                                                    new Object[] {fileName, e.getMessage()}));
            } catch (RuntimeException e) {
                printMessage(MessageFormat
                                     .format("FAIL: The extension at {0} failed validation. \n Reason: {1} \n Note: "
                                                     + "Set debug to true for more information.",
                                                    new Object[] {params.getFile().getAbsoluteFile(), e.getMessage()}));
            } catch (ClassNotFoundException e) {
                printMessage(MessageFormat
                                     .format("FAIL: The extension at {0} failed validation. \n Reason: The following "
                                                     + "class was not found: {1} \n Note: Set debug to true for more "
                                                     + "information.",
                                                    new Object[] {params.getFile().getAbsoluteFile(), e.getMessage()}));
            } catch (Exception e) {
                printMessage(MessageFormat
                                     .format("FAIL: The extension at {0} failed validation. \n Reason: {1} \n Note: "
                                                     + "Set debug to true for more information.",
                                                    new Object[] {params.getFile().getAbsoluteFile(), e.getMessage()}));
            }
        }
    }

    protected static void validateInstrumentation(XmlInstrumentParams params) throws Exception {
        Extension extension = ExtensionDomParser.readFile(params.getFile());

        if (params.isDebug()) {
            System.out.println("Xml was successfully read. Starting processing.");
        }

        List convertedPcs = ExtensionConversionUtility.convertToPointCutsForValidation(extension);

        Extension.Instrumentation inst = extension.getInstrumentation();

        if (inst == null) {
            throw new RuntimeException("The instrumentation propery must be set for the extension.");
        }

        List origPcs = inst.getPointcut();

        if (convertedPcs.size() != origPcs.size()) {
            throw new IllegalArgumentException("The processed number of point cuts does not match theoriginal number "
                                                       + "of point cuts in the xml. Remove duplicates.");
        }

        for (int i = 0; i < convertedPcs.size(); i++) {
            MethodHolder holder = sortData((Extension.Instrumentation.Pointcut) origPcs.get(i), params.isDebug());
            verifyPointCut((ExtensionClassAndMethodMatcher) convertedPcs.get(i), holder);
            verifyAllMethodsAccounted(holder);
        }
    }

    private static boolean verifyAndSetParameters(CommandLine cmd, XmlInstrumentParams params)
            throws IllegalArgumentException {
        try {
            XmlInstrumentOptions[] options = XmlInstrumentOptions.values();
            for (XmlInstrumentOptions ops : options) {
                ops.validateAndAddParameter(params, cmd.getOptionValues(ops.getFlagName()), ops.getFlagName());
            }

            return true;
        } catch (Exception e) {
            printMessage(MessageFormat.format("FAIL: The command line parameters are invalid. \n Reason: {0}",
                                                     new Object[] {e.getMessage()}));
        }
        return false;
    }

    private static void verifyAllMethodsAccounted(MethodHolder originals) {
        if (originals.hasMethods()) {
            throw new IllegalArgumentException(MessageFormat
                                                       .format("These methods are either duplicates, constructors, or"
                                                                       + " are not present in the class: {0}",
                                                                      new Object[] {originals.getCurrentMethods()}));
        }
    }

    private static void verifyPointCut(ExtensionClassAndMethodMatcher cut, MethodHolder origMethods)
            throws ClassNotFoundException {
        if (cut != null) {
            Collection<String> classNames = cut.getClassMatcher().getClassNames();

            for (String name : classNames) {
                String nameDoted = name.replace("/", ".").trim();
                Class theClass = Thread.currentThread().getContextClassLoader().loadClass(nameDoted);
                validateNoInterface(theClass);

                checkMethods(cut.getMethodMatcher(), theClass.getDeclaredMethods(), origMethods);
            }
        }
    }

    private static void validateNoInterface(Class theClass) {
        if (theClass.isInterface()) {
            throw new IllegalArgumentException(MessageFormat.format("Only classes can be implemented. This class is an "
                                                                            + "interface: {0}",
                                                                           new Object[] {theClass.getName()}));
        }
    }

    private static void checkMethods(MethodMatcher matcher, Method[] classMethods, MethodHolder origMethods) {
        if (classMethods != null) {
            if (origMethods == null) {
                throw new IllegalArgumentException("Instrumenting a class not found in the XML.");
            }
            for (Method m : classMethods) {
                String currentDesc = Type.getMethodDescriptor(m);

                checkPresenceAndMatcher(m.getName(), currentDesc, matcher, origMethods);
            }
        }
    }

    private static void checkPresenceAndMatcher(String currentName, String currentDesc, MethodMatcher matcher,
                                                MethodHolder origMethods) {
        if (origMethods.isMethodPresent(currentName, currentDesc, true)) {
            if (!matcher.matches(-1, currentName, currentDesc, MethodMatcher.UNSPECIFIED_ANNOTATIONS)) {
                throw new IllegalArgumentException(MessageFormat
                                                           .format("The method was in the point cut but did not match"
                                                                           + " the method matcher. Name: {0} Desc: {1}",
                                                                          new Object[] {currentName, currentDesc}));
            }
        }
    }

    private static MethodHolder sortData(Extension.Instrumentation.Pointcut pc, boolean debug) {
        MethodHolder cMethods = new MethodHolder(debug);
        if (pc != null) {
            cMethods.addMethods(pc.getMethod());
        }

        return cMethods;
    }

    protected static void printMessage(String msg) {
        System.out.println(msg);
    }
}