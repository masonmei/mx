package com.newrelic.agent.instrumentation.weaver;

import java.io.IOException;
import java.net.URL;

import com.newrelic.agent.util.BootstrapLoader;
import com.newrelic.agent.util.asm.ClassStructure;
import com.newrelic.agent.util.asm.Utils;
import com.newrelic.api.agent.Logger;

public class ClassStructureResolver {
    public ClassStructure getClassStructure(Logger logger, ClassLoader loader, String internalName, int flags)
            throws IOException {
        String classResourceName = Utils.getClassResourceName(internalName);
        URL resource = loader.getResource(classResourceName);

        if (resource == null) {
            resource = BootstrapLoader.get().getBootstrapResource(classResourceName);
        }
        return resource == null ? null : ClassStructure.getClassStructure(resource, flags);
    }
}