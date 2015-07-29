//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.extension.beans;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.newrelic.deps.org.objectweb.asm.Type;

class MethodConverterUtility {
    private static final String BEGIN_PARENTH_FOR_DESCRIPTOR = "(";
    private static final String END_PARENTH_FOR_DESCRIPTOR = ")";
    private static final String ARRAY_NOTATIOM = "[";
    private static final Pattern ARRAY_PATTERN = Pattern.compile("(.+?)((\\[\\])+)\\z");
    private static final Pattern BRACKETS = Pattern.compile("(\\[\\])");
    private static final String COLLECTION_TYPE_REGEX = "<.+?>";

    private MethodConverterUtility() {
    }

    private static String convertParamToDescriptorFormat(String inputParam) {
        if (inputParam == null) {
            throw new RuntimeException("The input parameter can not be null.");
        } else {
            Type paramType;
            if (Type.BOOLEAN_TYPE.getClassName().equals(inputParam)) {
                paramType = Type.BOOLEAN_TYPE;
            } else if (Type.BYTE_TYPE.getClassName().equals(inputParam)) {
                paramType = Type.BYTE_TYPE;
            } else if (Type.CHAR_TYPE.getClassName().equals(inputParam)) {
                paramType = Type.CHAR_TYPE;
            } else if (Type.DOUBLE_TYPE.getClassName().equals(inputParam)) {
                paramType = Type.DOUBLE_TYPE;
            } else if (Type.FLOAT_TYPE.getClassName().equals(inputParam)) {
                paramType = Type.FLOAT_TYPE;
            } else if (Type.INT_TYPE.getClassName().equals(inputParam)) {
                paramType = Type.INT_TYPE;
            } else if (Type.LONG_TYPE.getClassName().equals(inputParam)) {
                paramType = Type.LONG_TYPE;
            } else if (Type.SHORT_TYPE.getClassName().equals(inputParam)) {
                paramType = Type.SHORT_TYPE;
            } else {
                Matcher arrayMatcher = ARRAY_PATTERN.matcher(inputParam);
                String output;
                if (arrayMatcher.matches()) {
                    output = arrayMatcher.group(1);
                    String brackets = arrayMatcher.group(2);
                    return makeArrayType(output, brackets);
                }

                if (inputParam.contains("[")) {
                    throw new RuntimeException("Brackets should only be in the parameter name if it is an array. Name: "
                                                       + inputParam);
                }

                output = inputParam.replace(".", "/").replaceAll("<.+?>", "");
                paramType = Type.getObjectType(output);
            }

            return paramType.getDescriptor();
        }
    }

    private static String makeArrayType(String paramType, String brackets) {
        Matcher mms = BRACKETS.matcher(brackets);

        int count;
        for (count = 0; mms.find(); ++count) {
            ;
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < count; ++i) {
            sb.append("[");
        }

        sb.append(convertParamToDescriptorFormat(paramType));
        return sb.toString();
    }

    protected static String paramNamesToParamDescriptor(List<String> inputParameters) {
        if (inputParameters == null) {
            return "()";
        } else {
            ArrayList descriptors = new ArrayList();
            Iterator i$ = inputParameters.iterator();

            while (i$.hasNext()) {
                String param = (String) i$.next();
                descriptors.add(convertParamToDescriptorFormat(param.trim()));
            }

            return convertToParmDescriptor(descriptors);
        }
    }

    private static String convertToParmDescriptor(List<String> paramDescriptors) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        if (paramDescriptors != null && !paramDescriptors.isEmpty()) {
            Iterator i$ = paramDescriptors.iterator();

            while (i$.hasNext()) {
                String param = (String) i$.next();
                if (Type.getType(param) == null) {
                    throw new RuntimeException("The generated parameter descriptor is invalid. Name: " + param);
                }

                sb.append(param);
            }
        }

        sb.append(")");
        return sb.toString();
    }
}
