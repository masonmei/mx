package com.newrelic.agent.instrumentation.pointcuts.container.netty;

import java.util.List;
import java.util.Map.Entry;

import com.newrelic.agent.instrumentation.pointcuts.FieldAccessor;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName = {"org/jboss/netty/handler/codec/http/DefaultHttpResponse"})
public interface NettyHttpResponse {
    @FieldAccessor(fieldName = "status", fieldDesc = "Lorg/jboss/netty/handler/codec/http/HttpResponseStatus;",
                          existingField = true)
    HttpResponseStatus _nr_status();

    List<String> getHeaders(String paramString);

    List<Entry<String, String>> getHeaders();

    void setHeader(String paramString, Object paramObject);

    @InterfaceMixin(originalClassName = {"org/jboss/netty/handler/codec/http/HttpResponseStatus"})
    interface HttpResponseStatus {
        String CLASS = "org/jboss/netty/handler/codec/http/HttpResponseStatus";

        int getCode();

        String getReasonPhrase();
    }
}