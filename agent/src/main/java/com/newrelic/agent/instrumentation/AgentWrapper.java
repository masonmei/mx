package com.newrelic.agent.instrumentation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.IAgent;
import com.newrelic.agent.TracerService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.PointCutInvocationHandler;
import com.newrelic.agent.tracers.TracerFactory;

public class AgentWrapper implements InvocationHandler {
    public static final String CLASSLOADER_KEY = "CLASSLOADER";
    public static final String SUCCESSFUL_METHOD_INVOCATION = "s";
    public static final String UNSUCCESSFUL_METHOD_INVOCATION = "u";
    private final TracerService tracerService;
    private final IAgent agent;
    private final IAgentLogger logger;
    private final ClassTransformer classTransformer;

    private AgentWrapper(ClassTransformer classTransformer) {
        tracerService = ServiceFactory.getTracerService();
        this.classTransformer = classTransformer;
        agent = ServiceFactory.getAgent();
        logger = Agent.LOG.getChildLogger("com.newrelic.agent.InvocationHandler");
    }

    public static AgentWrapper getAgentWrapper(ClassTransformer classTransformer) {
        return new AgentWrapper(classTransformer);
    }

    public static ExitTracer invoke(PointCutInvocationHandler invocationHandler, String className, String methodName,
                                    String methodDesc, Object invocationTarget, Object[] args) {
        ClassMethodSignature classMethodSig = new ClassMethodSignature(className, methodName, methodDesc);
        if ((invocationHandler instanceof EntryInvocationHandler)) {
            EntryInvocationHandler handler = (EntryInvocationHandler) invocationHandler;

            handler.handleInvocation(classMethodSig, invocationTarget, args);
            return null;
        }
        if ((invocationHandler instanceof TracerFactory)) {
            return ServiceFactory.getTracerService()
                           .getTracer((TracerFactory) invocationHandler, classMethodSig, invocationTarget, args);
        }

        return null;
    }

    public Object invoke(Object proxy, Method method, Object[] args) {
        if (CLASSLOADER_KEY == proxy) {
            return Agent.getClassLoader();
        }
        if (!agent.isEnabled()) {
            return NoOpInvocationHandler.INVOCATION_HANDLER;
        }
        try {
            if ((proxy instanceof Class)) {
                return createInvocationHandler(proxy, args);
            }
            if ((proxy instanceof Integer)) {
                PointCutInvocationHandler invocationHandler = tracerService.getInvocationHandler((Integer) proxy);
                return invoke(invocationHandler, (String) args[0], (String) args[1], (String) args[2], args[3],
                                     (Object[]) args[4]);
            }

            logger.log(Level.FINEST, "Unknown invocation type " + proxy);
        } catch (Throwable ex) {
            logger.log(Level.FINEST, "Error initializing invocation point", ex);
        }

        return NoOpInvocationHandler.INVOCATION_HANDLER;
    }

    private Object createInvocationHandler(Object proxy, Object[] args) {
        boolean ignoreTransaction = (Boolean) args[4];
        if (ignoreTransaction) {
            return IgnoreTransactionHandler.IGNORE_TRANSACTION_INVOCATION_HANDLER;
        }
        return classTransformer
                       .evaluate((Class) proxy, tracerService, args[0], args[1], args[2], (Boolean) args[3], args);
    }

    private static class IgnoreTransactionHandler implements InvocationHandler {
        static final InvocationHandler IGNORE_TRANSACTION_INVOCATION_HANDLER = new IgnoreTransactionHandler();

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Transaction tx = Transaction.getTransaction();
            if (tx != null) {
                tx.setIgnore(true);
            }
            return NoOpInvocationHandler.INVOCATION_HANDLER;
        }
    }
}