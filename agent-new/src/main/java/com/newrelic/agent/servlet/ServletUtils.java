package com.newrelic.agent.servlet;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.StackTraces;
import com.newrelic.api.agent.Request;

public class ServletUtils {
    public static final String NEWRELIC_IGNORE_ATTRIBUTE_NAME = "com.newrelic.agent.IGNORE";
    public static final String NEWRELIC_IGNORE_APDEX_ATTRIBUTE_NAME = "com.newrelic.agent.IGNORE_APDEX";
    public static final String APPLICATION_NAME_PARAM = "com.newrelic.agent.APPLICATION_NAME";
    public static final String TRANSACTION_NAME_PARAM = "com.newrelic.agent.TRANSACTION_NAME";
    private static String SERVLET_EXCEPTION_CLASS_NAME = "javax.servlet.ServletException";

    public static Throwable getReportError(Throwable throwable) {
        if ((throwable != null) && (SERVLET_EXCEPTION_CLASS_NAME.equals(throwable.getClass().getName()))) {
            return StackTraces.getRootCause(throwable);
        }

        return throwable;
    }

    public static Map<String, String> getSimpleParameterMap(Map<String, String[]> parameterMap, int maxSizeLimit) {
        if ((parameterMap == null) || (parameterMap.isEmpty())) {
            return Collections.emptyMap();
        }
        Map parameters = new HashMap();
        for (Entry entry : parameterMap.entrySet()) {
            String name = (String) entry.getKey();
            String[] values = (String[]) entry.getValue();
            String value = getValue(values, maxSizeLimit);
            if (value != null) {
                parameters.put(name, value);
            }
        }
        return parameters;
    }

    public static void recordParameters(Transaction tx, Request request) {
        if (tx.isIgnore()) {
            return;
        }

        if (!ServiceFactory.getAttributesService().captureRequestParams(tx.getApplicationName())) {
            return;
        }
        Map requestParameters = getRequestParameterMap(request, tx.getAgentConfig().getMaxUserParameterSize());

        if (requestParameters.isEmpty()) {
            return;
        }
        tx.getPrefixedAgentAttributes().put("request.parameters.", requestParameters);
    }

    static Map<String, String> getRequestParameterMap(Request request, int maxSizeLimit) {
        Enumeration nameEnumeration = request.getParameterNames();
        if ((nameEnumeration == null) || (!nameEnumeration.hasMoreElements())) {
            return Collections.emptyMap();
        }
        Map requestParameters = new HashMap();

        while (nameEnumeration.hasMoreElements()) {
            String name = nameEnumeration.nextElement().toString();
            if (name.length() > maxSizeLimit) {
                Agent.LOG.log(Level.FINER,
                                     "Rejecting request parameter with key \"{0}\" because the key is over the size "
                                             + "limit of {1}",
                                     new Object[] {name, Integer.valueOf(maxSizeLimit)});
            } else {
                String[] values = request.getParameterValues(name);
                String value = getValue(values, maxSizeLimit);
                if (value != null) {
                    requestParameters.put(name, value);
                }
            }
        }
        return requestParameters;
    }

    private static String getValue(String[] values, int maxSizeLimit) {
        if ((values == null) || (values.length == 0)) {
            return null;
        }
        String value = values.length == 1 ? values[0] : Arrays.asList(values).toString();
        if ((value != null) && (value.length() > maxSizeLimit)) {
            if (values.length == 1) {
                value = value.substring(0, maxSizeLimit);
            } else {
                value = value.substring(0, maxSizeLimit - 1) + ']';
            }
        }
        return value;
    }
}