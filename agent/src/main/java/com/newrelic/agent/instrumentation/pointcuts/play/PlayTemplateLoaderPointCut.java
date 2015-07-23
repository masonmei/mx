package com.newrelic.agent.instrumentation.pointcuts.play;

import java.text.MessageFormat;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.transaction.TransactionNamingPolicy;

@PointCut
public class PlayTemplateLoaderPointCut extends TracerFactoryPointCut {
    private static final String POINT_CUT_NAME = PlayTemplateLoaderPointCut.class.getName();
    private static final String TEMPLATE_LOADER_CLASS = "play/templates/TemplateLoader";
    private static final String LOAD_METHOD = "load";
    private static final String LOAD_DESC = "(Ljava/lang/String;)Lplay/templates/Template;";
    private static final String PLAY_TEMPLATE = "PlayTemplate";

    public PlayTemplateLoaderPointCut(ClassTransformer classTransformer) {
        super(createPointCutConfig(), createClassMatcher(), createMethodMatcher());
    }

    private static PointCutConfiguration createPointCutConfig() {
        return new PointCutConfiguration(POINT_CUT_NAME, "play_instrumentation", true);
    }

    private static ClassMatcher createClassMatcher() {
        return new ExactClassMatcher("play/templates/TemplateLoader");
    }

    private static MethodMatcher createMethodMatcher() {
        return new ExactMethodMatcher("load", "(Ljava/lang/String;)Lplay/templates/Template;");
    }

    public Tracer doGetTracer(Transaction tx, ClassMethodSignature sig, Object object, Object[] args) {
        if ((tx.getDispatcher() != null) && (tx.isTransactionNamingEnabled())) {
            String templatePath = (String) args[0];
            setTransactionName(tx, templatePath);
        }
        return null;
    }

    private void setTransactionName(Transaction tx, String templatePath) {
        TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
        if ((Agent.LOG.isLoggable(Level.FINER)) && (policy.canSetTransactionName(tx,
                                                                                        TransactionNamePriority
                                                                                                .FRAMEWORK_HIGH))) {
            String msg = MessageFormat.format("Setting transaction name to \"{0}\" using Play template",
                                                     new Object[] {templatePath});

            Agent.LOG.finer(msg);
        }

        policy.setTransactionName(tx, templatePath, "PlayTemplate", TransactionNamePriority.FRAMEWORK_HIGH);
    }
}