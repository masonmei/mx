//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.custom;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import com.newrelic.deps.org.objectweb.asm.commons.Method;

import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.classmatchers.ClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher.Match;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcherBuilder;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.ContextClassTransformer;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;
import com.newrelic.agent.instrumentation.tracing.TraceDetails;

public class CustomClassTransformer implements ContextClassTransformer {
    final List<ExtensionClassAndMethodMatcher> extensionPointCuts;
    private final InstrumentationContextManager contextManager;
    private final ClassMatchVisitorFactory matcher;

    public CustomClassTransformer(InstrumentationContextManager contextManager,
                                  List<ExtensionClassAndMethodMatcher> extensionPointCuts) {
        this.extensionPointCuts = extensionPointCuts;
        this.matcher = OptimizedClassMatcherBuilder.newBuilder()
                               .addClassMethodMatcher((ClassAndMethodMatcher[]) extensionPointCuts
                                                                                        .toArray(new ExtensionClassAndMethodMatcher[0]))
                               .build();
        contextManager.addContextClassTransformer(this.matcher, this);
        this.contextManager = contextManager;
    }

    public void destroy() {
        this.contextManager.removeMatchVisitor(this.matcher);
    }

    public ClassMatchVisitorFactory getMatcher() {
        return this.matcher;
    }

    public byte[] transform(ClassLoader pLoader, String pClassName, Class<?> pClassBeingRedefined,
                            ProtectionDomain pProtectionDomain, byte[] pClassfileBuffer,
                            InstrumentationContext pContext, Match match) throws IllegalClassFormatException {
        try {
            this.addMatchesToTraces(pContext, match);
        } catch (Throwable var9) {
            Agent.LOG.log(Level.FINE, MessageFormat.format("Unable to transform class {0}", new Object[] {pClassName}));
            if (Agent.LOG.isFinestEnabled()) {
                Agent.LOG.log(Level.FINEST,
                                     MessageFormat.format("Unable to transform class {0}", new Object[] {pClassName}),
                                     var9);
            }
        }

        return null;
    }

    private void addMatchesToTraces(InstrumentationContext pContext, Match match) {
        ArrayList matches = Lists.newArrayList(this.extensionPointCuts);
        matches.retainAll(match.getClassMatches().keySet());
        if (!matches.isEmpty()) {
            Iterator i$ = matches.iterator();

            while (i$.hasNext()) {
                ExtensionClassAndMethodMatcher pc = (ExtensionClassAndMethodMatcher) i$.next();
                Iterator i$1 = match.getMethods().iterator();

                while (i$1.hasNext()) {
                    Method m = (Method) i$1.next();
                    if (pc.getMethodMatcher()
                                .matches(-1, m.getName(), m.getDescriptor(), match.getMethodAnnotations(m))) {
                        Method method = (Method) pContext.getBridgeMethods().get(m);
                        if (method != null) {
                            m = method;
                        }

                        TraceDetails td = pc.getTraceDetails();
                        if (td.ignoreTransaction()) {
                            if (Agent.LOG.isFinerEnabled()) {
                                Agent.LOG.log(Level.FINER, MessageFormat
                                                                   .format("Matched method {0} for ignoring the "
                                                                                   + "transaction trace.",
                                                                                  new Object[] {m.toString()}));
                            }

                            pContext.addIgnoreTransactionMethod(m);
                        } else {
                            if (Agent.LOG.isFinerEnabled()) {
                                Agent.LOG.log(Level.FINER, MessageFormat
                                                                   .format("Matched method {0} for instrumentation.",
                                                                                  new Object[] {m.toString()}));
                            }

                            pContext.addTrace(m, td);
                        }
                    }
                }
            }
        }

    }
}
