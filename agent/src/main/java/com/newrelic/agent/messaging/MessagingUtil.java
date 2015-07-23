package com.newrelic.agent.messaging;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.service.ServiceFactory;

public final class MessagingUtil {
    public static void recordParameters(Transaction tx, Map<String, String> requestParameters) {
        if (tx.isIgnore()) {
            return;
        }
        if (!ServiceFactory.getAttributesService().captureMessageParams(tx.getApplicationName())) {
            return;
        }

        if (requestParameters.isEmpty()) {
            return;
        }

        tx.getPrefixedAgentAttributes().put("message.parameters.", filterMessageParameters(requestParameters,
                                                                                                  tx.getAgentConfig()
                                                                                                          .getMaxUserParameterSize()));
    }

    static Map<String, String> filterMessageParameters(Map<String, String> messageParams, int maxSizeLimit) {
        Map atts = new LinkedHashMap();

        for (Entry current : messageParams.entrySet()) {
            if (((String) current.getKey()).length() > maxSizeLimit) {
                Agent.LOG.log(Level.FINER,
                                     "Rejecting request parameter with key \"{0}\" because the key is over the size "
                                             + "limit of {1}",
                                     new Object[] {current.getKey(), Integer.valueOf(maxSizeLimit)});
            } else {
                String value = getValue((String) current.getValue(), maxSizeLimit);
                if (value != null) {
                    atts.put(current.getKey(), value);
                }
            }
        }

        return atts;
    }

    private static String getValue(String value, int maxSizeLimit) {
        if ((value == null) || (value.length() == 0)) {
            return null;
        }
        if (value.length() > maxSizeLimit) {
            value = value.substring(0, maxSizeLimit);
        }
        return value;
    }
}