package com.newrelic.agent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.newrelic.agent.extension.ConfigurationConstruct;
import com.newrelic.agent.extension.ExtensionService;
import com.newrelic.agent.instrumentation.yaml.PointCutFactory;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.IgnoreTransactionTracerFactory;
import com.newrelic.agent.tracers.PointCutInvocationHandler;
import com.newrelic.agent.tracers.RetryException;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TracerFactory;

public class TracerService extends AbstractService {
    private final Map<String, TracerFactory> tracerFactories = new ConcurrentHashMap<String, TracerFactory>();
    public ITracerService tracerServiceFactory;
    private volatile PointCutInvocationHandler[] invocationHandlers = new PointCutInvocationHandler[0];

    public TracerService() {
        super(TracerService.class.getSimpleName());
        registerTracerFactory(IgnoreTransactionTracerFactory.TRACER_FACTORY_NAME, new IgnoreTransactionTracerFactory());

        ExtensionService extensionService = ServiceFactory.getExtensionService();
        for (ConfigurationConstruct construct : PointCutFactory.getConstructs()) {
            extensionService.addConstruct(construct);
        }
        this.tracerServiceFactory = new NoOpTracerService();
    }

    public Tracer getTracer(TracerFactory tracerFactory, ClassMethodSignature signature, Object object, Object[] args) {
        if (tracerFactory == null) {
            return null;
        }
        return this.tracerServiceFactory.getTracer(tracerFactory, signature, object, args);
    }

    public TracerFactory getTracerFactory(String tracerFactoryName) {
        return this.tracerFactories.get(tracerFactoryName);
    }

    public void registerTracerFactory(String name, TracerFactory tracerFactory) {
        this.tracerFactories.put(name.intern(), tracerFactory);
    }

    public void registerInvocationHandlers(List<PointCutInvocationHandler> handlers) {
        if (this.invocationHandlers == null) {
            this.invocationHandlers = handlers.toArray(new PointCutInvocationHandler[handlers.size()]);
        } else {
            PointCutInvocationHandler[] arrayToSwap =
                    new PointCutInvocationHandler[this.invocationHandlers.length + handlers.size()];

            System.arraycopy(this.invocationHandlers, 0, arrayToSwap, 0, this.invocationHandlers.length);
            System.arraycopy(handlers.toArray(), 0, arrayToSwap, this.invocationHandlers.length, handlers.size());
            this.invocationHandlers = arrayToSwap;
        }
    }

    public int getInvocationHandlerId(PointCutInvocationHandler handler) {
        for (int i = 0; i < this.invocationHandlers.length; i++) {
            if (this.invocationHandlers[i] == handler) {
                return i;
            }
        }
        return -1;
    }

    public PointCutInvocationHandler getInvocationHandler(int id) {
        return this.invocationHandlers[id];
    }

    protected void doStart() {
    }

    protected void doStop() {
        this.tracerFactories.clear();
    }

    public boolean isEnabled() {
        return true;
    }

    private interface ITracerService {
        Tracer getTracer(TracerFactory paramTracerFactory, ClassMethodSignature paramClassMethodSignature,
                         Object paramObject, Object[] paramArrayOfObject);
    }

    private class TracerServiceImpl implements ITracerService {
        private TracerServiceImpl() {
        }

        public Tracer getTracer(TracerFactory tracerFactory, ClassMethodSignature signature, Object object,
                                Object[] args) {
            Transaction transaction = Transaction.getTransaction();
            if (transaction == null) {
                return null;
            }
            try {
                return transaction.getTransactionState().getTracer(transaction, tracerFactory, signature, object, args);
            } catch (RetryException e) {
            }
            return getTracer(tracerFactory, signature, object, args);
        }
    }

    private class NoOpTracerService implements ITracerService {
        private NoOpTracerService() {
        }

        public Tracer getTracer(TracerFactory tracerFactory, ClassMethodSignature signature, Object object,
                                Object[] args) {
            if (ServiceFactory.getServiceManager().isStarted()) {
                TracerService.this.tracerServiceFactory = new TracerServiceImpl();
                return TracerService.this.tracerServiceFactory.getTracer(tracerFactory, signature, object, args);
            }
            return null;
        }
    }
}