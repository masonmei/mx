//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.jmx.metrics;

import java.text.MessageFormat;
import java.util.Map;

public enum JmxAction {
    USE_FIRST_ATT {
        public float performAction(String[] pAttributes, Map<String, Float> pValues) throws IllegalArgumentException {
            return pAttributes != null && pAttributes.length != 0 ? JmxAction.getValue(pValues, pAttributes[0]) : 0.0F;
        }
    },
    USE_FIRST_RECORDED_ATT {
        public float performAction(String[] pAttributes, Map<String, Float> pValues) throws IllegalArgumentException {
            if (pAttributes != null && pAttributes.length != 0) {
                Float value = null;
                String[] arr$ = pAttributes;
                int len$ = pAttributes.length;

                for (int i$ = 0; i$ < len$; ++i$) {
                    String current = arr$[i$];
                    value = JmxAction.getValueNullOkay(pValues, current);
                    if (value != null) {
                        return value.floatValue();
                    }
                }

                return 0.0F;
            } else {
                return 0.0F;
            }
        }
    },
    SUBTRACT_ALL_FROM_FIRST {
        public float performAction(String[] pAttributes, Map<String, Float> values) throws IllegalArgumentException {
            float output;
            if (pAttributes == null) {
                output = 0.0F;
            } else {
                int length = pAttributes.length;
                if (length == 0) {
                    output = 0.0F;
                } else {
                    output = JmxAction.getValue(values, pAttributes[0]);
                    if (length > 1) {
                        for (int i = 1; i < length; ++i) {
                            output -= JmxAction.getValue(values, pAttributes[i]);
                        }
                    }

                    if (output < 0.0F) {
                        throw new IllegalArgumentException(MessageFormat
                                                                   .format("The output value can not be negative: {0} ",
                                                                                  new Object[] {Float.valueOf(output)
                                                                                  }));
                    }
                }
            }

            return output;
        }
    },
    SUM_ALL {
        public float performAction(String[] pAttributes, Map<String, Float> values) throws IllegalArgumentException {
            float output;
            if (pAttributes == null) {
                output = 0.0F;
            } else {
                int length = pAttributes.length;
                if (length == 0) {
                    output = 0.0F;
                } else {
                    output = JmxAction.getValue(values, pAttributes[0]);
                    if (length > 1) {
                        for (int i = 1; i < length; ++i) {
                            output += JmxAction.getValue(values, pAttributes[i]);
                        }
                    }

                    if (output < 0.0F) {
                        throw new IllegalArgumentException(MessageFormat
                                                                   .format("The output value can not be negative: {0} ",
                                                                                  new Object[] {Float.valueOf(output)
                                                                                  }));
                    }
                }
            }

            return output;
        }
    };

    private JmxAction() {
    }

    private static float getValue(Map<String, Float> values, String att) {
        Float value = (Float) values.get(att);
        if (value == null) {
            throw new IllegalArgumentException(MessageFormat.format("There is no value for attribute {0}",
                                                                           new Object[] {att}));
        } else {
            return value.floatValue();
        }
    }

    private static Float getValueNullOkay(Map<String, Float> values, String att) {
        Float value = (Float) values.get(att);
        return value == null ? null : value;
    }

    public abstract float performAction(String[] var1, Map<String, Float> var2) throws IllegalArgumentException;
}
