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
@XmlType(name = "", propOrder = {"instrumentation"})
@XmlRootElement(name = "extension")
public class Extension {
    protected Instrumentation instrumentation;

    @XmlAttribute(name = "name")
    protected String name;

    @XmlAttribute(name = "enabled")
    protected Boolean enabled;

    @XmlAttribute(name = "version")
    protected Double version;

    public Instrumentation getInstrumentation() {
        return instrumentation;
    }

    public void setInstrumentation(Instrumentation value) {
        instrumentation = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        name = value;
    }

    public boolean isEnabled() {
        if (enabled == null) {
            return true;
        }
        return enabled.booleanValue();
    }

    public void setEnabled(Boolean value) {
        enabled = value;
    }

    public double getVersion() {
        if (version == null) {
            return 1.0D;
        }
        return version.doubleValue();
    }

    public void setVersion(Double value) {
        version = value;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {"pointcut"})
    public static class Instrumentation {
        protected List<Pointcut> pointcut;

        @XmlAttribute(name = "metricPrefix")
        protected String metricPrefix;

        public List<Pointcut> getPointcut() {
            if (pointcut == null) {
                pointcut = new ArrayList();
            }
            return pointcut;
        }

        public String getMetricPrefix() {
            return metricPrefix;
        }

        public void setMetricPrefix(String value) {
            metricPrefix = value;
        }

        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {"nameTransaction", "methodAnnotation", "className", "interfaceName", "method"})
        public static class Pointcut {
            protected NameTransaction nameTransaction;
            protected String methodAnnotation;
            protected ClassName className;
            protected String interfaceName;
            protected List<Method> method;

            @XmlAttribute(name = "transactionStartPoint")
            protected Boolean transactionStartPoint;

            @XmlAttribute(name = "metricNameFormat")
            protected String metricNameFormat;

            @XmlAttribute(name = "excludeFromTransactionTrace")
            protected Boolean excludeFromTransactionTrace;

            @XmlAttribute(name = "ignoreTransaction")
            protected Boolean ignoreTransaction;

            @XmlAttribute(name = "transactionType")
            protected String transactionType;

            public NameTransaction getNameTransaction() {
                return nameTransaction;
            }

            public void setNameTransaction(NameTransaction value) {
                nameTransaction = value;
            }

            public String getMethodAnnotation() {
                return methodAnnotation;
            }

            public void setMethodAnnotation(String value) {
                methodAnnotation = value;
            }

            public ClassName getClassName() {
                return className;
            }

            public void setClassName(ClassName value) {
                className = value;
            }

            public String getInterfaceName() {
                return interfaceName;
            }

            public void setInterfaceName(String value) {
                interfaceName = value;
            }

            public List<Method> getMethod() {
                if (method == null) {
                    method = new ArrayList();
                }
                return method;
            }

            public boolean isTransactionStartPoint() {
                if (transactionStartPoint == null) {
                    return false;
                }
                return transactionStartPoint.booleanValue();
            }

            public void setTransactionStartPoint(Boolean value) {
                transactionStartPoint = value;
            }

            public String getMetricNameFormat() {
                return metricNameFormat;
            }

            public void setMetricNameFormat(String value) {
                metricNameFormat = value;
            }

            public boolean isExcludeFromTransactionTrace() {
                if (excludeFromTransactionTrace == null) {
                    return false;
                }
                return excludeFromTransactionTrace.booleanValue();
            }

            public void setExcludeFromTransactionTrace(Boolean value) {
                excludeFromTransactionTrace = value;
            }

            public boolean isIgnoreTransaction() {
                if (ignoreTransaction == null) {
                    return false;
                }
                return ignoreTransaction.booleanValue();
            }

            public void setIgnoreTransaction(Boolean value) {
                ignoreTransaction = value;
            }

            public String getTransactionType() {
                if (transactionType == null) {
                    return "background";
                }
                return transactionType;
            }

            public void setTransactionType(String value) {
                transactionType = value;
            }

            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "")
            public static class NameTransaction {
            }

            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {"returnType", "name", "parameters"})
            public static class Method {
                protected String returnType;
                protected String name;
                protected Parameters parameters;

                public String getReturnType() {
                    return returnType;
                }

                public void setReturnType(String value) {
                    returnType = value;
                }

                public String getName() {
                    return name;
                }

                public void setName(String value) {
                    name = value;
                }

                public Parameters getParameters() {
                    return parameters;
                }

                public void setParameters(Parameters value) {
                    parameters = value;
                }

                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {"type"})
                public static class Parameters {
                    protected List<Type> type;

                    public List<Type> getType() {
                        if (type == null) {
                            type = new ArrayList();
                        }
                        return type;
                    }

                    @XmlAccessorType(XmlAccessType.FIELD)
                    @XmlType(name = "", propOrder = {"value"})
                    public static class Type {

                        @XmlValue
                        protected String value;

                        @XmlAttribute(name = "attributeName")
                        protected String attributeName;

                        public String getValue() {
                            return value;
                        }

                        public void setValue(String value) {
                            this.value = value;
                        }

                        public String getAttributeName() {
                            return attributeName;
                        }

                        public void setAttributeName(String value) {
                            attributeName = value;
                        }
                    }
                }
            }

            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {"value"})
            public static class ClassName {

                @XmlValue
                protected String value;

                @XmlAttribute(name = "includeSubclasses")
                protected Boolean includeSubclasses;

                public String getValue() {
                    return value;
                }

                public void setValue(String value) {
                    this.value = value;
                }

                public boolean isIncludeSubclasses() {
                    if (includeSubclasses == null) {
                        return false;
                    }
                    return includeSubclasses.booleanValue();
                }

                public void setIncludeSubclasses(Boolean value) {
                    includeSubclasses = value;
                }
            }
        }
    }
}