package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.agent.util.asm.ClassResolver;
import com.newrelic.agent.util.asm.ClassResolvers;
import com.newrelic.agent.util.asm.ClassStructure;
import com.newrelic.api.agent.Logger;
import java.io.IOException;
import java.io.InputStream;

public class AgentClassStructureResolver extends ClassStructureResolver
{
  private static final ClassResolver EMBEDDED_CLASS_RESOLVER = ClassResolvers.getEmbeddedJarsClassResolver();
  private final ClassResolver embeddedClassResolver;

  public AgentClassStructureResolver()
  {
    this(EMBEDDED_CLASS_RESOLVER);
  }

  AgentClassStructureResolver(ClassResolver embeddedClassResolver)
  {
    this.embeddedClassResolver = embeddedClassResolver;
  }

  public ClassStructure getClassStructure(Logger logger, ClassLoader loader, String internalName, int flags)
    throws IOException
  {
    InputStream classResource = this.embeddedClassResolver.getClassResource(internalName);
    if (classResource != null) {
      try {
        return ClassStructure.getClassStructure(new ClassReader(classResource), flags);
      } finally {
        classResource.close();
      }
    }

    return super.getClassStructure(logger, loader, internalName, flags);
  }
}