/**
 * Copyright (c) 2008-2009 Andrey Somov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.newrelic.deps.org.yaml.snakeyaml.representer;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;

import com.newrelic.deps.org.yaml.snakeyaml.nodes.Node;
import com.newrelic.deps.org.yaml.snakeyaml.nodes.Tags;
import com.newrelic.deps.org.yaml.snakeyaml.util.Base64Coder;

/**
 * Represent standard Java classes
 * 
 * @see <a href="http://pyyaml.org/wiki/PyYAML">PyYAML</a> for more information
 */
class SafeRepresenter extends BaseRepresenter {

    public SafeRepresenter() {
        this.nullRepresenter = new RepresentNull();
        this.representers.put(String.class, new RepresentString());
        this.representers.put(Boolean.class, new RepresentBoolean());
        this.representers.put(Character.class, new RepresentString());
        this.representers.put(byte[].class, new RepresentByteArray());
        this.multiRepresenters.put(Number.class, new RepresentNumber());
        this.multiRepresenters.put(List.class, new RepresentList());
        this.multiRepresenters.put(Map.class, new RepresentMap());
        this.multiRepresenters.put(Set.class, new RepresentSet());
        this.multiRepresenters.put(new Object[0].getClass(), new RepresentArray());
        this.multiRepresenters.put(Date.class, new RepresentDate());
        this.multiRepresenters.put(Enum.class, new RepresentEnum());
    }

    @Override
    protected boolean ignoreAliases(Object data) {
        if (data == null) {
            return true;
        }
        if (data instanceof Object[]) {
            Object[] array = (Object[]) data;
            if (array.length == 0) {
                return true;
            }
        }
        return data instanceof String || data instanceof Boolean || data instanceof Integer
                || data instanceof Long || data instanceof Float || data instanceof Double
                || data instanceof Enum;
    }

    private class RepresentNull implements Represent {
        public Node representData(Object data) {
            return representScalar(Tags.NULL, "null");
        }
    }

    public static Pattern BINARY_PATTERN = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]");

    private class RepresentString implements Represent {
        public Node representData(Object data) {
            String tag = Tags.STR;
            Character style = null;
            String value = data.toString();
            if (BINARY_PATTERN.matcher(value).find()) {
                tag = Tags.BINARY;
                char[] binary;
                binary = Base64Coder.encode(value.getBytes());
                value = String.valueOf(binary);
                style = '|';
            }
            return representScalar(tag, value, style);
        }
    }

    private class RepresentBoolean implements Represent {
        public Node representData(Object data) {
            String value;
            if (Boolean.TRUE.equals(data)) {
                value = "true";
            } else {
                value = "false";
            }
            return representScalar(Tags.BOOL, value);
        }
    }

    private class RepresentNumber implements Represent {
        public Node representData(Object data) {
            String tag;
            String value;
            if (data instanceof Byte || data instanceof Short || data instanceof Integer
                    || data instanceof Long || data instanceof BigInteger) {
                tag = Tags.INT;
                value = data.toString();
            } else {
                Number number = (Number) data;
                tag = Tags.FLOAT;
                if (number.equals(Double.NaN)) {
                    value = ".NaN";
                } else if (number.equals(Double.POSITIVE_INFINITY)) {
                    value = ".inf";
                } else if (number.equals(Double.NEGATIVE_INFINITY)) {
                    value = "-.inf";
                } else {
                    value = number.toString();
                }
            }
            return representScalar(tag, value);
        }
    }

    private class RepresentList implements Represent {
        @SuppressWarnings("unchecked")
        public Node representData(Object data) {
            return representSequence(Tags.SEQ, (List<Object>) data, null);
        }
    }

    private class RepresentArray implements Represent {
        public Node representData(Object data) {
            Object[] array = (Object[]) data;
            List<Object> list = Arrays.asList(array);
            return representSequence(Tags.SEQ, list, null);
        }
    }

    private class RepresentMap implements Represent {
        @SuppressWarnings("unchecked")
        public Node representData(Object data) {
            return representMapping(Tags.MAP, (Map<Object, Object>) data, null);
        }
    }

    private class RepresentSet implements Represent {
        @SuppressWarnings("unchecked")
        public Node representData(Object data) {
            Map<Object, Object> value = new LinkedHashMap<Object, Object>();
            Set<Object> set = (Set<Object>) data;
            for (Object key : set) {
                value.put(key, null);
            }
            return representMapping(Tags.SET, value, null);
        }
    }

    private class RepresentDate implements Represent {
        public Node representData(Object data) {
            // because SimpleDateFormat ignores timezone we have to use Calendar
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTime((Date) data);
            int years = calendar.get(Calendar.YEAR);
            int months = calendar.get(Calendar.MONTH) + 1; // 0..12
            int days = calendar.get(Calendar.DAY_OF_MONTH); // 1..31
            int hour24 = calendar.get(Calendar.HOUR_OF_DAY); // 0..24
            int minutes = calendar.get(Calendar.MINUTE); // 0..59
            int seconds = calendar.get(Calendar.SECOND); // 0..59
            int millis = calendar.get(Calendar.MILLISECOND);
            StringBuffer buffer = new StringBuffer(String.valueOf(years));
            buffer.append("-");
            if (months < 10) {
                buffer.append("0");
            }
            buffer.append(String.valueOf(months));
            buffer.append("-");
            if (days < 10) {
                buffer.append("0");
            }
            buffer.append(String.valueOf(days));
            buffer.append("T");
            if (hour24 < 10) {
                buffer.append("0");
            }
            buffer.append(String.valueOf(hour24));
            buffer.append(":");
            if (minutes < 10) {
                buffer.append("0");
            }
            buffer.append(String.valueOf(minutes));
            buffer.append(":");
            if (seconds < 10) {
                buffer.append("0");
            }
            buffer.append(String.valueOf(seconds));
            if (millis > 0) {
                if (millis < 10) {
                    buffer.append(".00");
                } else if (millis < 100) {
                    buffer.append(".0");
                } else {
                    buffer.append(".");
                }
                buffer.append(String.valueOf(millis));
            }
            buffer.append("Z");
            return representScalar(Tags.TIMESTAMP, buffer.toString(), null);
        }
    }

    private class RepresentEnum implements Represent {
        public Node representData(Object data) {
            String tag = Tags.getGlobalTagForClass(data.getClass());
            return representScalar(tag, data.toString());
        }
    }

    private class RepresentByteArray implements Represent {
        public Node representData(Object data) {
            char[] binary = Base64Coder.encode((byte[]) data);
            return representScalar(Tags.BINARY, String.valueOf(binary), '|');
        }
    }
}