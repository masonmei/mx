package com.newrelic.agent.instrumentation.pointcuts.container.tomcat;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.PointCutInvocationHandler;

@com.newrelic.agent.instrumentation.pointcuts.PointCut
public class PrepareResponsePointCut extends com.newrelic.agent.instrumentation.PointCut
  implements EntryInvocationHandler
{
  private static final String POINT_CUT_NAME = PrepareResponsePointCut.class.getName();
  private static final boolean DEFAULT_ENABLED = true;
  private static final String COYOTE_ABSTRACT_HTTP11_PROCESSOR_CLASS = "org/apache/coyote/http11/AbstractHttp11Processor";
  private static final String COYOTE_HTTP11_PROCESSOR_CLASS = "org/apache/coyote/http11/Http11Processor";
  private static final String GRIZZLY_PROCESSOR_TASK_CLASS = "com/sun/grizzly/http/ProcessorTask";
  private static final String PREPARE_RESPONSE_METHOD_NAME = "prepareResponse";
  private static final String PREPARE_RESPONSE_METHOD_DESC = "()V";

  public PrepareResponsePointCut(ClassTransformer classTransformer)
  {
    super(createPointCutConfig(), createClassMatcher(), createMethodMatcher());
  }

  private static PointCutConfiguration createPointCutConfig() {
    return new PointCutConfiguration(POINT_CUT_NAME, "tomcat", true);
  }

  private static ClassMatcher createClassMatcher()
  {
    return ExactClassMatcher.or(new String[] { "org/apache/coyote/http11/AbstractHttp11Processor", "org/apache/coyote/http11/Http11Processor", "com/sun/grizzly/http/ProcessorTask" });
  }

  private static MethodMatcher createMethodMatcher()
  {
    return new ExactMethodMatcher("prepareResponse", "()V");
  }

  protected PointCutInvocationHandler getPointCutInvocationHandlerImpl()
  {
    return this;
  }

  public void handleInvocation(ClassMethodSignature sig, Object object, Object[] args)
  {
    Transaction tx = Transaction.getTransaction();
    if (tx == null) {
      return;
    }
    tx.beforeSendResponseHeaders();
  }
}