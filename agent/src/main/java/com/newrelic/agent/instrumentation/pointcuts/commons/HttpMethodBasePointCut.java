package com.newrelic.agent.instrumentation.pointcuts.commons;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.InstrumentUtils;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.ExternalComponentTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.util.MethodCache;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.OutboundHeaders;

@PointCut
public class HttpMethodBasePointCut extends HttpCommonsPointCut {
    protected static final String HTTP_METHOD_BASE_CLASS_NAME_MATCH =
            "com/newrelic/agent/deps/org/apache/commons/httpclient/HttpMethodBase";
    private static final int HOST_DELIMITER = 58;
    private static final String HTTPCONNECTION_CLASS_NAME =
            "com/newrelic/agent/deps/org/apache/commons/httpclient/HttpConnection";
    private static final String GET_HOST_METHOD_NAME = "getHost";
    private static final String GET_HOST_METHOD_DESC = "()V";
    private static final String GET_PORT_METHOD_NAME = "getPort";
    private static final String GET_PORT_METHOD_DESC = "()I";
    private static final String GET_PROTOCOL_METHOD_NAME = "getProtocol";
    private static final String GET_PROTOCOL_METHOD_DESC = "()Lorg/apache/commons/httpclient/protocol/Protocol;";
    private static final int DEFAULT_PORT_VALUE = -1;
    private static final String UNKNOWN_HOST_NAME = "Unknown";
    private static final String EXECUTE_METHOD_NAME = "execute";
    private static final String EXECUTE_METHOD_DESC =
            "(Lorg/apache/commons/httpclient/HttpState;Lorg/apache/commons/httpclient/HttpConnection;)I";
    private static final String GET_RESPONSE_BODY_NAME = "getResponseBody";
    private static final String GET_RESPONSE_BODY_DESC_1 = "()[B";
    private static final String GET_RESPONSE_BODY_DESC_2 = "(I)[B";
    private static final String RELEASE_CONNECTION_NAME = "releaseConnection";
    private static final String RELEASE_CONNECTION_DESC = "()V";
    private final MethodCache getHostMethodCache;
    private final MethodCache getPortMethodCache;
    private final MethodCache getProtocolMethodCache;

    public HttpMethodBasePointCut(ClassTransformer classTransformer) {
        super(HttpMethodBasePointCut.class,
                     new ExactClassMatcher("com/newrelic/agent/deps/org/apache/commons/httpclient/HttpMethodBase"),
                     OrMethodMatcher.getMethodMatcher(new MethodMatcher[] {new ExactMethodMatcher("execute",
                                                                                                         "(Lorg/apache/commons/httpclient/HttpState;Lorg/apache/commons/httpclient/HttpConnection;)I"),
                                                                                  new ExactMethodMatcher
                                                                                          ("getResponseBody",
                                                                                                                new String[] {"()[B",
                                                                                                                                     "(I)[B"}),
                                                                                  new ExactMethodMatcher
                                                                                          ("releaseConnection",
                                                                                                                "()V")}));

        getHostMethodCache = ServiceFactory.getCacheService()
                                     .getMethodCache
                                              ("com/newrelic/agent/deps/org/apache/commons/httpclient/HttpConnection",
                                                            "getHost", "()V");

        getPortMethodCache = ServiceFactory.getCacheService()
                                     .getMethodCache
                                              ("com/newrelic/agent/deps/org/apache/commons/httpclient/HttpConnection",
                                                            "getPort", "()I");

        getProtocolMethodCache = ServiceFactory.getCacheService()
                                         .getMethodCache
                                                  ("com/newrelic/agent/deps/org/apache/commons/httpclient/HttpConnection",
                                                                "getProtocol",
                                                                "()Lorg/apache/commons/httpclient/protocol/Protocol;");
    }

    protected Tracer getExternalTracer(Transaction transaction, ClassMethodSignature sig, Object httpMethod,
                                       Object[] args) {
        String host = "Unknown";
        String methodName = "";
        String uri = "";
        int port = -1;
        try {
            if (sig.getMethodName() == "execute") {
                Object httpConnection = args[1];
                if (httpConnection != null) {
                    host = getHost(httpConnection);
                    port = getPort(httpConnection);
                    if (httpMethod != null) {
                        URI theUri = getUri(httpMethod);
                        String scheme = theUri.getScheme();
                        if (scheme == null) {
                            scheme = getScheme(httpConnection);
                            String path = theUri.getPath();
                            if ("null".equals(path)) {
                                path = null;
                            }
                            uri = InstrumentUtils.getURI(scheme, host, port, path);
                        } else {
                            uri = InstrumentUtils.getURI(theUri.getScheme(), theUri.getHost(), port, theUri.getPath());
                        }
                    }
                }
                if ((httpMethod instanceof HttpMethodBase)) {
                    HttpMethodBase httpMethodBase = (HttpMethodBase) httpMethod;
                    transaction.getCrossProcessState()
                            .processOutboundRequestHeaders(new OutboundHeadersWrapper(httpMethodBase));
                }

                return super.doGetTracer(transaction, sig, httpMethod, host, uri, "execute");
            }
            if ((sig.getMethodName() == "getResponseBody") || (sig.getMethodName() == "releaseConnection")) {
                Object header = ((HttpMethodExtension) httpMethod)._nr_getRequestHeader("host");
                if (header != null) {
                    host = ((NameValuePair) header).getValue();

                    int index = host.indexOf(':');
                    if (index > -1) {
                        host = host.substring(0, index);
                    }
                }
            }

            if (sig != null) {
                methodName = sig.getMethodName();
            }
        } catch (Throwable t) {
            String msg = MessageFormat.format("Instrumentation error invoking {0} in {1}: {2}",
                                                     new Object[] {sig, getClass().getName(), t});

            if (Agent.LOG.isLoggable(Level.FINEST)) {
                Agent.LOG.log(Level.FINEST, msg, t);
            } else {
                Agent.LOG.finer(msg);
            }
        }

        return new ExternalComponentTracer(transaction, sig, httpMethod, host, "CommonsHttp", uri,
                                                  new String[] {methodName});
    }

    private String getHost(Object httpConnection) throws Exception {
        if ((httpConnection instanceof HttpConnection)) {
            return ((HttpConnection) httpConnection).getHost();
        }

        Method getHost = getHostMethodCache.getDeclaredMethod(httpConnection.getClass());
        return (String) getHost.invoke(httpConnection, new Object[0]);
    }

    private String getScheme(Object httpConnection) throws Exception {
        Method getProtocol = getProtocolMethodCache.getDeclaredMethod(httpConnection.getClass());
        Object protocol = getProtocol.invoke(httpConnection, new Object[0]);
        if ((protocol instanceof Protocol)) {
            return ((Protocol) protocol).getScheme();
        }
        return null;
    }

    private int getPort(Object httpConnection) throws Exception {
        if ((httpConnection instanceof HttpConnection)) {
            return ((HttpConnection) httpConnection).getPort();
        }
        Method getPort = getPortMethodCache.getDeclaredMethod(httpConnection.getClass());
        Integer port = (Integer) getPort.invoke(httpConnection, new Object[0]);
        if (port == null) {
            return -1;
        }
        return port.intValue();
    }

    private URI getUri(Object httpMethod) throws Exception {
        Object uri = ((HttpMethodExtension) httpMethod)._nr_getUri();
        if ((uri instanceof URI)) {
            return (URI) uri;
        }
        return null;
    }

    @InterfaceMixin(originalClassName = {"com/newrelic/agent/deps/org/apache/commons/httpclient/HttpConnection"})
    public static abstract interface HttpConnection {
        public abstract String getHost();

        public abstract int getPort();
    }

    private class OutboundHeadersWrapper implements OutboundHeaders {
        private final HttpMethodBase request;

        public OutboundHeadersWrapper(HttpMethodBase httpMethodBase) {
            request = httpMethodBase;
        }

        public void setHeader(String name, String value) {
            request.setRequestHeader(name, value);
        }

        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }
    }
}