package com.newrelic.agent.instrumentation.pointcuts.container.netty;

import java.text.MessageFormat;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.TransactionHolder;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.PointCutInvocationHandler;
import com.newrelic.api.agent.Response;

@com.newrelic.agent.instrumentation.pointcuts.PointCut
public class NettyCompletePointCut extends com.newrelic.agent.instrumentation.PointCut implements
        EntryInvocationHandler {
    public static final boolean DEFAULT_ENABLED = true;
    private static final String POINT_CUT_NAME = NettyCompletePointCut.class.getName();

    public NettyCompletePointCut(ClassTransformer classTransformer) {
        super(createPointCutConfig(), createClassMatcher(), createMethodMatcher());
    }

    private static PointCutConfiguration createPointCutConfig() {
        return new PointCutConfiguration(POINT_CUT_NAME, "netty_instrumentation", true);
    }

    private static ClassMatcher createClassMatcher() {
        return new ExactClassMatcher("org/jboss/netty/handler/codec/http/HttpMessageEncoder");
    }

    private static MethodMatcher createMethodMatcher() {
        return new ExactMethodMatcher("encode",
                                             "(Lorg/jboss/netty/channel/ChannelHandlerContext;"
                                                     + "Lorg/jboss/netty/channel/Channel;Ljava/lang/Object;)"
                                                     + "Ljava/lang/Object;");
    }

    protected PointCutInvocationHandler getPointCutInvocationHandlerImpl() {
        return this;
    }

    public void handleInvocation(ClassMethodSignature sig, Object object, Object[] args) {
        if (!object.getClass().getName().equals("org.jboss.netty.handler.codec.http.HttpResponseEncoder")) {
            return;
        }
        handleTransactionHolder(args);
    }

    void handleTransactionHolder(Object[] args) {
        if (((args[1] instanceof TransactionHolder)) && ((args[2] instanceof NettyHttpResponse))) {
            TransactionHolder txHolder = (TransactionHolder) args[1];
            Transaction tx = (Transaction) txHolder._nr_getTransaction();
            if ((tx == null) || (!tx.isStarted())) {
                Agent.LOG.fine(MessageFormat.format("Unable to complete {0} held by {1}", new Object[] {tx, txHolder}));
                return;
            }
            Response response = tx.getDispatcher().getResponse();
            if ((response instanceof DelegatingNettyHttpResponse)) {
                ((DelegatingNettyHttpResponse) response).setDelegate((NettyHttpResponse) args[2]);
            }
            tx.getTransactionState().asyncJobFinished(txHolder);

            tx.beforeSendResponseHeaders();
        }
    }
}