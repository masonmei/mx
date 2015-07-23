package com.newrelic.agent.instrumentation.pointcuts.play;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionState;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.FieldAccessor;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.RetryException;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.tracers.servlet.BasicRequestRootTracer;
import com.newrelic.agent.transaction.TransactionNamingPolicy;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;

@PointCut
public class PlayDispatcherPointCut extends TracerFactoryPointCut {
    public static final String PLAY_INSTRUMENTATION_GROUP_NAME = "play_instrumentation";
    public static final boolean DEFAULT_ENABLED = true;
    public static final String UNKNOWN_CONTROLLER_ACTION = "UNKNOWN";
    public static final String PLAY_CONTROLLER_ACTION = "PlayControllerAction";
    private static final String POINT_CUT_NAME = PlayDispatcherPointCut.class.getName();
    private static final String ACTION_INVOKER_CLASS = "play/mvc/ActionInvoker";
    private static final String SCOPE_PARAMS_CLASS = "play/mvc/Scope$Params";
    private static final String HTTP_COOKIE_CLASS = "play/mvc/Http$Cookie";
    private static final String HTTP_HEADER_CLASS = "play/mvc/Http$Header";
    private static final String HTTP_REQUEST_CLASS = "play/mvc/Http$Request";
    private static final String HTTP_RESPONSE_CLASS = "play/mvc/Http$Response";
    private static final String INVOKE_METHOD_NAME = "invoke";
    private static final String INVOKE_METHOD_DESC = "(Lplay/mvc/Http$Request;Lplay/mvc/Http$Response;)V";

    public PlayDispatcherPointCut(ClassTransformer classTransformer) {
        super(createPointCutConfig(), createClassMatcher(), createMethodMatcher());
    }

    private static PointCutConfiguration createPointCutConfig() {
        return new PointCutConfiguration(POINT_CUT_NAME, "play_instrumentation", true);
    }

    private static ClassMatcher createClassMatcher() {
        return new ExactClassMatcher("play/mvc/ActionInvoker");
    }

    private static MethodMatcher createMethodMatcher() {
        return new ExactMethodMatcher("invoke", "(Lplay/mvc/Http$Request;Lplay/mvc/Http$Response;)V");
    }

    protected boolean isDispatcher() {
        return true;
    }

    public final Tracer doGetTracer(Transaction tx, ClassMethodSignature sig, Object object, Object[] args) {
        Tracer rootTracer = tx.getRootTracer();
        if (rootTracer != null) {
            return null;
        }
        PlayHttpRequest request = (PlayHttpRequest) args[0];
        Transaction savedTx = getAndClearSavedTransaction(request);
        if (savedTx != null) {
            resumeTransaction(savedTx);

            throw new RetryException();
        }
        TransactionState transactionState = tx.getTransactionState();
        if (!(transactionState instanceof PlayTransactionStateImpl)) {
            transactionState = new PlayTransactionStateImpl(request);
            tx.setTransactionState(transactionState);

            throw new RetryException();
        }
        Tracer tracer = createTracer(tx, sig, object, args);
        if (tracer != null) {
            setTransactionName(tx, request);
        }
        return tracer;
    }

    private Transaction getAndClearSavedTransaction(PlayHttpRequest request) {
        Transaction savedTx = (Transaction) request._nr_getTransaction();
        if (savedTx == null) {
            return null;
        }
        request._nr_setTransaction(null);
        return savedTx;
    }

    private void resumeTransaction(Transaction savedTx) {
        TransactionState transactionState = savedTx.getTransactionState();
        transactionState.resume();
        Transaction.clearTransaction();
        Transaction.setTransaction(savedTx);
    }

    private Tracer createTracer(Transaction tx, ClassMethodSignature sig, Object object, Object[] args) {
        try {
            return new BasicRequestRootTracer(tx, sig, object, getRequest(tx, sig, object, args),
                                                     getResponse(tx, sig, object, args),
                                                     getMetricNameFormat(tx, sig, object, args));
        } catch (Exception e) {
            String msg = MessageFormat.format("Unable to create request dispatcher tracer: {0}", new Object[] {e});
          if (Agent.LOG.isFinestEnabled()) {
            Agent.LOG.log(Level.WARNING, msg, e);
          } else {
            Agent.LOG.warning(msg);
          }
        }
        return null;
    }

    private MetricNameFormat getMetricNameFormat(Transaction transaction, ClassMethodSignature sig, Object object,
                                                 Object[] args) {
        return new SimpleMetricNameFormat("RequestDispatcher", ClassMethodMetricNameFormat.getMetricName(sig, object,
                                                                                                                "RequestDispatcher"));
    }

    private Response getResponse(Transaction tx, ClassMethodSignature sig, Object object, Object[] args)
            throws Exception {
        return DelegatingPlayHttpResponse.create((PlayHttpResponse) args[1]);
    }

    private Request getRequest(Transaction tx, ClassMethodSignature sig, Object object, Object[] args)
            throws Exception {
        return DelegatingPlayHttpRequest.create((PlayHttpRequest) args[0]);
    }

    private void setTransactionName(Transaction tx, PlayHttpRequest request) {
        if (!tx.isTransactionNamingEnabled()) {
            return;
        }
        String action = request._nr_getAction();
        action = action == null ? "UNKNOWN" : action;
        setTransactionName(tx, action);
    }

    private void setTransactionName(Transaction tx, String action) {
        TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
        if ((Agent.LOG.isLoggable(Level.FINER)) && (policy.canSetTransactionName(tx,
                                                                                        TransactionNamePriority
                                                                                                .FRAMEWORK_LOW))) {
            String msg = MessageFormat.format("Setting transaction name to \"{0}\" using Play controller action",
                                                     new Object[] {action});

            Agent.LOG.finer(msg);
        }

        policy.setTransactionName(tx, action, "PlayControllerAction", TransactionNamePriority.FRAMEWORK_LOW);
    }

    @InterfaceMixin(originalClassName = {"play/mvc/Http$Response"})
    public static abstract interface PlayHttpResponse {
        public abstract void setHeader(String paramString1, String paramString2);

        @FieldAccessor(fieldName = "status", existingField = true)
        public abstract Integer _nr_getResponseStatus();

        @FieldAccessor(fieldName = "contentType", existingField = true)
        public abstract String _nr_getContentType();
    }

    @InterfaceMixin(originalClassName = {"play/mvc/Http$Request"})
    public static abstract interface PlayHttpRequest {
        @FieldAccessor(fieldName = "transaction")
        public abstract void _nr_setTransaction(Object paramObject);

        @FieldAccessor(fieldName = "transaction")
        public abstract Object _nr_getTransaction();

        @FieldAccessor(fieldName = "headers", existingField = true)
        public abstract Map<?, ?> _nr_getHeaders();

        @FieldAccessor(fieldName = "cookies", existingField = true)
        public abstract Map<?, ?> _nr_getCookies();

        @FieldAccessor(fieldName = "url", existingField = true)
        public abstract String _nr_getUrl();

        @FieldAccessor(fieldName = "action", existingField = true)
        public abstract String _nr_getAction();

        @FieldAccessor(fieldName = "params", fieldDesc = "Lplay/mvc/Scope$Params;", existingField = true)
        public abstract Object _nr_getParams();
    }

    @InterfaceMixin(originalClassName = {"play/mvc/Http$Cookie"})
    public static abstract interface PlayHttpCookie {
        @FieldAccessor(fieldName = "value", existingField = true)
        public abstract String _nr_getValue();
    }

    @InterfaceMixin(originalClassName = {"play/mvc/Scope$Params"})
    public static abstract interface PlayScopeParams {
        public abstract String[] getAll(String paramString);

        public abstract Map<String, String[]> all();
    }

    @InterfaceMixin(originalClassName = {"play/mvc/Http$Header"})
    public static abstract interface PlayHttpHeader {
        public abstract String value();
    }

    private static class DelegatingPlayHttpResponse implements Response {
        private final PlayHttpResponse delegate;

        private DelegatingPlayHttpResponse(PlayHttpResponse delegate) {
            this.delegate = delegate;
        }

        static Response create(PlayHttpResponse delegate) {
            return new DelegatingPlayHttpResponse(delegate);
        }

        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }

        public String getStatusMessage() {
            return null;
        }

        public void setHeader(String name, String value) {
            delegate.setHeader(name, value);
        }

        public int getStatus() {
            Integer status = delegate._nr_getResponseStatus();
            return status == null ? 0 : status.intValue();
        }

        public String getContentType() {
            return delegate._nr_getContentType();
        }
    }

    private static class DelegatingPlayHttpRequest implements Request {
        private final PlayHttpRequest delegate;

        private DelegatingPlayHttpRequest(PlayHttpRequest delegate) {
            this.delegate = delegate;
        }

        static Request create(PlayHttpRequest delegate) {
            return new DelegatingPlayHttpRequest(delegate);
        }

        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }

        public Enumeration<?> getParameterNames() {
            PlayScopeParams playScopeParams = getScopeParams();
            if (playScopeParams == null) {
                return null;
            }
            Map params = playScopeParams.all();
            return Collections.enumeration(params.keySet());
        }

        public String[] getParameterValues(String name) {
            PlayScopeParams playScopeParams = getScopeParams();
            if (playScopeParams == null) {
                return new String[0];
            }
            return playScopeParams.getAll(name);
        }

        public Object getAttribute(String name) {
            return null;
        }

        public String getRequestURI() {
            return delegate._nr_getUrl();
        }

        public String getHeader(String name) {
            if (name == null) {
                return null;
            }
            Map headers = delegate._nr_getHeaders();
            PlayHttpHeader header = (PlayHttpHeader) headers.get(name);
            if (header != null) {
                return header.value();
            }

            header = (PlayHttpHeader) headers.get(name.toLowerCase());
            return header == null ? null : header.value();
        }

        public String getRemoteUser() {
            return null;
        }

        public String getCookieValue(String name) {
            if (name == null) {
                return null;
            }
            Map cookies = delegate._nr_getCookies();
            PlayHttpCookie cookie = (PlayHttpCookie) cookies.get(name);
            return cookie == null ? null : cookie._nr_getValue();
        }

        private PlayScopeParams getScopeParams() {
            Object scopeParams = delegate._nr_getParams();
            if ((scopeParams instanceof PlayScopeParams)) {
                return (PlayScopeParams) scopeParams;
            }
            return null;
        }
    }
}