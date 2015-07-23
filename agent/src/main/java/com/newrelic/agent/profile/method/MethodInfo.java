package com.newrelic.agent.profile.method;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.InstrumentedMethod;

public abstract class MethodInfo {
    protected static void addOneMethodInstrumentedInfo(Map<String, Object> toAdd,
                                                       InstrumentedMethod instrumentedMethod) {
        if (instrumentedMethod != null) {
            Map inst = Maps.newHashMap();
            inst.put("dispatcher", Boolean.valueOf(instrumentedMethod.dispatcher()));
            addInstrumentationInfo(inst, instrumentedMethod);

            toAdd.put("traced_instrumentation", inst);
        }
    }

    private static void addInstrumentationInfo(Map<String, Object> inst, InstrumentedMethod instrumentedMethod) {
        InstrumentationType[] inputTypes = instrumentedMethod.instrumentationTypes();
        String[] inputNames = instrumentedMethod.instrumentationNames();

        if ((inputTypes != null) && (inputNames != null) && (inputTypes.length > 0) &&
                    (inputTypes.length == inputNames.length)) {
            Map instrumentedTypes = Maps.newHashMap();

            for (int i = 0; i < inputTypes.length; i++) {
                if (isTimedInstrumentation(inputTypes[i])) {
                    List names = (List) instrumentedTypes.get(inputTypes[i].toString());
                    if (names == null) {
                        names = Lists.newArrayList();
                        names.add(inputNames[i]);
                        instrumentedTypes.put(inputTypes[i].toString(), names);
                    } else {
                        names.add(inputNames[i]);
                    }
                }
            }

            if (instrumentedTypes.size() > 0) {
                inst.put("types", instrumentedTypes);
            }
        }
    }

    private static boolean isTimedInstrumentation(InstrumentationType type) {
        return type != InstrumentationType.WeaveInstrumentation;
    }

    protected static void addOneMethodArgs(Map<String, Object> toAdd, List<String> arguments) {
        toAdd.put("args", arguments);
    }

    public abstract List<Map<String, Object>> getJsonMethodMaps();
}