//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.pointcuts.container;

import java.text.MessageFormat;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.jasper.GeneratorVisitTracerFactory;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;

@PointCut
public class JasperCompilerPointCut extends TracerFactoryPointCut {
    private static final ClassMatcher CLASS_MATCHER = new ExactClassMatcher("org/apache/jasper/compiler/Compiler");
    private static final MethodMatcher COMPILE_METHOD_1_MATCHER = new ExactMethodMatcher("compile", "(ZZ)V");
    private static final MethodMatcher COMPILE_METHOD_2_MATCHER = new ExactMethodMatcher("compile", "(Z)V");
    private static final MethodMatcher METHOD_MATCHER;
    public static String CURRENT_JSP_FILE_KEY = "CurrentJspFileKey";

    static {
        METHOD_MATCHER = OrMethodMatcher.getMethodMatcher(new MethodMatcher[] {COMPILE_METHOD_1_MATCHER,
                                                                                      COMPILE_METHOD_2_MATCHER});
    }

    public JasperCompilerPointCut(ClassTransformer classTransformer) {
        super(JasperCompilerPointCut.class, CLASS_MATCHER, METHOD_MATCHER);
    }

    public Tracer doGetTracer(Transaction tx, ClassMethodSignature sig, Object compiler, Object[] args) {
        Tracer parent = tx.getTransactionActivity().getLastTracer();
        if (parent != null && parent instanceof JasperCompilerPointCut.JasperCompilerTracer) {
            return null;
        } else {
            try {
                Object t = compiler.getClass().getMethod("getCompilationContext", new Class[0])
                                   .invoke(compiler, new Object[0]);
                if (t != null) {
                    String page = (String) t.getClass().getMethod("getJspFile", new Class[0]).invoke(t, new Object[0]);
                    if (page != null) {
                        String msg = MessageFormat.format("Compiling JSP: {0}", new Object[] {page});
                        Agent.LOG.fine(msg);
                        GeneratorVisitTracerFactory.noticeJspCompile(tx, page);
                        JasperCompilerPointCut.JasperCompilerTracer tracer =
                                new JasperCompilerPointCut.JasperCompilerTracer(tx, sig, compiler,
                                                                                       new SimpleMetricNameFormat("View"
                                                                                                                          + page.replace('.',
                                                                                                                                                '_')
                                                                                                                          + "/Compile"));
                        return tracer;
                    }
                }
            } catch (Throwable var10) {
                Agent.LOG.severe("Unable to generate a Jasper compilation metric: " + var10.getMessage());
            }

            return null;
        }
    }

    private final class JasperCompilerTracer extends DefaultTracer {
        public JasperCompilerTracer(Transaction tx, ClassMethodSignature sig, Object object,
                                    MetricNameFormat metricNameFormatter) {
            super(tx, sig, object, metricNameFormatter, 0);
        }
    }
}
