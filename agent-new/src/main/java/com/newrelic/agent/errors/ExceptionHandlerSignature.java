package com.newrelic.agent.errors;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.newrelic.deps.org.json.simple.JSONArray;
import com.newrelic.deps.org.json.simple.JSONStreamAware;
import com.newrelic.deps.org.objectweb.asm.Type;

import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.InvalidMethodDescriptor;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.tracers.ClassMethodSignature;

public class ExceptionHandlerSignature implements JSONStreamAware {
  private final String className;
  private final String methodName;
  private final String methodDescription;

  public ExceptionHandlerSignature(String className, String methodName, String methodDescription)
          throws InvalidMethodDescriptor {
    this.className = className;
    this.methodName = methodName;
    this.methodDescription = methodDescription;
    new ExactMethodMatcher(methodName, methodDescription).validate();
  }

  public ExceptionHandlerSignature(ClassMethodSignature sig) throws InvalidMethodDescriptor {
    className = sig.getClassName();
    methodName = sig.getMethodName();
    methodDescription = sig.getMethodDesc();
    new ExactMethodMatcher(methodName, methodDescription).validate();
  }

  private static Collection<String> getExceptionClassNames() {
    List<Class> classes = Arrays.asList(new Class[] {Throwable.class, Error.class, Exception.class});
    Collection<String> classNames = new ArrayList<String>();
    for (Class clazz : classes) {
      classNames.add(clazz.getName());
    }
    classNames.add("javax.servlet.ServletException");

    return classNames;
  }

  public int getExceptionArgumentIndex() {
    Type[] types = Type.getArgumentTypes(methodDescription);
    Collection exceptionClassNames = getExceptionClassNames();

    for (int i = 0; i < types.length; i++) {
      if (exceptionClassNames.contains(types[i].getClassName())) {
        return i;
      }
    }

    return -1;
  }

  public String getClassName() {
    return className;
  }

  public String getMethodName() {
    return methodName;
  }

  public String getMethodDescription() {
    return methodDescription;
  }

  public ClassMatcher getClassMatcher() {
    return new ExactClassMatcher(className);
  }

  public MethodMatcher getMethodMatcher() {
    return new ExactMethodMatcher(methodName, methodDescription);
  }

  public void writeJSONString(Writer out) throws IOException {
    JSONArray.writeJSONString(Arrays.asList(new String[] {className, methodName, methodDescription}), out);
  }

  public String toString() {
    return className.replace('/', '.') + '.' + methodName + methodDescription;
  }
}