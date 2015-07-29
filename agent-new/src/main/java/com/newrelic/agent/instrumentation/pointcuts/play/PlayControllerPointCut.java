//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.pointcuts.play;

import java.text.MessageFormat;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
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
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;

@PointCut
public class PlayControllerPointCut extends TracerFactoryPointCut {
    private static final String POINT_CUT_NAME = PlayControllerPointCut.class.getName();
    private static final String CONTROLLER_CLASS = "play/mvc/Controller";
    private static final String AWAIT_METHOD_NAME = "await";
    private static final String AWAIT_METHOD_DESC_1 = "(I)V";
    private static final String AWAIT_METHOD_DESC_2 = "(Ljava/util/concurrent/Future;)Ljava/lang/Object;";
    private static final String AWAIT_METHOD_DESC_3 = "(ILplay/libs/F$Action0;)V";
    private static final String AWAIT_METHOD_DESC_4 = "(Ljava/util/concurrent/Future;Lplay/libs/F$Action;)V";
    private static final String RENDER_TEMPLATE_METHOD_NAME = "renderTemplate";
    private static final String RENDER_TEMPLATE_METHOD_DESC = "(Ljava/lang/String;Ljava/util/Map;)V";
    private static final String TEMPLATE_METRIC_NAME = "Controller.renderTemplate/{0}";
    private static final String AWAIT_METRIC_NAME = "Controller.await";
    private static final String SUSPEND_EXECPTION_CLASS = "play.Invoker$Suspend";

    public PlayControllerPointCut(ClassTransformer classTransformer) {
        super(createPointCutConfig(), createClassMatcher(), createMethodMatcher());
    }

    private static PointCutConfiguration createPointCutConfig() {
        return new PointCutConfiguration(POINT_CUT_NAME, "play_instrumentation", true);
    }

    private static ClassMatcher createClassMatcher() {
        return new ExactClassMatcher("play/mvc/Controller");
    }

    private static MethodMatcher createMethodMatcher() {
        return OrMethodMatcher.getMethodMatcher(new MethodMatcher[] {new ExactMethodMatcher("renderTemplate",
                                                                                                   "(Ljava/lang/String;Ljava/util/Map;)V"),
                                                                            new ExactMethodMatcher("await",
                                                                                                          new String[] {"(I)V",
                                                                                                                               "(Ljava/util/concurrent/Future;)Ljava/lang/Object;",
                                                                                                                               "(ILplay/libs/F$Action0;)V",
                                                                                                                               "(Ljava/util/concurrent/Future;Lplay/libs/F$Action;)V"})});
    }

    public Tracer doGetTracer(Transaction tx, ClassMethodSignature sig, Object object, Object[] args) {
        return "await" == sig.getMethodName() ? this.getAwaitTracer(tx, sig, object, args)
                       : this.getRenderTracer(tx, sig, object, args);
    }

    private Tracer getAwaitTracer(Transaction tx, ClassMethodSignature sig, Object object, Object[] args) {
        SimpleMetricNameFormat format = new SimpleMetricNameFormat("Controller.await");
        return (Tracer) (sig.getMethodDesc() != "(ILplay/libs/F$Action0;)V"
                                 && sig.getMethodDesc() != "(Ljava/util/concurrent/Future;Lplay/libs/F$Action;)V"
                                 ? new DefaultTracer(tx, sig, object, format)
                                 : new PlayControllerPointCut.PlayControllerTracer(tx, sig, object, format));
    }

    private Tracer getRenderTracer(Transaction tx, ClassMethodSignature sig, Object object, Object[] args) {
        String templateName = (String) args[0];
        String metricName = MessageFormat.format("Controller.renderTemplate/{0}", new Object[] {templateName});
        SimpleMetricNameFormat format = new SimpleMetricNameFormat(metricName);
        return new DefaultTracer(tx, sig, object, format);
    }

    private static class PlayControllerTracer extends DefaultTracer {
        public PlayControllerTracer(Transaction tx, ClassMethodSignature sig, Object object, MetricNameFormat format) {
            super(tx, sig, object, format);
        }

        protected void doFinish(Throwable throwable) {
            if (throwable.getClass().getName() == "play.Invoker$Suspend") {
                this.getTransaction().getTransactionState().suspendRootTracer();
            }

        }
    }
}
