package com.newrelic.agent.install;

import com.newrelic.agent.util.EditableFile;

public class JBoss7SelfInstaller extends SelfInstaller {
    private final String scriptPath = "/bin/standalone";

    private final String agentAlreadySet = "(.*)JAVA_OPTS=(.*)\\-javaagent:(.*)newrelic.jar";
    private final String windowsLocator = "rem Setup JBoss specific properties";
    private String rootDir;

    public boolean backupAndEditStartScript(String appServerRootDir) {
        this.rootDir = appServerRootDir;

        boolean result = backupAndEdit(appServerRootDir + getStartScript());

        result &= backupAndEdit(appServerRootDir + getAlternateStartScript());

        return result;
    }

    private boolean backupAndEdit(String fullPathToScript) {
        try {
            EditableFile file = new EditableFile(fullPathToScript);

            String fullSwitch = getCommentForAgentSwitch(file.comment) + lineSep + getAgentSettings();

            if (!file.contains(getAgentAlreadySetExpr())) {
                backup(file);
                if (this.osIsWindows) {
                    file.insertBeforeLocator(getLocator(), fullSwitch + lineSep, true);
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
        String path = "/bin/standalone";
        if (this.osIsWindows) {
            path = path.replaceAll("/", "\\\\");
        }
        return path + (this.osIsWindows ? ".bat" : ".conf");
    }

    public String getAlternateStartScript() {
        return getStartScript();
    }

    public String getLocator() {
        return "rem Setup JBoss specific properties";
    }

    public String getAlternateLocator() {
        return getLocator();
    }

    public String getAgentSettings() {
        String unixSwitch = "JAVA_OPTS=\"$JAVA_OPTS -javaagent:" + this.rootDir + "/newrelic/newrelic.jar\"" + lineSep;
        String windowsSwitch = "set JAVA_OPTS=-javaagent:\"" + this.rootDir.replaceAll("\\\\", "/")
                                       + "/newrelic/newrelic.jar\" %JAVA_OPTS%";

        return this.osIsWindows ? windowsSwitch : unixSwitch;
    }

    public String getAgentAlreadySetExpr() {
        return "(.*)JAVA_OPTS=(.*)\\-javaagent:(.*)newrelic.jar";
    }
}