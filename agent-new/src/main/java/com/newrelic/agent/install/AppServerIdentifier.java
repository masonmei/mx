package com.newrelic.agent.install;

import java.io.File;

public class AppServerIdentifier
{
  private static String tomcat5Marker = "/server/lib/catalina.jar";
  private static String tomcat67Marker = "/lib/catalina.jar";

  private static String jettyMarker = "/bin/jetty.sh";

  private static String jBoss4Marker = "/lib/jboss-common.jar";
  private static String jBoss56Marker = "/lib/jboss-common-core.jar";
  private static String jBoss7Marker = "/jboss-modules.jar";

  private static String glassfish3Marker = "/config/domain.xml";

  public static AppServerType getAppServerType(String path) throws Exception {
    return getAppServerType(new File(path));
  }

  public static AppServerType getAppServerType(File rootDir)
    throws Exception
  {
    if (!rootDir.exists()) {
      throw new Exception("App server root " + rootDir.toString() + "does not exist on filesystem.");
    }
    if (!rootDir.isDirectory()) {
      throw new Exception("App server root " + rootDir.toString() + "is not a directory.");
    }

    if (isTomcat(rootDir.toString())) {
      return AppServerType.TOMCAT;
    }

    if (isJetty(rootDir.toString())) {
      return AppServerType.JETTY;
    }

    if (isJBoss(rootDir.toString())) {
      return AppServerType.JBOSS;
    }

    if (isJBoss7(rootDir.toString())) {
      return AppServerType.JBOSS7;
    }

    if (isGlassfish(rootDir.toString())) {
      return AppServerType.GLASSFISH;
    }

    return AppServerType.UNKNOWN;
  }

  private static boolean isTomcat(String rootDir)
  {
    return (markerFileExists(rootDir + tomcat5Marker)) || (markerFileExists(rootDir + tomcat67Marker));
  }

  private static boolean isJetty(String rootDir)
  {
    return markerFileExists(rootDir + jettyMarker);
  }

  private static boolean isJBoss(String rootDir)
  {
    return (markerFileExists(rootDir + jBoss4Marker)) || (markerFileExists(rootDir + jBoss56Marker));
  }

  private static boolean isJBoss7(String rootDir)
  {
    return markerFileExists(rootDir + jBoss7Marker);
  }

  private static boolean isGlassfish(String rootDir)
  {
    return markerFileExists(rootDir + glassfish3Marker);
  }

  private static boolean markerFileExists(String path)
  {
    File markerFile = new File(path);
    if ((markerFile.exists()) && (markerFile.isFile())) {
      return true;
    }
    return false;
  }

  public static enum AppServerType
  {
    TOMCAT("Tomcat"), JETTY("Jetty"), JBOSS("JBoss"), JBOSS7("JBoss7"), GLASSFISH("Glassfish"), UNKNOWN("Unknown");

    private final String name;

    private AppServerType(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }
  }
}