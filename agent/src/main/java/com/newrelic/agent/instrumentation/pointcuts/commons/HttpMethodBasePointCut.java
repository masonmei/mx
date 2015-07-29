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
            "com/newrelic/deps/org/apache/commons/httpclient/HttpMethodBase";
    private static final int HOST_DELIMITER = 58;
    private static final String HTTPCONNECTION_CLASS_NAME =
            "com/newrelic/deps/org/apache/commons/httpclient/HttpConnection";
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
        super(HttpMethodBasePointCut.class, new ExactClassMatcher(HTTP_METHOD_BASE_CLASS_NAME_MATCH),
                     OrMethodMatcher.getMethodMatcher(new ExactMethodMatcher(EXECUTE_METHOD_NAME, EXECUTE_METHOD_DESC),
                                                             new ExactMethodMatcher(GET_RESPONSE_BODY_NAME, new String[] {GET_RESPONSE_BODY_DESC_1, GET_RESPONSE_BODY_DESC_2}),
                                                             new ExactMethodMatcher(RELEASE_CONNECTION_NAME, RELEASE_CONNECTION_DESC)));

        getHostMethodCache = ServiceFactory.getCacheService()
                                     .getMethodCache(HTTPCONNECTION_CLASS_NAME, GET_HOST_METHOD_NAME, GET_HOST_METHOD_DESC);

        getPortMethodCache = ServiceFactory.getCacheService()
                                     .getMethodCache(HTTPCONNECTION_CLASS_NAME, GET_PORT_METHOD_NAME, GET_PORT_METHOD_DESC);

        getProtocolMethodCache = ServiceFactory.getCacheService()
                                         .getMethodCache(HTTPCONNECTION_CLASS_NAME, GET_PROTOCOL_METHOD_NAME,
                                                                GET_PROTOCOL_METHOD_DESC);
    }

    protected Tracer getExternalTracer(Transaction transaction, ClassMethodSignature sig, Object httpMethod,
                                       Object[] args) {
        String host = UNKNOWN_HOST_NAME;
        String methodName = "";
        String uri = "";
        int port = DEFAULT_PORT_VALUE;
        try {
            if (sig.getMethodName() == EXECUTE_METHOD_NAME) {
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

                return super.doGetTracer(transaction, sig, httpMethod, host, uri, EXECUTE_METHOD_NAME);
            }
            if ((sig.getMethodName() == GET_RESPONSE_BODY_NAME) || (sig.getMethodName() == RELEASE_CONNECTION_NAME)) {
                Object header = ((HttpMethodExtension) httpMethod)._nr_getRequestHeader("host");
                if (header != null) {
                    host = ((NameValuePair) header).getValue();

                    int index = host.indexOf(':');
                    if (index > DEFAULT_PORT_VALUE) {
                        host = host.substring(0, index);
                    }
                }
            }

            methodName = sig.getMethodName();
        } catch (Throwable t) {
            String msg = MessageFormat.format("Instrumentation error invoking {0} in {1}: {2}", sig, getClass()
                                                                    .getName(),
                                                     t);

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
        return (String) getHost.invoke(httpConnection);
    }

    private String getScheme(Object httpConnection) throws Exception {
        Method getProtocol = getProtocolMethodCache.getDeclaredMethod(httpConnection.getClass());
        Object protocol = getProtocol.invoke(httpConnection);
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
        Integer port = (Integer) getPort.invoke(httpConnection);
        if (port == null) {
            return DEFAULT_PORT_VALUE;
        }
        return port;
    }

    private URI getUri(Object httpMethod) throws Exception {
        Object uri = ((HttpMethodExtension) httpMethod)._nr_getUri();
        if ((uri instanceof URI)) {
            return (URI) uri;
        }
        return null;
    }

    @InterfaceMixin(originalClassName = {HTTPCONNECTION_CLASS_NAME})
    public interface HttpConnection {
        String getHost();

        int getPort();
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