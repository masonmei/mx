package com.newrelic.agent.instrumentation.pointcuts.akka;

import java.text.MessageFormat;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.async.AsyncTransactionState;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OrClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.NameMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.TransactionHolder;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.PointCutInvocationHandler;

@com.newrelic.agent.instrumentation.pointcuts.PointCut
public class AkkaTransactionHolderInitPointCut extends com.newrelic.agent.instrumentation.PointCut implements
        EntryInvocationHandler {
    public static final boolean DEFAULT_ENABLED = true;
    private static final String POINT_CUT_NAME = AkkaTransactionHolderInitPointCut.class.getName();

    public AkkaTransactionHolderInitPointCut(ClassTransformer classTransformer) {
        super(createPointCutConfig(), createClassMatcher(), createMethodMatcher());
    }

    private static PointCutConfiguration createPointCutConfig() {
        return new PointCutConfiguration(POINT_CUT_NAME, "akka_instrumentation", true);
    }

    private static ClassMatcher createClassMatcher() {
        return OrClassMatcher.getClassMatcher(new ClassMatcher[] {new ExactClassMatcher("akka/dispatch/Envelope"),
                                                                         new ExactClassMatcher
                                                                                 ("akka/dispatch/AbstractPromise"),
                                                                         new ExactClassMatcher
                                                                                 ("akka/dispatch/Future$$anon$4")});
    }

    private static MethodMatcher createMethodMatcher() {
        return new NameMethodMatcher("<init>");
    }

    protected PointCutInvocationHandler getPointCutInvocationHandlerImpl() {
        return this;
    }

    public void handleInvocation(ClassMethodSignature sig, Object object, Object[] args) {
        if ((object instanceof TransactionHolder)) {
            TransactionHolder th = (TransactionHolder) object;
            Transaction tx = Transaction.getTransaction();
            if ((tx.isStarted()) && (th._nr_getTransaction() == null)) {
                if (!(tx.getTransactionState() instanceof AsyncTransactionState)) {
                    return;
                }
                th._nr_setTransaction(tx);
                if (args.length > 1) {
                    String akkaOrigin = args[1].toString();
                    akkaOrigin = akkaOrigin.replaceAll("\\$[^/\\]]+", "");
                    akkaOrigin = akkaOrigin.replace("/", "\\");
                    akkaOrigin = akkaOrigin.replace(".", "_");
                    th._nr_setName(MessageFormat.format("Java/{0}/tell", new Object[] {akkaOrigin}));
                }
                tx.getTransactionState().asyncJobStarted(th);
            }
        }
    }
}