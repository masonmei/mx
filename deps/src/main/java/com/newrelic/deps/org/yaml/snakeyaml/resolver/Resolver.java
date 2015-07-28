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
package com.newrelic.deps.org.yaml.snakeyaml.resolver;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.newrelic.deps.org.yaml.snakeyaml.nodes.NodeId;
import com.newrelic.deps.org.yaml.snakeyaml.nodes.Tags;

/**
 * Resolver tries to detect a type by scalars's content (when the type is
 * implicit)
 * 
 * @see <a href="http://pyyaml.org/wiki/PyYAML">PyYAML</a> for more information
 */
public class Resolver {
    private static final Pattern BOOL = Pattern
            .compile("^(?:yes|Yes|YES|no|No|NO|true|True|TRUE|false|False|FALSE|on|On|ON|off|Off|OFF)$");
    private static final Pattern FLOAT = Pattern
            .compile("^(?:[-+]?(?:[0-9][0-9_]*)\\.[0-9_]*(?:[eE][-+][0-9]+)?|[-+]?(?:[0-9][0-9_]*)?\\.[0-9_]+(?:[eE][-+][0-9]+)?|[-+]?[0-9][0-9_]*(?::[0-5]?[0-9])+\\.[0-9_]*|[-+]?\\.(?:inf|Inf|INF)|\\.(?:nan|NaN|NAN))$");
    private static final Pattern INT = Pattern
            .compile("^(?:[-+]?0b[0-1_]+|[-+]?0[0-7_]+|[-+]?(?:0|[1-9][0-9_]*)|[-+]?0x[0-9a-fA-F_]+|[-+]?[1-9][0-9_]*(?::[0-5]?[0-9])+)$");
    private static final Pattern MERGE = Pattern.compile("^(?:<<)$");
    private static final Pattern NULL = Pattern.compile("^(?:~|null|Null|NULL| )$");
    private static final Pattern EMPTY = Pattern.compile("^$");
    private static final Pattern TIMESTAMP = Pattern
            .compile("^(?:[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]|[0-9][0-9][0-9][0-9]-[0-9][0-9]?-[0-9][0-9]?(?:[Tt]|[ \t]+)[0-9][0-9]?:[0-9][0-9]:[0-9][0-9](?:\\.[0-9]*)?(?:[ \t]*(?:Z|[-+][0-9][0-9]?(?::[0-9][0-9])?))?)$");
    private static final Pattern VALUE = Pattern.compile("^(?:=)$");
    private static final Pattern YAML = Pattern.compile("^(?:!|&|\\*)$");

    private Map<Character, List<ResolverTuple>> yamlImplicitResolvers = new HashMap<Character, List<ResolverTuple>>();

    /**
     * Create Resolver
     * 
     * @param respectDefaultImplicitScalars
     *            false to parse/dump scalars as plain Strings
     */
    public Resolver(boolean respectDefaultImplicitScalars) {
        if (respectDefaultImplicitScalars) {
            addImplicitResolver(Tags.BOOL, BOOL, "yYnNtTfFoO");
            addImplicitResolver(Tags.FLOAT, FLOAT, "-+0123456789.");
            addImplicitResolver(Tags.INT, INT, "-+0123456789");
            addImplicitResolver(Tags.MERGE, MERGE, "<");
            addImplicitResolver(Tags.NULL, NULL, "~nN\0");
            addImplicitResolver(Tags.NULL, EMPTY, null);
            addImplicitResolver(Tags.TIMESTAMP, TIMESTAMP, "0123456789");
            addImplicitResolver(Tags.VALUE, VALUE, "=");
            // The following implicit resolver is only for documentation
            // purposes.
            // It cannot work
            // because plain scalars cannot start with '!', '&', or '*'.
            addImplicitResolver(Tags.YAML, YAML, "!&*");
        }
    }

    public Resolver() {
        this(true);
    }

    public void addImplicitResolver(String tag, Pattern regexp, String first) {
        if (first == null) {
            List<ResolverTuple> curr = yamlImplicitResolvers.get(null);
            if (curr == null) {
                curr = new LinkedList<ResolverTuple>();
                yamlImplicitResolvers.put(null, curr);
            }
            curr.add(new ResolverTuple(tag, regexp));
        } else {
            char[] chrs = first.toCharArray();
            for (int i = 0, j = chrs.length; i < j; i++) {
                Character theC = new Character(chrs[i]);
                if (theC == 0) {
                    // special case: for null
                    theC = null;
                }
                List<ResolverTuple> curr = yamlImplicitResolvers.get(theC);
                if (curr == null) {
                    curr = new LinkedList<ResolverTuple>();
                    yamlImplicitResolvers.put(theC, curr);
                }
                curr.add(new ResolverTuple(tag, regexp));
            }
        }
    }

    public String resolve(NodeId kind, String value, boolean implicit) {
        if (kind == NodeId.scalar && implicit) {
            List<ResolverTuple> resolvers = null;
            if ("".equals(value)) {
                resolvers = yamlImplicitResolvers.get('\0');
            } else {
                resolvers = yamlImplicitResolvers.get(value.charAt(0));
            }
            if (resolvers != null) {
                for (ResolverTuple v : resolvers) {
                    String tag = v.getTag();
                    Pattern regexp = v.getRegexp();
                    if (regexp.matcher(value).matches()) {
                        return tag;
                    }
                }
            }
            if (yamlImplicitResolvers.containsKey(null)) {
                for (ResolverTuple v : yamlImplicitResolvers.get(null)) {
                    String tag = v.getTag();
                    Pattern regexp = v.getRegexp();
                    if (regexp.matcher(value).matches()) {
                        return tag;
                    }
                }
            }
        }
        switch (kind) {
        case scalar:
            return Tags.STR;
        case sequence:
            return Tags.SEQ;
        default:
            return Tags.MAP;
        }
    }
}