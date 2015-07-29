package com.newrelic.agent.install;

public class SelfInstallerFactory {
    public static SelfInstaller getSelfInstaller(AppServerIdentifier.AppServerType type) throws Exception {
        if (type == AppServerIdentifier.AppServerType.TOMCAT) {
            return new TomcatSelfInstaller();
        }
        if (type == AppServerIdentifier.AppServerType.JETTY) {
            return new JettySelfInstaller();
        }
        if (type == AppServerIdentifier.AppServerType.JBOSS) {
            return new JBossSelfInstaller();
        }
        if (type == AppServerIdentifier.AppServerType.JBOSS7) {
            return new JBoss7SelfInstaller();
        }
        if (type == AppServerIdentifier.AppServerType.GLASSFISH) {
            return new GlassfishSelfInstaller();
        }

        throw new Exception("Unknown app server type");
    }
}