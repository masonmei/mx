package com.newrelic.agent.instrumentation.pointcuts.asynchttp;

import java.net.MalformedURLException;
import java.net.URL;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.NameMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.FieldAccessor;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;
import com.newrelic.agent.instrumentation.pointcuts.scala.ScalaTracerHolder;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.PointCutInvocationHandler;

@com.newrelic.agent.instrumentation.pointcuts.PointCut
public class AsyncHttpClientRequestPointCut extends com.newrelic.agent.instrumentation.PointCut implements
        EntryInvocationHandler {
    public static final boolean DEFAULT_ENABLED = true;
    private static final String POINT_CUT_NAME = AsyncHttpClientRequestPointCut.class.getName();

    public AsyncHttpClientRequestPointCut(ClassTransformer classTransformer) {
        super(createPointCutConfig(), createClassMatcher(), createMethodMatcher());
    }

    private static PointCutConfiguration createPointCutConfig() {
        return new PointCutConfiguration(POINT_CUT_NAME, "play2_instrumentation", true);
    }

    private static ClassMatcher createClassMatcher() {
        return new InterfaceMatcher("com/ning/http/client/AsyncHttpProvider");
    }

    private static MethodMatcher createMethodMatcher() {
        return new NameMethodMatcher("execute");
    }

    protected PointCutInvocationHandler getPointCutInvocationHandlerImpl() {
        return this;
    }

    public void handleInvocation(ClassMethodSignature sig, Object object, Object[] args) {
        Transaction tx = Transaction.getTransaction();
        if (!tx.isStarted()) {
            return;
        }
        Request request = null;
        if ((args[0] instanceof Request)) {
            request = (Request) args[0];
        } else {
            return;
        }
        ScalaTracerHolder tracerHolder = null;
        if ((args[1] instanceof AsyncHandler)) {
            Object oref = ((AsyncHandler) args[1])._nr_objectRef();
            if ((oref instanceof ObjectRef)) {
                Object elem = ((ObjectRef) oref)._nr_element();
                if ((elem instanceof ScalaTracerHolder)) {
                    tracerHolder = (ScalaTracerHolder) elem;
                }
            }
        } else if ((args[1] instanceof JavaAsyncHandler)) {
            tracerHolder = (ScalaTracerHolder) ((JavaAsyncHandler) args[1])._nr_scalaPromise();
        } else {
            return;
        }
        URL url = null;
        try {
            url = new URL(request.getUrl());
        } catch (MalformedURLException e) {
        }
        if (tracerHolder != null) {
            Object tracerInfo = new AsyncHttpClientTracerInfo(sig, url == null ? "URL_PARSE_ERROR" : url.getHost(),
                                                                     request.getUrl(), request.getMethod());

            tracerHolder._nr_setTracer(tracerInfo);
        }
    }

    @InterfaceMixin(originalClassName = {"scala/runtime/ObjectRef"})
    public static abstract interface ObjectRef {
        public static final String CLASS = "scala/runtime/ObjectRef";

        @FieldAccessor(fieldName = "elem", existingField = true)
        public abstract Object _nr_element();
    }

    @InterfaceMixin(originalClassName = {"play.api.libs.ws.WS$WSRequest$$anon$1",
                                                "play.api.libs.ws.ning.NingWSRequest$$anon$1"})
    public static abstract interface AsyncHandler {
        public static final String SCALA_CLASS = "play.api.libs.ws.WS$WSRequest$$anon$1";
        public static final String SCALA_CLASS_2_3 = "play.api.libs.ws.ning.NingWSRequest$$anon$1";

        @FieldAccessor(fieldName = "result$1", fieldDesc = "Lscala/runtime/ObjectRef;", existingField = true)
        public abstract Object _nr_objectRef();
    }

    @InterfaceMixin(originalClassName = {"play.libs.WS$WSRequest$1", "play.libs.ws.ning.NingWSRequest$1"})
    public static abstract interface JavaAsyncHandler {
        public static final String CLASS = "play.libs.WS$WSRequest$1";
        public static final String CLASS_2_3 = "play.libs.ws.ning.NingWSRequest$1";

        @FieldAccessor(fieldName = "val$scalaPromise", fieldDesc = "Lscala/concurrent/Promise;", existingField = true)
        public abstract Object _nr_scalaPromise();
    }

    @InterfaceMixin(originalClassName = {"com/ning/http/client/Request"})
    public static abstract interface Request {
        public static final String INTERFACE = "com/ning/http/client/Request";

        public abstract String getMethod();

        public abstract String getUrl();
    }

    public static final class AsyncHttpClientTracerInfo {
        private final long startTime;
        private final ClassMethodSignature sig;
        private final String host;
        private final String uri;
        private final String methodName;

        public AsyncHttpClientTracerInfo(ClassMethodSignature sig, String host, String uri, String methodName) {
            startTime = System.nanoTime();
            this.sig = sig;
            this.host = host;
            this.uri = uri;
            this.methodName = methodName;
        }

        public long getStartTime() {
            return startTime;
        }

        public ClassMethodSignature getClassMethodSignature() {
            return sig;
        }

        public String getHost() {
            return host;
        }

        public String getUri() {
            return uri;
        }

        public String getMethodName() {
            return methodName;
        }
    }
}