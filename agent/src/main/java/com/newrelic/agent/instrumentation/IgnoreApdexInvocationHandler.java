package com.newrelic.agent.instrumentation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.dispatchers.Dispatcher;

class IgnoreApdexInvocationHandler implements InvocationHandler {
    static final InvocationHandler INVOCATION_HANDLER = new IgnoreApdexInvocationHandler();

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Transaction transaction = Transaction.getTransaction();
        if (transaction != null) {
            Dispatcher dispatcher = transaction.getDispatcher();
            if (dispatcher != null) {
                dispatcher.setIgnoreApdex(true);
                if (Agent.LOG.isLoggable(Level.FINER)) {
                    String msg = MessageFormat.format("Set Ignore apdex to \"{0}\"", Boolean.valueOf(true));
                    Agent.LOG.log(Level.FINER, msg, new Exception());
                }
            }
        }
        return null;
    }
}