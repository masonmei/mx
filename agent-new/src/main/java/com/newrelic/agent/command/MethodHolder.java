package com.newrelic.agent.command;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.Method;
import com.newrelic.agent.extension.beans.MethodParameters;

class MethodHolder {
    private final Map<String, List<String>> nameToMethods;
    private final boolean isDebug;

    public MethodHolder(boolean pDebug) {
        this.nameToMethods = new HashMap();
        this.isDebug = pDebug;
    }

    protected void addMethods(List<Method> methods) {
        if (methods != null) {
            for (Method m : methods) {
                if ((m != null) && (m.getParameters() != null)) {
                    addMethod(m.getName(), MethodParameters.getDescriptor(m.getParameters()));
                }
            }
        }
    }

    private void addMethod(String name, String descr) {
        name = name.trim();
        descr = descr.trim();

        List value = (List) this.nameToMethods.get(name);
        if (value == null) {
            value = new ArrayList();
            this.nameToMethods.put(name, value);
        }

        if (!value.contains(descr)) {
            value.add(descr);
        }
    }

    protected boolean isMethodPresent(String name, String descr, boolean remove) {
        name = name.trim();
        descr = descr.trim();

        List value = (List) this.nameToMethods.get(name);
        if (value != null) {
            if (this.isDebug) {
                XmlInstrumentValidator.printMessage(MessageFormat
                                                            .format("Found the method {0} from the xml in the list of"
                                                                            + " class methods. Checking method "
                                                                            + "parameters.", new Object[] {name}));
            }

            Iterator it = value.iterator();
            while (it.hasNext()) {
                String xmlDesc = (String) it.next();
                if (descr.startsWith(xmlDesc)) {
                    XmlInstrumentValidator
                            .printMessage(MessageFormat.format("Matched Method: {0} {1}", new Object[] {name, descr}));

                    if (remove) {
                        it.remove();
                        if (value.isEmpty()) {
                            this.nameToMethods.remove(name);
                        }
                    }
                    return true;
                }
                if (this.isDebug) {
                    XmlInstrumentValidator.printMessage(MessageFormat
                                                                .format("Descriptors for method {0} did not match. "
                                                                                + "Xml descriptor: {1}, Method "
                                                                                + "descriptor: {2} ",
                                                                               new Object[] {name, xmlDesc, descr}));
                }
            }
        }

        return false;
    }

    protected boolean hasMethods() {
        Iterator it = this.nameToMethods.entrySet().iterator();
        return it.hasNext();
    }

    protected String getCurrentMethods() {
        StringBuilder sb = new StringBuilder();
        Iterator it = this.nameToMethods.entrySet().iterator();
        while (it.hasNext()) {
            Entry values = (Entry) it.next();
            List<String> descriptors = (List) values.getValue();
            if ((descriptors != null) && (!descriptors.isEmpty())) {
                sb.append("\nMethod Name: ");
                sb.append((String) values.getKey());
                sb.append(" Param Descriptors: ");
                for (String v : descriptors) {
                    sb.append(v);
                    sb.append(" ");
                }
            }
        }

        return sb.toString();
    }
}