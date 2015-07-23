//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.classmatchers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;

public final class OptimizedClassMatcher implements ClassMatchVisitorFactory {
    public static final Set<Method> METHODS_WE_NEVER_INSTRUMENT = ImmutableSet.of(new Method("equals",
                                                                                                    "(Ljava/lang/Object;)Z"),
                                                                                         new Method("toString",
                                                                                                           "()Ljava/lang/String;"),
                                                                                         new Method("finalize", "()V"),
                                                                                         new Method("hashCode", "()I"));
    public static final Method DEFAULT_CONSTRUCTOR = new Method("<init>", "()V");
    static final OptimizedClassMatcher EMPTY_MATCHER = new OptimizedClassMatcher();
    static final Supplier<Set<String>> STRING_COLLECTION_SUPPLIER = new Supplier() {
        public Set<String> get() {
            return Sets.newHashSet();
        }
    };
    final Entry<MethodMatcher, ClassAndMethodMatcher>[] methodMatchers;
    final Map<Method, Collection<ClassAndMethodMatcher>> methods;
    final Set<String> methodAnnotationsToMatch;
    Set<String> exactClassNames;

    private OptimizedClassMatcher() {
        this.methodAnnotationsToMatch = ImmutableSet.of();
        this.methodMatchers = new Entry[0];
        this.methods = ImmutableMap.of();
    }

    protected OptimizedClassMatcher(Set<String> annotationMatchers, SetMultimap<Method, ClassAndMethodMatcher> methods,
                                    SetMultimap<MethodMatcher, ClassAndMethodMatcher> methodMatchers,
                                    Set<String> exactClassNames) {
        this.methodAnnotationsToMatch = ImmutableSet.copyOf(annotationMatchers);
        this.methodMatchers = (Entry[]) methodMatchers.entries().toArray(new Entry[0]);
        this.methods = ImmutableMap.copyOf(methods.asMap());
        this.exactClassNames = exactClassNames == null ? null : ImmutableSet.copyOf(exactClassNames);
    }

    public ClassVisitor newClassMatchVisitor(ClassLoader loader, Class<?> classBeingRedefined, ClassReader reader,
                                             ClassVisitor cv, InstrumentationContext context) {
        return this.exactClassNames != null && !this.exactClassNames.contains(reader.getClassName()) ? null
                       : new OptimizedClassMatcher.ClassMethods(loader, reader, classBeingRedefined, cv, context);
    }

    private Multimap<ClassAndMethodMatcher, String> newClassMatches() {
        return Multimaps.newSetMultimap(Maps.<ClassAndMethodMatcher, Collection<String>>newHashMap(),
                                               STRING_COLLECTION_SUPPLIER);
    }

    public String toString() {
        return "OptimizedClassMatcher [methodMatchers=" + Arrays.toString(this.methodMatchers) + ", methods="
                       + this.methods + ", methodAnnotationsToMatch=" + this.methodAnnotationsToMatch
                       + ", exactClassNames=" + this.exactClassNames + "]";
    }

    public static final class Match {
        private final Map<ClassAndMethodMatcher, Collection<String>> classNames;
        private final Set<Method> methods;
        private final Map<Method, Set<String>> methodAnnotations;

        public Match(Multimap<ClassAndMethodMatcher, String> classMatches, Set<Method> methods,
                     Map<Method, Set<String>> methodAnnotations) {
            this.classNames = ImmutableMap.copyOf(classMatches.asMap());
            this.methods = ImmutableSet.copyOf(methods);
            this.methodAnnotations = (Map) (methodAnnotations == null ? ImmutableMap.of() : methodAnnotations);
        }

        public Map<ClassAndMethodMatcher, Collection<String>> getClassMatches() {
            return this.classNames;
        }

        public Set<Method> getMethods() {
            return this.methods;
        }

        public Set<String> getMethodAnnotations(Method method) {
            Set set = (Set) this.methodAnnotations.get(method);
            return (Set) (set == null ? ImmutableSet.of() : set);
        }

        public boolean isClassAndMethodMatch() {
            return !this.methods.isEmpty() && !this.classNames.isEmpty();
        }

        public String toString() {
            return this.classNames.toString() + " methods " + this.methods;
        }
    }

    private class ClassMethods extends ClassVisitor {
        private final Class<?> classBeingRedefined;
        private final ClassReader cr;
        private final ClassLoader loader;
        private final InstrumentationContext context;
        private SetMultimap<Method, ClassAndMethodMatcher> matches;
        private Map<Method, Set<String>> methodAnnotations;
        private Map<ClassMatcher, Boolean> classMatcherMatches;

        private ClassMethods(ClassLoader loader, ClassReader cr, Class<?> classBeingRedefined, ClassVisitor cv,
                             InstrumentationContext context) {
            super(Agent.ASM_LEVEL, cv);
            this.cr = cr;
            this.classBeingRedefined = classBeingRedefined;
            this.loader = loader;
            this.context = context;
        }

        private void addMethodAnnotations(Method method, Set<String> annotations) {
            if (!annotations.isEmpty()) {
                if (this.methodAnnotations == null) {
                    this.methodAnnotations = Maps.newHashMap();
                }

                this.methodAnnotations.put(method, annotations);
            }

        }

        private SetMultimap<Method, ClassAndMethodMatcher> getOrCreateMatches() {
            if (this.matches == null) {
                this.matches = Multimaps.newSetMultimap(Maps.<Method, Collection<ClassAndMethodMatcher>>newHashMap(),
                                                               new Supplier() {
                                                                   public Set<ClassAndMethodMatcher> get() {
                                                                       return Sets.newHashSet();
                                                                   }
                                                               });
            }

            return this.matches;
        }

        private boolean isMatch(ClassMatcher classMatcher, ClassLoader loader, ClassReader cr,
                                Class<?> classBeingRedefined) {
            if (null == this.classMatcherMatches) {
                this.classMatcherMatches = Maps.newHashMap();
            }

            Boolean match = (Boolean) this.classMatcherMatches.get(classMatcher);
            if (match == null) {
                match = Boolean.valueOf(classBeingRedefined == null ? classMatcher.isMatch(loader, cr)
                                                : classMatcher.isMatch(classBeingRedefined));
                this.classMatcherMatches.put(classMatcher, match);
            }

            return match.booleanValue();
        }

        public MethodVisitor visitMethod(final int access, String methodName, String methodDesc, String signature,
                                         String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, methodName, methodDesc, signature, exceptions);
            if ((access & 1024) == 0 && (access & 256) == 0) {
                final Method method = new Method(methodName, methodDesc);
                if (OptimizedClassMatcher.METHODS_WE_NEVER_INSTRUMENT.contains(method)) {
                    return mv;
                }

                if (!OptimizedClassMatcher.this.methodAnnotationsToMatch.isEmpty()) {
                    mv = new MethodVisitor(Agent.ASM_LEVEL, mv) {
                        final Set<String> annotations = Sets.newHashSet();

                        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                            if (OptimizedClassMatcher.this.methodAnnotationsToMatch.contains(desc)) {
                                this.annotations.add(desc);
                            }

                            return super.visitAnnotation(desc, visible);
                        }

                        public void visitEnd() {
                            super.visitEnd();
                            ClassMethods.this.addMethodAnnotations(method, this.annotations);
                            if (ClassMethods.this.addMethodIfMatches(access, method, this.annotations)
                                        && (access & 64) != 0) {
                                ClassMethods.this.context.addBridgeMethod(method);
                            }

                        }
                    };
                } else if (this.addMethodIfMatches(access, method, ImmutableSet.<String>of()) && (access & 64) != 0) {
                    this.context.addBridgeMethod(method);
                }
            }

            return mv;
        }

        public void visitEnd() {
            super.visitEnd();
            if (this.matches != null) {
                Multimap classMatches = OptimizedClassMatcher.this.newClassMatches();
                Iterator match = this.matches.values().iterator();

                while (match.hasNext()) {
                    ClassAndMethodMatcher matcher = (ClassAndMethodMatcher) match.next();
                    Iterator i$ = matcher.getClassMatcher().getClassNames().iterator();

                    while (i$.hasNext()) {
                        String className = (String) i$.next();
                        classMatches.put(matcher, className);
                    }

                    classMatches.put(matcher, this.cr.getClassName());
                }

                Set daMethods = this.matches.keySet();
                OptimizedClassMatcher.Match match1 =
                        new OptimizedClassMatcher.Match(classMatches, daMethods, this.methodAnnotations);
                this.context.putMatch(OptimizedClassMatcher.this, match1);
            }

        }

        private boolean addMethodIfMatches(int access, Method method, Set<String> annotations) {
            boolean match = false;
            Collection set = (Collection) OptimizedClassMatcher.this.methods.get(method);
            if (set != null) {
                Iterator arr$ = set.iterator();

                while (arr$.hasNext()) {
                    ClassAndMethodMatcher len$ = (ClassAndMethodMatcher) arr$.next();
                    if (this.isMatch(len$.getClassMatcher(), this.loader, this.cr, this.classBeingRedefined)) {
                        this.getOrCreateMatches().put(method, len$);
                        match = true;
                    }
                }
            }

            for (Entry<MethodMatcher, ClassAndMethodMatcher> entry : OptimizedClassMatcher.this.methodMatchers) {
                if (((MethodMatcher) entry.getKey())
                            .matches(access, method.getName(), method.getDescriptor(), annotations)
                            && this.isMatch((entry.getValue()).getClassMatcher(), this.loader, this.cr,
                                                   this.classBeingRedefined)) {
                    this.getOrCreateMatches().put(method, entry.getValue());
                    match = true;
                }
            }

            return match;
        }
    }
}
