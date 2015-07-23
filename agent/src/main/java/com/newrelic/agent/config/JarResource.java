package com.newrelic.agent.config;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public abstract interface JarResource extends Closeable {
    public abstract InputStream getInputStream(String paramString) throws IOException;

    public abstract long getSize(String paramString);
}