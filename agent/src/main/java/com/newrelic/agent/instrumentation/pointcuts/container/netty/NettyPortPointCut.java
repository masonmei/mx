package com.newrelic.agent.instrumentation.pointcuts.container.netty;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.PointCutInvocationHandler;

@com.newrelic.agent.instrumentation.pointcuts.PointCut
public class NettyPortPointCut extends com.newrelic.agent.instrumentation.PointCut implements EntryInvocationHandler {
    private static final String POINT_CUT_NAME = NettyPortPointCut.class.getName();
    private static final boolean DEFAULT_ENABLED = true;
    private static final String CLASS = "org/jboss/netty/bootstrap/ServerBootstrap";
    private static final String METHOD_NAME = "bind";
    private static final String METHOD_DESC = "(Ljava/net/SocketAddress;)Lorg/jboss/netty/channel/Channel;";

    public NettyPortPointCut(ClassTransformer classTransformer) {
        super(createPointCutConfig(), createClassMatcher(), createMethodMatcher());
    }

    private static PointCutConfiguration createPointCutConfig() {
        return new PointCutConfiguration(POINT_CUT_NAME, "netty_instrumentation", true);
    }

    private static ClassMatcher createClassMatcher() {
        return new ExactClassMatcher("org/jboss/netty/bootstrap/ServerBootstrap");
    }

    private static MethodMatcher createMethodMatcher() {
        return new ExactMethodMatcher("bind", "(Ljava/net/SocketAddress;)Lorg/jboss/netty/channel/Channel;");
    }

    protected PointCutInvocationHandler getPointCutInvocationHandlerImpl() {
        return this;
    }

    public void handleInvocation(ClassMethodSignature sig, Object object, Object[] args) {
        try {
            InetSocketAddress socketAddress = (InetSocketAddress) args[0];
            AgentBridge.privateApi.setAppServerPort(socketAddress.getPort());
        } catch (Exception e) {
            Agent.LOG.log(Level.FINE, "Unable to get Netty port number", e);
        }

        String playVersion = System.getProperty("play.version");
        if (playVersion != null) {
            ServiceFactory.getEnvironmentService().getEnvironment().setServerInfo("Play", playVersion);
        } else {
            String version;
            try {
                Class versionClass = object.getClass().getClassLoader().loadClass("org.jboss.netty.util.Version");
                Field versionField = versionClass.getField("ID");
                version = (String) versionField.get(null);
            } catch (Throwable e) {
                version = null;
            }
            ServiceFactory.getEnvironmentService().getEnvironment().setServerInfo("Netty", version);
        }
    }
}