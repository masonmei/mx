package com.newrelic.agent.install;

import com.newrelic.agent.util.EditableFile;

public class GlassfishSelfInstaller extends SelfInstaller {
    private final String scriptPath = "/config/domain.xml";

    private final String agentAlreadySet = "(.*)\\-javaagent:(.*)newrelic.jar(.*)";
    private final String locatorString = "(^.*java-config.*$)";

    public boolean backupAndEditStartScript(String appServerRootDir) {
        return backupAndEdit(appServerRootDir + getStartScript());
    }

    private boolean backupAndEdit(String fullPathToScript) {
        try {
            EditableFile file = new EditableFile(fullPathToScript);

            if (!file.contains(getAgentAlreadySetExpr())) {
                backup(file);
                file.insertAfterLocator(getLocator(), getAgentSettings(), true);
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
        return "/config/domain.xml";
    }

    public String getAlternateStartScript() {
        return getStartScript();
    }

    public String getLocator() {
        return "(^.*java-config.*$)";
    }

    public String getAlternateLocator() {
        return getLocator();
    }

    public String getAgentSettings() {
        return "        <jvm-options>-javaagent:\\${com.sun.aas.instanceRoot}/newrelic/newrelic.jar</jvm-options>";
    }

    public String getAgentAlreadySetExpr() {
        return "(.*)\\-javaagent:(.*)newrelic.jar(.*)";
    }
}