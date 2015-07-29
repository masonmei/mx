package com.newrelic.agent.instrumentation.pointcuts.database;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.transaction.ConnectionCache;
import java.sql.Connection;

@PointCut
public class ConnectionPointCut extends TracerFactoryPointCut
{
  public ConnectionPointCut(ClassTransformer classTransformer)
  {
    super(ConnectionPointCut.class, ExactClassMatcher.or(new String[] { "com/newrelic/agent/deps/org/apache/commons/dbcp/PoolingDataSource$PoolGuardConnectionWrapper", "com/newrelic/agent/deps/org/apache/commons/dbcp/PoolableConnection", "org/apache/tomcat/dbcp/dbcp/cpdsadapter/ConnectionImpl", "org/jboss/resource/adapter/jdbc/WrappedConnection", "org/postgresql/jdbc2/AbstractJdbc2Connection", "weblogic/jdbc/pool/Connection", "org/postgresql/jdbc3/Jdbc3Connection", "com/mysql/jdbc/JDBC4Connection" }), createExactMethodMatcher("close", new String[] { "()V" }));
  }

  protected boolean isDispatcher()
  {
    return true;
  }

  public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object connection, Object[] args)
  {
    transaction.getConnectionCache().removeConnectionFactory((Connection)connection);
    return null;
  }
}