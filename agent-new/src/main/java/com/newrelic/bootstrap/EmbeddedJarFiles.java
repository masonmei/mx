package com.newrelic.bootstrap;

import java.io.File;
import java.io.IOException;

public interface EmbeddedJarFiles {
    String[] getEmbeddedAgentJarFileNames();

    File getJarFileInAgent(String paramString) throws IOException;
}