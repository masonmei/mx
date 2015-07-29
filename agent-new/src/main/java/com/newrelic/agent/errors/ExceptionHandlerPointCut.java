package com.newrelic.agent.errors;

import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.tracers.ClassMethodSignature;

public final class ExceptionHandlerPointCut extends AbstractExceptionHandlerPointCut
{
  private final int exceptionArgumentIndex;

  public ExceptionHandlerPointCut(ExceptionHandlerSignature sig)
  {
    super(new PointCutConfiguration("exception_handler"), sig.getClassMatcher(), sig.getMethodMatcher());
    this.exceptionArgumentIndex = sig.getExceptionArgumentIndex();
  }

  protected Throwable getThrowable(ClassMethodSignature sig, Object[] args)
  {
    if (this.exceptionArgumentIndex >= 0) {
      return (Throwable)args[this.exceptionArgumentIndex];
    }
    for (int i = 0; i < args.length; i++) {
      if ((args[i] instanceof Throwable)) {
        return (Throwable)args[i];
      }
    }
    return null;
  }
}