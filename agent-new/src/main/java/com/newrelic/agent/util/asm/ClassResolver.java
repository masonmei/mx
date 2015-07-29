package com.newrelic.agent.util.asm;

import java.io.IOException;
import java.io.InputStream;

public abstract interface ClassResolver {
    public abstract InputStream getClassResource(String paramString) throws IOException;
}