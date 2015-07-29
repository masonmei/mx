package com.newrelic.agent.instrumentation.pointcuts.play2;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;

@PointCut
public class AnormResultSetParsePointCut extends TracerFactoryPointCut
{
  private static final boolean DEFAULT_ENABLED = true;
  private static final String POINT_CUT_NAME = AnormResultSetParsePointCut.class.getName();
  static final String FUNCTION_CLASS = "anorm/Sql$";
  static final String METHOD_NAME = "as";
  static final String METHOD_DESC = "(Lanorm/ResultSetParser;Ljava/sql/ResultSet;)Ljava/lang/Object;";

  public AnormResultSetParsePointCut(ClassTransformer classTransformer)
  {
    super(createPointCutConfig(), createClassMatcher(), createMethodMatcher());
  }

  private static PointCutConfiguration createPointCutConfig() {
    return new PointCutConfiguration(POINT_CUT_NAME, "play2_instrumentation", true);
  }

  private static ClassMatcher createClassMatcher()
  {
    return new ExactClassMatcher("anorm/Sql$");
  }

  private static MethodMatcher createMethodMatcher()
  {
    return new ExactMethodMatcher("as", "(Lanorm/ResultSetParser;Ljava/sql/ResultSet;)Ljava/lang/Object;");
  }

  public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object object, Object[] args)
  {
    if (!transaction.isStarted()) {
      Transaction.clearTransaction();
      return null;
    }
    return new DefaultTracer(transaction, sig, object, new ClassMethodMetricNameFormat(sig, object));
  }
}