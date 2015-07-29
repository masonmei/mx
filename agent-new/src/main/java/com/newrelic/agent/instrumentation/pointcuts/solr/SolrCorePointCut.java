package com.newrelic.agent.instrumentation.pointcuts.solr;

import com.newrelic.agent.Agent;
import com.newrelic.agent.environment.Environment;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.jmx.JmxService;
import com.newrelic.agent.jmx.values.SolrJmxValues;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.PointCutInvocationHandler;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;

@com.newrelic.agent.instrumentation.pointcuts.PointCut
public class SolrCorePointCut extends com.newrelic.agent.instrumentation.PointCut
  implements EntryInvocationHandler
{
  private static final String POINT_CUT_NAME = SolrCorePointCut.class.getName();
  private static final boolean DEFAULT_ENABLED = true;
  private static final String SOLR_CORE_CLASS = "org/apache/solr/core/SolrCore";
  private static final String INIT_INDEX_METHOD_NAME = "initIndex";
  private static final String INIT_INDEX_METHOD_DESC = "()V";
  private static final String INIT_INDEX_METHOD_4_0_DESC = "(Z)V";
  private final AtomicBoolean addJmx = new AtomicBoolean(false);

  public SolrCorePointCut(ClassTransformer classTransformer) {
    super(createPointCutConfig(), createClassMatcher(), createMethodMatcher());
  }

  private static PointCutConfiguration createPointCutConfig() {
    return new PointCutConfiguration(POINT_CUT_NAME, null, true);
  }

  private static ClassMatcher createClassMatcher() {
    return new ExactClassMatcher("org/apache/solr/core/SolrCore");
  }

  private static MethodMatcher createMethodMatcher() {
    return new ExactMethodMatcher("initIndex", new String[] { "()V", "(Z)V" });
  }

  protected PointCutInvocationHandler getPointCutInvocationHandlerImpl()
  {
    return this;
  }

  public void handleInvocation(ClassMethodSignature sig, Object core, Object[] args)
  {
    Object version = null;
    try {
      InputStream iStream = core.getClass().getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF");
      try {
        Manifest manifest = new Manifest(iStream);
        version = manifest.getMainAttributes().getValue("Specification-Version");
      } finally {
        iStream.close();
      }
    } catch (Exception e) {
      Agent.LOG.fine("Unable to determine the Solr version : " + e.toString());
    }
    try {
      version = version == null ? getVersion(core) : version;
    } catch (Exception e) {
      version = "1.0";
    }
    ServiceFactory.getEnvironmentService().getEnvironment().addSolrVersion(version);

    addJmxConfig();
  }

  private void addJmxConfig() {
    try {
      if (!this.addJmx.getAndSet(true)) {
        ServiceFactory.getJmxService().addJmxFrameworkValues(new SolrJmxValues());
        if (Agent.LOG.isFinerEnabled())
          Agent.LOG.log(Level.FINER, "Added JMX for Solr");
      }
    }
    catch (Exception e) {
      String msg = MessageFormat.format("Unable to add Solr JMX metrics: {0}", new Object[] { e.toString() });
      Agent.LOG.severe(msg);
    }
  }

  private Object getVersion(Object core) throws Exception {
    return core.getClass().getMethod("getVersion", new Class[0]).invoke(core, new Object[0]);
  }
}