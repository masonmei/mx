package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.agent.Agent;
import com.newrelic.deps.com.google.common.collect.Sets;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.agent.logging.IAgentLogger;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class NewClassDependencyVisitor extends ClassVisitor
{
  protected String className;
  protected final List<String> newClassLoadOrder;
  protected Set<String> referencedSupertypes;

  public NewClassDependencyVisitor(int api, ClassVisitor cv, List<String> newClassLoadOrder)
  {
    super(api, cv);

    this.newClassLoadOrder = newClassLoadOrder;
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    this.className = name;
    this.referencedSupertypes = Sets.newHashSet(Arrays.asList(interfaces));
    this.referencedSupertypes.add(superName);
  }

  public void visitEnd() {
    super.visitEnd();

    int classIndex = this.newClassLoadOrder.indexOf(this.className);
    if (classIndex < 0)
      Agent.LOG.log(Level.FINEST, "Error: Visisted class is not in the dependency list: {0}", new Object[] { this.className });
    else
      sortDependencyList(classIndex, this.newClassLoadOrder, this.referencedSupertypes);
  }

  private void sortDependencyList(int classIndex, List<String> dependencies, Set<String> classDependencies)
  {
    for (String dependency : classDependencies) {
      int dependencyIndex = dependencies.indexOf(dependency);

      if (dependencyIndex > classIndex) {
        Agent.LOG.log(Level.FINEST, "{0} : Moving dependency {1} to position {2}", new Object[] { this.className, dependency, Integer.valueOf(classIndex) });

        dependencies.add(classIndex, dependencies.remove(dependencyIndex));
        classIndex++;
      }
    }
  }
}