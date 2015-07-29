package com.newrelic.bootstrap;

import java.io.File;
import java.io.IOException;

public abstract interface EmbeddedJarFiles {
    public abstract String[] getEmbeddedAgentJarFileNames();

    public abstract File getJarFileInAgent(String paramString) throws IOException;
}