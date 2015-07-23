package com.newrelic.agent.instrumentation.pointcuts.asynchttp;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMapper;
import com.newrelic.agent.instrumentation.pointcuts.MethodMapper;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.PointCutInvocationHandler;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.OutboundHeaders;

@com.newrelic.agent.instrumentation.pointcuts.PointCut
public class AsyncHttpRequestBuilderPointCut extends com.newrelic.agent.instrumentation.PointCut implements
        EntryInvocationHandler {
    public static final boolean DEFAULT_ENABLED = true;
    private static final String POINT_CUT_NAME = AsyncHttpRequestBuilderPointCut.class.getName();

    public AsyncHttpRequestBuilderPointCut(ClassTransformer classTransformer) {
        super(createPointCutConfig(), createClassMatcher(), createMethodMatcher());
    }

    private static PointCutConfiguration createPointCutConfig() {
        return new PointCutConfiguration(POINT_CUT_NAME, "play2_instrumentation", true);
    }

    private static ClassMatcher createClassMatcher() {
        return new ExactClassMatcher("com/ning/http/client/RequestBuilderBase");
    }

    private static MethodMatcher createMethodMatcher() {
        return new ExactMethodMatcher("build", "()Lcom/ning/http/client/Request;");
    }

    protected PointCutInvocationHandler getPointCutInvocationHandlerImpl() {
        return this;
    }

    public void handleInvocation(ClassMethodSignature sig, Object object, Object[] args) {
        Transaction tx = Transaction.getTransaction();
        if (!tx.isStarted()) {
            return;
        }
        RequestBuilder request = null;
        if ((object instanceof RequestBuilder)) {
            request = (RequestBuilder) object;
        } else {
            return;
        }

        tx.getCrossProcessState().processOutboundRequestHeaders(new OutboundHeadersWrapper(request));
    }

    @InterfaceMapper(originalInterfaceName = "com/ning/http/client/RequestBuilderBase")
    public static abstract interface RequestBuilder {
        public static final String CLASS = "com/ning/http/client/RequestBuilderBase";

        @MethodMapper(originalMethodName = "setHeader", originalDescriptor = "(Ljava/lang/String;Ljava/lang/String;)"
                                                                                     + "Lcom/ning/http/client/RequestBuilderBase;", invokeInterface = false)
        public abstract Object nr_setHeader(String paramString1, String paramString2);
    }

    private class OutboundHeadersWrapper implements OutboundHeaders {
        private final RequestBuilder request;

        public OutboundHeadersWrapper(RequestBuilder connection) {
            request = connection;
        }

        public void setHeader(String name, String value) {
            request.nr_setHeader(name, value);
        }

        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }
    }
}