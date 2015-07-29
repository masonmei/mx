package com.newrelic.agent.extension.beans;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="", propOrder={"instrumentation"})
@XmlRootElement(name="extension")
public class Extension
{
  protected Instrumentation instrumentation;

  @XmlAttribute(name="name")
  protected String name;

  @XmlAttribute(name="enabled")
  protected Boolean enabled;

  @XmlAttribute(name="version")
  protected Double version;

  public Instrumentation getInstrumentation()
  {
    return this.instrumentation;
  }

  public void setInstrumentation(Instrumentation value)
  {
    this.instrumentation = value;
  }

  public String getName()
  {
    return this.name;
  }

  public void setName(String value)
  {
    this.name = value;
  }

  public boolean isEnabled()
  {
    if (this.enabled == null) {
      return true;
    }
    return this.enabled.booleanValue();
  }

  public void setEnabled(Boolean value)
  {
    this.enabled = value;
  }

  public double getVersion()
  {
    if (this.version == null) {
      return 1.0D;
    }
    return this.version.doubleValue();
  }

  public void setVersion(Double value)
  {
    this.version = value;
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name="", propOrder={"pointcut"})
  public static class Instrumentation
  {
    protected List<Pointcut> pointcut;

    @XmlAttribute(name="metricPrefix")
    protected String metricPrefix;

    public List<Pointcut> getPointcut()
    {
      if (this.pointcut == null) {
        this.pointcut = new ArrayList();
      }
      return this.pointcut;
    }

    public String getMetricPrefix()
    {
      return this.metricPrefix;
    }

    public void setMetricPrefix(String value)
    {
      this.metricPrefix = value;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name="", propOrder={"nameTransaction", "methodAnnotation", "className", "interfaceName", "method"})
    public static class Pointcut
    {
      protected NameTransaction nameTransaction;
      protected String methodAnnotation;
      protected ClassName className;
      protected String interfaceName;
      protected List<Method> method;

      @XmlAttribute(name="transactionStartPoint")
      protected Boolean transactionStartPoint;

      @XmlAttribute(name="metricNameFormat")
      protected String metricNameFormat;

      @XmlAttribute(name="excludeFromTransactionTrace")
      protected Boolean excludeFromTransactionTrace;

      @XmlAttribute(name="ignoreTransaction")
      protected Boolean ignoreTransaction;

      @XmlAttribute(name="transactionType")
      protected String transactionType;

      public NameTransaction getNameTransaction()
      {
        return this.nameTransaction;
      }

      public void setNameTransaction(NameTransaction value)
      {
        this.nameTransaction = value;
      }

      public String getMethodAnnotation()
      {
        return this.methodAnnotation;
      }

      public void setMethodAnnotation(String value)
      {
        this.methodAnnotation = value;
      }

      public ClassName getClassName()
      {
        return this.className;
      }

      public void setClassName(ClassName value)
      {
        this.className = value;
      }

      public String getInterfaceName()
      {
        return this.interfaceName;
      }

      public void setInterfaceName(String value)
      {
        this.interfaceName = value;
      }

      public List<Method> getMethod()
      {
        if (this.method == null) {
          this.method = new ArrayList();
        }
        return this.method;
      }

      public boolean isTransactionStartPoint()
      {
        if (this.transactionStartPoint == null) {
          return false;
        }
        return this.transactionStartPoint.booleanValue();
      }

      public void setTransactionStartPoint(Boolean value)
      {
        this.transactionStartPoint = value;
      }

      public String getMetricNameFormat()
      {
        return this.metricNameFormat;
      }

      public void setMetricNameFormat(String value)
      {
        this.metricNameFormat = value;
      }

      public boolean isExcludeFromTransactionTrace()
      {
        if (this.excludeFromTransactionTrace == null) {
          return false;
        }
        return this.excludeFromTransactionTrace.booleanValue();
      }

      public void setExcludeFromTransactionTrace(Boolean value)
      {
        this.excludeFromTransactionTrace = value;
      }

      public boolean isIgnoreTransaction()
      {
        if (this.ignoreTransaction == null) {
          return false;
        }
        return this.ignoreTransaction.booleanValue();
      }

      public void setIgnoreTransaction(Boolean value)
      {
        this.ignoreTransaction = value;
      }

      public String getTransactionType()
      {
        if (this.transactionType == null) {
          return "background";
        }
        return this.transactionType;
      }

      public void setTransactionType(String value)
      {
        this.transactionType = value;
      }

      @XmlAccessorType(XmlAccessType.FIELD)
      @XmlType(name="")
      public static class NameTransaction
      {
      }

      @XmlAccessorType(XmlAccessType.FIELD)
      @XmlType(name="", propOrder={"returnType", "name", "parameters"})
      public static class Method
      {
        protected String returnType;
        protected String name;
        protected Parameters parameters;

        public String getReturnType()
        {
          return this.returnType;
        }

        public void setReturnType(String value)
        {
          this.returnType = value;
        }

        public String getName()
        {
          return this.name;
        }

        public void setName(String value)
        {
          this.name = value;
        }

        public Parameters getParameters()
        {
          return this.parameters;
        }

        public void setParameters(Parameters value)
        {
          this.parameters = value;
        }

        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name="", propOrder={"type"})
        public static class Parameters
        {
          protected List<Type> type;

          public List<Type> getType()
          {
            if (this.type == null) {
              this.type = new ArrayList();
            }
            return this.type;
          }

          @XmlAccessorType(XmlAccessType.FIELD)
          @XmlType(name="", propOrder={"value"})
          public static class Type
          {

            @XmlValue
            protected String value;

            @XmlAttribute(name="attributeName")
            protected String attributeName;

            public String getValue()
            {
              return this.value;
            }

            public void setValue(String value)
            {
              this.value = value;
            }

            public String getAttributeName()
            {
              return this.attributeName;
            }

            public void setAttributeName(String value)
            {
              this.attributeName = value;
            }
          }
        }
      }

      @XmlAccessorType(XmlAccessType.FIELD)
      @XmlType(name="", propOrder={"value"})
      public static class ClassName
      {

        @XmlValue
        protected String value;

        @XmlAttribute(name="includeSubclasses")
        protected Boolean includeSubclasses;

        public String getValue()
        {
          return this.value;
        }

        public void setValue(String value)
        {
          this.value = value;
        }

        public boolean isIncludeSubclasses()
        {
          if (this.includeSubclasses == null) {
            return false;
          }
          return this.includeSubclasses.booleanValue();
        }

        public void setIncludeSubclasses(Boolean value)
        {
          this.includeSubclasses = value;
        }
      }
    }
  }
}