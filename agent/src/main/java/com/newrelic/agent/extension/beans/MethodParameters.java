//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.extension.beans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.Method.Parameters;

public class MethodParameters extends Parameters {
    private final String paramDescriptor;
    private final boolean wasError;
    private final String errorMessage;

    public MethodParameters(List<String> pParams) {
        String eMessage;
        if (pParams != null) {
            ArrayList desc = Lists.newArrayListWithCapacity(pParams.size());
            Iterator pError = pParams.iterator();

            while (pError.hasNext()) {
                eMessage = (String) pError.next();
                Type e = new Type();
                e.setValue(eMessage);
                desc.add(e);
            }

            this.type = desc;
        }

        String desc1;
        boolean pError1;
        try {
            desc1 = MethodConverterUtility.paramNamesToParamDescriptor(pParams);
            pError1 = false;
            eMessage = "";
        } catch (Exception var6) {
            pError1 = true;
            eMessage = var6.getMessage();
            desc1 = null;
        }

        this.paramDescriptor = desc1;
        this.wasError = pError1;
        this.errorMessage = eMessage;
    }

    public static String getDescriptor(Parameters parameters) {
        return (new MethodParameters(parameters == null ? Collections.EMPTY_LIST
                                             : convertToStringList(parameters.getType()))).getDescriptor();
    }

    private static List<String> convertToStringList(List<Type> types) {
        ArrayList params = Lists.newArrayListWithCapacity(types.size());
        Iterator i$ = types.iterator();

        while (i$.hasNext()) {
            Type t = (Type) i$.next();
            params.add(t.getValue());
        }

        return params;
    }

    public String getDescriptor() {
        return this.paramDescriptor;
    }

    public boolean isWasError() {
        return this.wasError;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }
}
