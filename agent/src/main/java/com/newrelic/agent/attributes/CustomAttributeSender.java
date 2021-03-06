package com.newrelic.agent.attributes;

import java.util.Map;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;

public class CustomAttributeSender extends AttributeSender {
    protected static String ATTRIBUTE_TYPE = "custom";

    protected String getAttributeType() {
        return ATTRIBUTE_TYPE;
    }

    protected Map<String, Object> getAttributeMap() throws Throwable {
        Transaction tx = Transaction.getTransaction();
        return tx.getUserAttributes();
    }

    public Object verifyParameterAndReturnValue(String key, Object value, String methodCalled) {
        try {
            if (Transaction.getTransaction().getAgentConfig().isHighSecurity()) {
                Agent.LOG.log(Level.FINER,
                                     "Unable to add {0} attribute because {1} was invoked with key \"{2}\" while in "
                                             + "high security mode.", getAttributeType(), methodCalled, key);

                return null;
            }
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINEST,
                                 "Unable to verify attribute. Exception thrown while verifying high security mode.", t);

            return null;
        }
        return super.verifyParameterAndReturnValue(key, value, methodCalled);
    }

    protected void addCustomAttributeImpl(String key, Object value, String methodName) {
        try {
            Transaction tx = Transaction.getTransaction();
            if (getAttributeMap().size() >= tx.getAgentConfig().getMaxUserParameters()) {
                Agent.LOG.log(Level.FINER, "Unable to add {0} attribute for key \"{1}\" because the limit is {2}.",
                                     getAttributeType(), key, tx.getAgentConfig().getMaxUserParameters());

                return;
            }
            super.addCustomAttributeImpl(key, value, methodName);
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINER, "Exception adding {0} parameter for key: \"{1}\": {2}", getAttributeType(), key,
                                 t);
        }
    }
}