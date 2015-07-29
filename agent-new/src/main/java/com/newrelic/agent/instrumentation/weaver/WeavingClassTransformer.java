package com.newrelic.agent.instrumentation.weaver;

import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.ClassWriter;

import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher;
import com.newrelic.agent.instrumentation.context.ContextClassTransformer;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.tracing.BridgeUtils;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsWorks;
import com.newrelic.agent.util.asm.ClassStructure;
import com.newrelic.agent.util.asm.Utils;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;

public class WeavingClassTransformer implements ContextClassTransformer {
  protected final InstrumentationPackage instrumentationPackage;

  protected WeavingClassTransformer(InstrumentationPackage instrumentationPackage) {
    this.instrumentationPackage = instrumentationPackage;
  }

  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                          ProtectionDomain protectionDomain, byte[] classfileBuffer, InstrumentationContext context,
                          OptimizedClassMatcher.Match match) throws IllegalClassFormatException {
    try {
      return doTransform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer, context,
                                match);
    } catch (Throwable t) {
      Agent.LOG.log(Level.SEVERE, "Unable to transform class " + className + ".  Error: " + t.toString());
      Agent.LOG.log(Level.FINE, t.toString(), t);
    }
    return null;
  }

  protected byte[] doTransform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                               ProtectionDomain protectionDomain, byte[] classfileBuffer,
                               InstrumentationContext context, OptimizedClassMatcher.Match match) throws Exception {
    String classMatch = instrumentationPackage.getClassMatch(match);
    if (classMatch == null) {
      return null;
    }

    Verifier verifier = instrumentationPackage.getVerifier();

    if (!verifier.isEnabled(loader)) {
      return null;
    }

    MixinClassVisitor mixinClassVisitor =
            instrumentationPackage.getMixinClassVisitor(new String[] {className, classMatch});

    if (null != mixinClassVisitor) {
      if ((!verifier.isVerified(loader)) && (!verifier.verify(instrumentationPackage.getClassAppender(), loader,
                                                                     instrumentationPackage.getClassBytes(),
                                                                     instrumentationPackage.newClassLoadOrder))) {
        return null;
      }

      try {
        ClassReader reader = new ClassReader(classfileBuffer);

        if ((reader.getAccess() & 0x200) != 0) {
          if (!MatchType.Interface.equals(mixinClassVisitor.getMatchType())) {
            instrumentationPackage.getLogger()
                    .severe(className + " is an interface, but it is not marked with the " + Weave.class
                                                                                                     .getSimpleName()
                                    + " annotation of type Interface");
          }

          return null;
        }

        if (instrumentationPackage.getLogger().isFinerEnabled()) {
          instrumentationPackage.getLogger()
                  .finer("Modifying " + className + " methods " + mixinClassVisitor.getMethods().keySet());
        }

        context.addClassResolver(instrumentationPackage);
        ClassWriter writer = instrumentationPackage.getClassWriter(2, loader);
        ClassVisitor cv = writer;

        ClassStructure classStructure =
                ClassStructure.getClassStructure(new ClassReader(context.getOriginalClassBytes()), 15);

        ClassWeaver classWeaver =
                new ClassWeaver(cv, mixinClassVisitor, className, verifier, classStructure, context,
                                       instrumentationPackage, match);

        cv = classWeaver;

        if (mixinClassVisitor.interfaces.length > 0) {
          List interfaces = Lists.newArrayList(mixinClassVisitor.interfaces);
          interfaces.remove(BridgeUtils.WEAVER_TYPE.getInternalName());
          removeExistingInterfaces(instrumentationPackage, loader, reader, interfaces);

          if (!interfaces.isEmpty()) {
            instrumentationPackage.getLogger()
                    .severe(instrumentationPackage.getImplementationTitle() + " error.  " + className
                                    + " cannot add interfaces " + interfaces);

            return null;
          }
        }

        reader.accept(cv, 4);

        StatsService statsService = ServiceFactory.getStatsService();
        statsService.doStatsWork(StatsWorks.getRecordMetricWork(MessageFormat
                                                                        .format("Supportability"
                                                                                        +
                                                                                        "/WeaveInstrumentation/WeaveClass/{0}/{1}",
                                                                                       new Object[]
                                                                                               {instrumentationPackage
                                                                                                        .getImplementationTitle(),
                                                                                                       className}),
                                                                       1.0F));

        return writer.toByteArray();
      } catch (SkipTransformException e) {
        instrumentationPackage.getLogger().severe(e.getMessage());
        instrumentationPackage.getLogger().log(Level.FINE, "Skip transform", e);
      } catch (Throwable t) {
        instrumentationPackage.getLogger().severe("Unable to transform " + className + ".  " + t.getMessage());
        instrumentationPackage.getLogger().log(Level.FINE, t.getMessage(), t);
      }
    }

    return null;
  }

  private void removeExistingInterfaces(InstrumentationPackage instrumentationPackage, ClassLoader loader,
                                        ClassReader reader, List<String> interfaces) {
    if (reader == null) {
      return;
    }
    interfaces.removeAll(Arrays.asList(reader.getInterfaces()));

    for (String interfaceName : reader.getInterfaces()) {
      try {
        removeExistingInterfaces(instrumentationPackage, loader, Utils.readClass(loader, interfaceName),
                                        interfaces);
      } catch (IOException e) {
        instrumentationPackage.getLogger().log(Level.FINER,
                                                      "Unable to remove interface " + interfaceName + " from "
                                                              + reader.getClassName(), e);
      }

    }

    if (!"java/lang/Object".equals(reader.getSuperName())) {
      try {
        removeExistingInterfaces(instrumentationPackage, loader, Utils.readClass(loader, reader.getSuperName()),
                                        interfaces);
      } catch (IOException e) {
        instrumentationPackage.getLogger().log(Level.FINER,
                                                      "Unable to remove super class " + reader.getSuperName()
                                                              + " from " + reader.getClassName(), e);
      }
    }
  }

  public String toString() {
    return instrumentationPackage.getImplementationTitle();
  }
}