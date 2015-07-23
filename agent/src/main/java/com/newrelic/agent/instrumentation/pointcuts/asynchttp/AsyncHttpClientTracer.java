package com.newrelic.agent.instrumentation.pointcuts.asynchttp;

import java.util.List;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.dispatchers.AsyncDispatcher;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.instrumentation.pointcuts.FieldAccessor;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;
import com.newrelic.agent.tracers.AbstractCrossProcessTracer;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.ExternalComponentNameFormat;
import com.newrelic.agent.tracers.TransactionActivityInitiator;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;

public class AsyncHttpClientTracer extends AbstractCrossProcessTracer implements TransactionActivityInitiator {
    private final String txName;
    private final long startTime2;
    private Object response;

    public AsyncHttpClientTracer(Transaction transaction, String txName, ClassMethodSignature sig, Object object,
                                 String host, String library, String uri, long startTime, String methodName) {
        super(transaction, sig, object, host, library, uri, methodName);
        setMetricNameFormat(ExternalComponentNameFormat.create(host, library, true, uri, new String[0]));
        this.txName = txName;
        startTime2 = startTime;
    }

    protected String getHeaderValue(Object returnValue, String name) {
        if (response == null) {
            return null;
        }

        if ((response instanceof WSResponse)) {
            response = ((WSResponse) response)._nr_response();
        }
        if ((response instanceof Response)) {
            return ((Response) response).getHeader(name);
        }

        return null;
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    public long getStartTime() {
        return startTime2;
    }

    public String getUri() {
        return txName;
    }

    public String getHeader(String name) {
        return null;
    }

    public Dispatcher createDispatcher() {
        return new AsyncDispatcher(getTransaction(), new SimpleMetricNameFormat(getUri()));
    }

    @InterfaceMixin(originalClassName = {"com/ning/http/client/Response"})
    public static abstract interface Response {
        public static final String CLASS = "com/ning/http/client/Response";

        public abstract String getHeader(String paramString);

        public abstract List<String> getHeaders(String paramString);
    }

    @InterfaceMixin(originalClassName = {"play/api/libs/ws/Response"})
    public static abstract interface WSResponse {
        public static final String CLASS = "play/api/libs/ws/Response";

        @FieldAccessor(fieldName = "ahcResponse", fieldDesc = "Lcom/ning/http/client/Response;", existingField = true)
        public abstract Object _nr_response();
    }
}