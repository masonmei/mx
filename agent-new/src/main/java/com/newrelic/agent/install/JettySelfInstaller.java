package com.newrelic.agent.install;

import com.newrelic.agent.util.EditableFile;
import java.io.File;
import java.io.PrintStream;

public class JettySelfInstaller extends SelfInstaller
{
  private final String scriptPath = "/bin/jetty";
  private final String altScriptPath = "/bin/jetty-cygwin";

  private final String agentAlreadySet = "^(export )?JAVA_OPTIONS=(.*)\\-javaagent:(.*)newrelic.jar(.*)$";
  private final String winlinLocator = "[# \r\n]+ This is how the Jetty server will be started([ \t]+)?";
  private String rootDir;

  public boolean backupAndEditStartScript(String appServerRootDir)
  {
    this.rootDir = appServerRootDir;

    boolean result = backupAndEdit(appServerRootDir + getStartScript());

    File jettyCygwin = new File(appServerRootDir + getAlternateStartScript());
    if (jettyCygwin.exists()) {
      result &= backupAndEdit(jettyCygwin.toString());
    }

    return result;
  }

  private boolean backupAndEdit(String fullPathToScript)
  {
    try {
      EditableFile file = new EditableFile(fullPathToScript);

      String fullSwitch = lineSep + lineSep + getCommentForAgentSwitch(file.comment) + lineSep + getAgentSettings();

      if (!file.contains(getAgentAlreadySetExpr()))
      {
        if (file.contains(getLocator())) {
          backup(file);
          file.insertBeforeLocator(getLocator(), fullSwitch, false);
          System.out.println("Added agent switch to start script " + file.getLocation());
        } else {
          System.out.println("Did not locate Jetty start script. No edit performed");
        }
      }
      else {
        System.out.println("Did not edit start script " + file.getLocation() + " because:");
        System.out.println(" .:. The agent switch is already set");
      }

      return true;
    }
    catch (Exception e) {
      System.out.println(e.getMessage());
    }return false;
  }

  public String getStartScript()
  {
    String path = "/bin/jetty";
    if (this.osIsWindows) {
      path = path.replaceAll("/", "\\\\");
    }
    return path + ".sh";
  }

  public String getAlternateStartScript()
  {
    String path = "/bin/jetty-cygwin";
    if (this.osIsWindows) {
      path = path.replaceAll("/", "\\\\");
    }
    return path + ".sh";
  }

  public String getLocator()
  {
    return "[# \r\n]+ This is how the Jetty server will be started([ \t]+)?";
  }

  public String getAlternateLocator()
  {
    return getLocator();
  }

  public String getAgentSettings()
  {
    String switchPath = this.rootDir;
    if (this.osIsWindows) {
      switchPath = switchPath.replaceAll("\\\\", "/");
    }
    return "NR_JAR=" + switchPath + "/newrelic/newrelic.jar; export NR_JAR" + lineSep + "JAVA_OPTIONS=\"\\$\\{JAVA_OPTIONS\\} -javaagent:\\$NR_JAR\"; export JAVA_OPTIONS";
  }

  public String getAgentAlreadySetExpr()
  {
    return "^(export )?JAVA_OPTIONS=(.*)\\-javaagent:(.*)newrelic.jar(.*)$";
  }
}