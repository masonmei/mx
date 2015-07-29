package com.newrelic.agent.instrumentation.pointcuts.frameworks.spring;

import com.newrelic.agent.errors.AbstractExceptionHandlerPointCut;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;

@PointCut
public class SpringExceptionHandlerPointCut extends AbstractExceptionHandlerPointCut
{
  private static final String PROCESS_HANDLER_EXCEPTION_METHOD_NAME = "processHandlerException";

  public SpringExceptionHandlerPointCut(ClassTransformer classTransformer)
  {
    super(new PointCutConfiguration("spring_exception_handler", "spring_framework", true), new ExactClassMatcher("org/springframework/web/servlet/DispatcherServlet"), createMethodMatcher(new MethodMatcher[] { new ExactMethodMatcher("processHandlerException", "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Ljava/lang/Object;Ljava/lang/Exception;)Lorg/springframework/web/servlet/ModelAndView;"), new ExactMethodMatcher("triggerAfterCompletion", "(Lorg/springframework/web/servlet/HandlerExecutionChain;ILjavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Ljava/lang/Exception;)V") }));
  }

  protected Throwable getThrowable(ClassMethodSignature sig, Object[] args)
  {
    int index = "processHandlerException".equals(sig.getMethodName()) ? 3 : 4;
    return (Throwable)args[index];
  }
}