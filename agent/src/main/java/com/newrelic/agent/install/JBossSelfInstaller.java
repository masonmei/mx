package com.newrelic.agent.install;

import com.newrelic.agent.util.EditableFile;

public class JBossSelfInstaller extends SelfInstaller {
    private final String scriptPath = "/bin/run";

    private final String agentAlreadySet = "(.*)JAVA_OPTS=(.*)\\-javaagent:(.*)newrelic.jar";
    private final String windowsLocator = "set JBOSS_CLASSPATH=%RUN_CLASSPATH%";
    private String rootDir;

    public boolean backupAndEditStartScript(String appServerRootDir) {
        rootDir = appServerRootDir;
        return backupAndEdit(appServerRootDir + getStartScript());
    }

    private boolean backupAndEdit(String fullPathToScript) {
        try {
            EditableFile file = new EditableFile(fullPathToScript);

            String fullSwitch = getCommentForAgentSwitch(file.comment) + lineSep + getAgentSettings();

            if (!file.contains(getAgentAlreadySetExpr())) {
                backup(file);
                if (osIsWindows) {
                    file.insertAfterLocator(getLocator(), lineSep + fullSwitch, true);
                } else {
                    file.append(fullSwitch + lineSep);
                }
                System.out.println("Added agent switch to start script " + file.getLocation());
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
        String path = "/bin/run";
        if (osIsWindows) {
            path = path.replaceAll("/", "\\\\");
        }
        return path + (osIsWindows ? ".bat" : ".conf");
    }

    public String getAlternateStartScript() {
        return getStartScript();
    }

    public String getLocator() {
        return "set JBOSS_CLASSPATH=%RUN_CLASSPATH%";
    }

    public String getAlternateLocator() {
        return getLocator();
    }

    public String getAgentSettings() {
        String unixSwitch = "JAVA_OPTS=\"$JAVA_OPTS -javaagent:" + rootDir + "/newrelic/newrelic.jar\"" + lineSep;
        String windowsSwitch = "set JAVA_OPTS=-javaagent:\"" + rootDir.replaceAll("\\\\", "/")
                                       + "/newrelic/newrelic.jar\" %JAVA_OPTS%";

        return osIsWindows ? windowsSwitch : unixSwitch;
    }

    public String getAgentAlreadySetExpr() {
        return "(.*)JAVA_OPTS=(.*)\\-javaagent:(.*)newrelic.jar";
    }
}