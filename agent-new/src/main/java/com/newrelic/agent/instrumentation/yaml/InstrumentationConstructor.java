//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.yaml;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.newrelic.deps.org.yaml.snakeyaml.constructor.Constructor;
import com.newrelic.deps.org.yaml.snakeyaml.nodes.Node;
import com.newrelic.deps.org.yaml.snakeyaml.nodes.ScalarNode;
import com.newrelic.deps.org.yaml.snakeyaml.nodes.SequenceNode;

import com.newrelic.agent.extension.ConfigurationConstruct;
import com.newrelic.agent.instrumentation.classmatchers.AndClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ChildClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.classmatchers.NotMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OrClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.AllMethodsMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.instrumentation.yaml.PointCutFactory.ClassMethodNameFormatDescriptor;

public class InstrumentationConstructor extends Constructor {
  public final Collection<ConfigurationConstruct> constructs =
          Arrays.asList(new ConstructClassMethodNameFormatDescriptor(), new ConstructChildClassMatcher(),
                               new ConstructNotClassMatcher(), new ConstructAndClassMatcher(),
                               new ConstructOrClassMatcher(), new ConstructExactClassMatcher(),
                               new ConstructInterfaceMatcher(), new ConstructAllMethodsMatcher(),
                               new ConstructOrMethodMatcher(), new ConstructExactMethodMatcher(),
                               new ConstructInstanceMethodMatcher(), new ConstructStaticMethodMatcher());

  public InstrumentationConstructor() {
    for (ConfigurationConstruct construct : constructs) {
      this.yamlConstructors.put(construct.getName(), construct);
    }
  }

  private class ConstructStaticMethodMatcher extends ConfigurationConstruct {
    public ConstructStaticMethodMatcher() {
      super("!static_method_matcher");
    }

    public Object construct(Node node) {
      List args = InstrumentationConstructor.this.constructSequence((SequenceNode) node);
      return PointCutFactory.getMethodMatcher(args.get(0));
    }

    public void construct2ndStep(Node node, Object o) {
    }
  }

  private class ConstructInstanceMethodMatcher extends ConfigurationConstruct {
    public ConstructInstanceMethodMatcher() {
      super("!instance_method_matcher");
    }

    public Object construct(Node node) {
      List args = InstrumentationConstructor.this.constructSequence((SequenceNode) node);
      return PointCutFactory.getMethodMatcher(args.get(0));
    }

    public void construct2ndStep(Node node, Object o) {

    }
  }

  private class ConstructExactMethodMatcher extends ConfigurationConstruct {
    public ConstructExactMethodMatcher() {
      super("!exact_method_matcher");
    }

    public Object construct(Node node) {
      List args = InstrumentationConstructor.this.constructSequence((SequenceNode) node);
      List methodDescriptors = args.subList(1, args.size());
      return PointCutFactory.createExactMethodMatcher((String) args.get(0), methodDescriptors);
    }

    public void construct2ndStep(Node node, Object o) {

    }
  }

  private class ConstructOrMethodMatcher extends ConfigurationConstruct {
    public ConstructOrMethodMatcher() {
      super("!or_method_matcher");
    }

    public Object construct(Node node) {
      List args = InstrumentationConstructor.this.constructSequence((SequenceNode) node);
      return OrMethodMatcher.getMethodMatcher(PointCutFactory.getMethodMatchers(args));
    }

    public void construct2ndStep(Node node, Object o) {

    }
  }

  private class ConstructAllMethodsMatcher extends ConfigurationConstruct {
    public ConstructAllMethodsMatcher() {
      super("!all_methods_matcher");
    }

    public Object construct(Node node) {
      return new AllMethodsMatcher();
    }

    public void construct2ndStep(Node node, Object o) {

    }
  }

  private class ConstructInterfaceMatcher extends ConfigurationConstruct {
    public ConstructInterfaceMatcher() {
      super("!interface_matcher");
    }

    public Object construct(Node node) {
      String val = (String) InstrumentationConstructor.this.constructScalar((ScalarNode) node);
      return new InterfaceMatcher(val);
    }

    public void construct2ndStep(Node node, Object o) {

    }
  }

  private class ConstructExactClassMatcher extends ConfigurationConstruct {
    public ConstructExactClassMatcher() {
      super("!exact_class_matcher");
    }

    public Object construct(Node node) {
      String val = (String) InstrumentationConstructor.this.constructScalar((ScalarNode) node);
      return new ExactClassMatcher(val);
    }

    public void construct2ndStep(Node node, Object o) {

    }
  }

  private class ConstructOrClassMatcher extends ConfigurationConstruct {
    public ConstructOrClassMatcher() {
      super("!or_class_matcher");
    }

    public Object construct(Node node) {
      List args = InstrumentationConstructor.this.constructSequence((SequenceNode) node);
      return OrClassMatcher.getClassMatcher(PointCutFactory.getClassMatchers(args));
    }

    public void construct2ndStep(Node node, Object o) {

    }
  }

  private class ConstructAndClassMatcher extends ConfigurationConstruct {
    public ConstructAndClassMatcher() {
      super("!and_class_matcher");
    }

    public Object construct(Node node) {
      List args = InstrumentationConstructor.this.constructSequence((SequenceNode) node);
      return new AndClassMatcher(PointCutFactory.getClassMatchers(args));
    }

    public void construct2ndStep(Node node, Object o) {

    }
  }

  private class ConstructNotClassMatcher extends ConfigurationConstruct {
    public ConstructNotClassMatcher() {
      super("!not_class_matcher");
    }

    public Object construct(Node node) {
      List args = InstrumentationConstructor.this.constructSequence((SequenceNode) node);
      return new NotMatcher(PointCutFactory.getClassMatcher(args.get(0)));
    }

    public void construct2ndStep(Node node, Object o) {

    }
  }

  private class ConstructChildClassMatcher extends ConfigurationConstruct {
    public ConstructChildClassMatcher() {
      super("!child_class_matcher");
    }

    public Object construct(Node node) {
      String val = (String) InstrumentationConstructor.this.constructScalar((ScalarNode) node);
      return new ChildClassMatcher(val);
    }

    public void construct2ndStep(Node node, Object o) {

    }
  }

  private class ConstructClassMethodNameFormatDescriptor extends ConfigurationConstruct {
    public ConstructClassMethodNameFormatDescriptor() {
      super("!class_method_metric_name_format");
    }

    public Object construct(Node node) {
      String prefix = (String) InstrumentationConstructor.this.constructScalar((ScalarNode) node);
      return new ClassMethodNameFormatDescriptor(prefix, false);
    }

    public void construct2ndStep(Node node, Object o) {

    }
  }
}
