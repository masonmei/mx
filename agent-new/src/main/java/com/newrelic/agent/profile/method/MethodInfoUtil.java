//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.profile.method;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.newrelic.agent.profile.MethodLineNumberMatcher;
import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.deps.com.google.common.collect.Sets;
import com.newrelic.deps.org.objectweb.asm.Type;

public class MethodInfoUtil {
    public MethodInfoUtil() {
    }

    public static MethodInfo createMethodInfo(Class<?> declaringClass, String methodName, int lineNumber) {
        String methodDesc = MethodLineNumberMatcher.getMethodDescription(declaringClass, methodName, lineNumber);
        return getMethodInfo(declaringClass, methodName, methodDesc);
    }

    protected static MethodInfo getMethodInfo(Class<?> declaringClass, String methodName, String methodDesc) {
        if (methodDesc == null) {
            return handleNoMethodDesc(declaringClass, methodName);
        } else {
            List args = getArguments(methodDesc);
            return isConstructor(methodName) ? handleConstructor(declaringClass, methodName, methodDesc, args)
                           : handleMethod(declaringClass, methodName, methodDesc, args);
        }
    }

    private static MethodInfo handleMethod(Class<?> declaringClass, String methodName, String methodDesc,
                                           List<String> args) {
        ArrayList members = Lists.newArrayList();
        return (MethodInfo) (getMethod(members, declaringClass, methodName, args) ? new ExactMethodInfo(args,
                                                                                                               (Member) members.get(0))
                                     : new MultipleMethodInfo(Sets.newHashSet(members)));
    }

    private static MethodInfo handleConstructor(Class<?> declaringClass, String methodName, String methodDesc,
                                                List<String> args) {
        ArrayList members = Lists.newArrayList();
        return (MethodInfo) (getConstructor(members, declaringClass, methodName, args) ? new ExactMethodInfo(args,
                                                                                                                    (Member) members.get(0))
                                     : new MultipleMethodInfo(Sets.newHashSet(members)));
    }

    private static MethodInfo handleNoMethodDesc(Class<?> declaringClass, String methodName) {
        return isConstructor(methodName) ? new MultipleMethodInfo(Sets.<Member>newHashSet(declaringClass
                                                                                                  .getDeclaredConstructors()))
                       : new MultipleMethodInfo(getMatchingMethods(declaringClass, methodName));
    }

    protected static List<String> getArguments(Member m) {
        ArrayList paramClasses = Lists.newArrayList();
        Class[] params;
        if (m instanceof Method) {
            params = ((Method) m).getParameterTypes();
        } else if (m instanceof Constructor) {
            params = ((Constructor) m).getParameterTypes();
        } else {
            params = new Class[0];
        }

        Class[] arr$ = params;
        int len$ = params.length;

        for (int i$ = 0; i$ < len$; ++i$) {
            Class clazz = arr$[i$];
            paramClasses.add(clazz.getCanonicalName());
        }

        return paramClasses;
    }

    protected static List<String> getArguments(String methodDesc) {
        Type[] types = Type.getArgumentTypes(methodDesc);
        ArrayList args = Lists.newArrayListWithCapacity(types.length);
        Type[] arr$ = types;
        int len$ = types.length;

        for (int i$ = 0; i$ < len$; ++i$) {
            Type current = arr$[i$];
            args.add(current.getClassName());
        }

        return args;
    }

    private static boolean isConstructor(String methodName) {
        return methodName.startsWith("<");
    }

    private static boolean getConstructor(List<Member> addToHere, Class<?> declaringClass, String constName,
                                          List<String> arguments) {
        Constructor[] arr$ = declaringClass.getDeclaredConstructors();
        int len$ = arr$.length;

        for (int i$ = 0; i$ < len$; ++i$) {
            Constructor constructor = arr$[i$];
            addToHere.add(constructor);
            Class[] params = constructor.getParameterTypes();
            if (params.length == arguments.size()) {
                boolean matches = true;

                for (int i = 0; i < params.length; ++i) {
                    if (!((String) arguments.get(i)).equals(params[i].getCanonicalName())) {
                        matches = false;
                        break;
                    }
                }

                if (matches) {
                    addToHere.clear();
                    addToHere.add(constructor);
                    return true;
                }
            }
        }

        return false;
    }

    protected static boolean getMethod(List<Member> addToHere, Class<?> declaringClass, String methodName,
                                       List<String> arguments) {
        Method[] arr$ = declaringClass.getDeclaredMethods();
        int len$ = arr$.length;

        for (int i$ = 0; i$ < len$; ++i$) {
            Method method = arr$[i$];
            if (methodName.equals(method.getName())) {
                addToHere.add(method);
                Class[] params = method.getParameterTypes();
                if (params.length == arguments.size()) {
                    boolean matches = true;

                    for (int i = 0; i < params.length; ++i) {
                        if (!((String) arguments.get(i)).equals(params[i].getCanonicalName())) {
                            matches = false;
                            break;
                        }
                    }

                    if (matches) {
                        addToHere.clear();
                        addToHere.add(method);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    protected static Set<Member> getMatchingMethods(Class<?> declaringClass, String methodName) {
        HashSet methods = Sets.newHashSet();
        Method[] arr$ = declaringClass.getDeclaredMethods();
        int len$ = arr$.length;

        for (int i$ = 0; i$ < len$; ++i$) {
            Method method = arr$[i$];
            if (methodName.equals(method.getName())) {
                methods.add(method);
            }
        }

        return methods;
    }
}
