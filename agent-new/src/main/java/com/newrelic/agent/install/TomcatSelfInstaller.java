package com.newrelic.agent.install;

import java.io.File;

import com.newrelic.agent.util.EditableFile;

public class TomcatSelfInstaller extends SelfInstaller {
    private final String scriptPath = "/bin/catalina";
    private final String altScriptPath = "/bin/catalina.50";

    private final String unixAgentSet = "^(export )?CATALINA_OPTS=(.*)\\-javaagent:(.*)newrelic.jar(.*)$";
    private final String unixLocator = "^(export )?CATALINA_OPTS=(.*)$";
    private final String altUnixLocator = "^# OS specific support(.*)$";
    private final String windowsAgentSet = "^(SET|set) CATALINA_OPTS=%CATALINA_OPTS% \\-javaagent:(.*)$";
    private final String winLocator = "^rem Guess CATALINA_HOME if not defined(.*)$";
    private final String altWinLocator = "^rem Suppress Terminate batch job(.*)$";
    private String rootDir;

    public boolean backupAndEditStartScript(String appServerRootDir) {
        this.rootDir = appServerRootDir;

        boolean result = backupAndEdit(appServerRootDir + getStartScript());

        File catalina50 = new File(appServerRootDir + getAlternateStartScript());
        if (catalina50.exists()) {
            result &= backupAndEdit(catalina50.toString());
        }

        return result;
    }

    private boolean backupAndEdit(String fullPathToScript) {
        try {
            EditableFile file = new EditableFile(fullPathToScript);

            String fullSwitch = lineSep + getCommentForAgentSwitch(file.comment) + lineSep + getAgentSettings();

            if (!file.contains(getAgentAlreadySetExpr())) {
                if (file.contains(getLocator())) {
                    backup(file);
                    if (this.osIsWindows) {
                        file.insertBeforeLocator(getLocator(), fullSwitch, true);
                    } else {
                        file.insertAfterLocator(getLocator(), fullSwitch, true);
                    }
                    System.out.println("Added agent switch to start script " + file.getLocation());
                } else if (file.contains(getAlternateLocator())) {
                    backup(file);
                    file.insertBeforeLocator(getAlternateLocator(), fullSwitch, true);
                    System.out.println("Added agent switch to start script " + file.getLocation());
                } else {
                    System.out.println("Did not locate Tomcat start script. No edit performed");
                    return false;
                }
            } else {
                System.out.println("Did not edit start script " + file.getLocation() + " because:");
                System.out.println(" .:. The agent switch is already set");
            }

            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    public String getStartScript() {
        String path = "/bin/catalina";
        if (this.osIsWindows) {
            path = path.replaceAll("/", "\\\\");
        }
        return path + (this.osIsWindows ? ".bat" : ".sh");
    }

    public String getAlternateStartScript() {
        return "/bin/catalina.50" + (this.osIsWindows ? ".bat" : ".sh");
    }

    public String getLocator() {
        return this.osIsWindows ? "^rem Guess CATALINA_HOME if not defined(.*)$" : "^(export )?CATALINA_OPTS=(.*)$";
    }

    public String getAlternateLocator() {
        return this.osIsWindows ? "^rem Suppress Terminate batch job(.*)$" : "^# OS specific support(.*)$";
    }

    public String getAgentSettings() {
        String unixSwitch = "NR_JAR=" + this.rootDir + "/newrelic/newrelic.jar; export NR_JAR" + lineSep
                                    + "CATALINA_OPTS=\"\\$CATALINA_OPTS -javaagent:\\$NR_JAR\"; export CATALINA_OPTS"
                                    + lineSep;

        String winSwitch = "set CATALINA_OPTS=%CATALINA_OPTS% -javaagent:\"" + this.rootDir.replaceAll("\\\\", "/")
                                   + "/newrelic/newrelic.jar\"" + lineSep;

        return this.osIsWindows ? winSwitch : unixSwitch;
    }

    public String getAgentAlreadySetExpr() {
        return this.osIsWindows ? "^(SET|set) CATALINA_OPTS=%CATALINA_OPTS% \\-javaagent:(.*)$"
                       : "^(export )?CATALINA_OPTS=(.*)\\-javaagent:(.*)newrelic.jar(.*)$";
    }
}