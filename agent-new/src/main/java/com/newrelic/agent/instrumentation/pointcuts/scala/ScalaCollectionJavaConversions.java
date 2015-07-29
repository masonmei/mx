package com.newrelic.agent.instrumentation.pointcuts.scala;

import com.newrelic.agent.Agent;
import com.newrelic.agent.cache.CacheService;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.MethodCache;
import com.newrelic.agent.util.SingleClassLoader;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ScalaCollectionJavaConversions
{
  private static final String JAVA_CONVERSIONS_CLASS = "scala.collection.JavaConversions";
  private static final String SCALA_MAP_CLASS = "scala.collection.Map";
  private static final String MAP_AS_JAVA_MAP_METHOD_NAME = "mapAsJavaMap";
  private static final String MAP_AS_JAVA_MAP_METHOD_DESC = "(Lscala/collection/Map;)Ljava/util/Map;";
  private static final String SCALA_SEQ_CLASS = "scala.collection.Seq";
  private static final String SEQ_AS_JAVA_LIST_METHOD_NAME = "seqAsJavaList";
  private static final String SEQ_AS_JAVA_LIST_METHOD_DESC = "(Lscala/collection/Seq;)Ljava/util/List;";
  private static final MethodCache mapAsJavaMapCache = ServiceFactory.getCacheService().getMethodCache("scala.collection.JavaConversions", "mapAsJavaMap", "(Lscala/collection/Map;)Ljava/util/Map;");

  private static final MethodCache seqAsJavaListCache = ServiceFactory.getCacheService().getMethodCache("scala.collection.JavaConversions", "seqAsJavaList", "(Lscala/collection/Seq;)Ljava/util/List;");

  private static final SingleClassLoader javaConversions = ServiceFactory.getCacheService().getSingleClassLoader("scala.collection.JavaConversions");

  private static final SingleClassLoader scalaMap = ServiceFactory.getCacheService().getSingleClassLoader("scala.collection.Map");

  private static final SingleClassLoader scalaSeq = ServiceFactory.getCacheService().getSingleClassLoader("scala.collection.Seq");

  public static Map asJavaMap(Object map)
  {
    try
    {
      ClassLoader cl = map.getClass().getClassLoader();
      Class javaConversionsClass = javaConversions.loadClass(cl);
      Class scalaMapClass = scalaMap.loadClass(cl);
      Method m = mapAsJavaMapCache.getDeclaredMethod(javaConversionsClass, new Class[] { scalaMapClass });
      return (Map)m.invoke(null, new Object[] { map });
    } catch (Exception e) {
      if (Agent.LOG.isLoggable(Level.FINER)) {
        String msg = MessageFormat.format("Exception converting Scala Map to Java Map: {0}", new Object[] { e });
        Agent.LOG.finer(msg);
      }
    }
    return null;
  }

  public static List asJavaList(Object list)
  {
    try {
      ClassLoader cl = list.getClass().getClassLoader();
      Class javaConversionsClass = javaConversions.loadClass(cl);
      Class scalaSeqClass = scalaSeq.loadClass(cl);
      Method m = seqAsJavaListCache.getDeclaredMethod(javaConversionsClass, new Class[] { scalaSeqClass });
      return (List)m.invoke(null, new Object[] { list });
    } catch (Exception e) {
      if (Agent.LOG.isLoggable(Level.FINER)) {
        String msg = MessageFormat.format("Exception converting Scala Seq to Java List: {0}", new Object[] { e });
        Agent.LOG.finer(msg);
      }
    }
    return null;
  }
}