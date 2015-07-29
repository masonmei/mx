//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.weaver;

import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.logging.Level;

import com.newrelic.deps.org.objectweb.asm.Type;

import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.deps.com.google.common.collect.Sets;
import com.newrelic.agent.Agent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.JarUtils;
import com.newrelic.agent.util.asm.Utils;
import com.newrelic.bootstrap.BootstrapAgent;

abstract class ClassAppender {
    ClassAppender() {
    }

    public static ClassAppender getBootstrapClassAppender(final Instrumentation instrumentation) {
        return new ClassAppender() {
            public void appendClasses(ClassLoader loader, Map<String, byte[]> classBytesMap, List<String> loadOrderHint)
                    throws IOException {
                if (Agent.LOG.isFinestEnabled()) {
                    Iterator i$ = classBytesMap.entrySet().iterator();

                    while (i$.hasNext()) {
                        Entry entry = (Entry) i$.next();
                        Agent.LOG.log(Level.FINEST, "Appending \'{0}\' to bootstrap class loader",
                                             new Object[] {entry.getKey()});
                    }
                }

                instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(JarUtils.createJarFile("instrumentation",
                                                                                                             classBytesMap)));
            }
        };
    }

    public static ClassAppender getSystemClassAppender() {
        final Method defineClassMethod;
        try {
            defineClassMethod = ClassLoader.class.getDeclaredMethod("defineClass",
                                                                           new Class[] {String.class, byte[].class,
                                                                                               Integer.TYPE,
                                                                                               Integer.TYPE,
                                                                                               ProtectionDomain.class});
            defineClassMethod.setAccessible(true);
        } catch (Exception var2) {
            throw new RuntimeException(var2);
        }

        return new ClassAppender() {
            public void appendClasses(ClassLoader loader, Map<String, byte[]> classBytesMap, List<String> loadOrder)
                    throws IOException {
                List existingClasses = this.loadClassesInTopologicalOrder(loader, classBytesMap, loadOrder);
                if (!existingClasses.isEmpty() && ServiceFactory.getAgent().getInstrumentation()
                                                          .isRedefineClassesSupported()) {
                    try {
                        Agent.LOG.log(Level.FINEST, "Trying to redefine {0} classes: {1}",
                                             new Object[] {Integer.valueOf(existingClasses.size()), existingClasses});
                        ServiceFactory.getAgent().getInstrumentation()
                                .redefineClasses((ClassDefinition[]) existingClasses.toArray(new ClassDefinition[0]));
                    } catch (Exception var6) {
                        throw new IOException(var6);
                    }
                }

            }

            private List<ClassDefinition> loadClassesInTopologicalOrder(ClassLoader loader,
                                                                        Map<String, byte[]> classBytesMap,
                                                                        List<String> loadOrderHint) throws IOException {
                ProtectionDomain protectionDomain = BootstrapAgent.class.getProtectionDomain();
                ArrayList existingClasses = Lists.newArrayList();
                boolean continueLoading = true;
                HashMap loadedClasses = Maps.newHashMap();
                Object unloadedClasses = Sets.newLinkedHashSet(loadOrderHint);
                if (classBytesMap.size() != ((Set) unloadedClasses).size()) {
                    Agent.LOG.log(Level.FINEST, "loadOrderHint ( size {0} ) differs from classBytesMap ( size {1} )",
                                         new Object[] {Integer.valueOf(((Set) unloadedClasses).size()),
                                                              Integer.valueOf(classBytesMap.size())});
                    unloadedClasses = Sets.newHashSet(classBytesMap.keySet());
                }

                for (; continueLoading && ((Set) unloadedClasses).size() > 0;
                     ((Set) unloadedClasses).removeAll(loadedClasses.keySet())) {
                    continueLoading = false;
                    Iterator i$ = ((Set) unloadedClasses).iterator();

                    while (i$.hasNext()) {
                        String classname = (String) i$.next();
                        byte[] classBytes = (byte[]) classBytesMap.get(classname);
                        if (null == classBytes) {
                            Agent.LOG.log(Level.FINEST, "Class in loadOrderHint {0} was not found in classBytesMap",
                                                 new Object[] {classname});
                            unloadedClasses = Sets.newHashSet(classBytesMap.keySet());
                            continueLoading = true;
                            break;
                        }

                        try {
                            defineClassMethod.invoke(loader, new Object[] {classname.replace('/', '.'), classBytes,
                                                                                  Integer.valueOf(0),
                                                                                  Integer.valueOf(classBytes.length),
                                                                                  protectionDomain});
                            continueLoading = true;
                            loadedClasses.put(classname, classBytes);
                        } catch (Exception var16) {
                            if (Agent.isDebugEnabled()) {
                                Utils.print(classBytes);
                            }

                            if (!(var16.getCause() instanceof LinkageError)) {
                                throw new IOException(var16);
                            }

                            String errorMessage = var16.getCause().getMessage();
                            if (errorMessage != null && errorMessage
                                                                .contains("attempted  duplicate class definition")) {
                                try {
                                    Agent.LOG.log(Level.FINEST, "attempted to append existing class {0}",
                                                         new Object[] {classname});
                                    Class e1 = loader.loadClass(Type.getObjectType(classname).getClassName());
                                    existingClasses.add(new ClassDefinition(e1, classBytes));
                                } catch (ClassNotFoundException var15) {
                                    Agent.LOG.log(Level.FINEST, var15, var15.getMessage(), new Object[0]);
                                    continue;
                                }
                            }

                            Agent.LOG.log(Level.FINEST, "Could not resolve {0} due to {1}",
                                                 new Object[] {classname, var16.getCause()});
                        }
                    }
                }

                if (((Set) unloadedClasses).size() > 0) {
                    Agent.LOG.log(Level.FINEST, "Error resolving {0} classes: {1}",
                                         new Object[] {Integer.valueOf(((Set) unloadedClasses).size()),
                                                              unloadedClasses});
                }

                return existingClasses;
            }
        };
    }

    public abstract void appendClasses(ClassLoader var1, Map<String, byte[]> var2, List<String> var3)
            throws IOException;
}
