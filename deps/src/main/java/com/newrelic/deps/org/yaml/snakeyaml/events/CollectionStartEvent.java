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
package com.newrelic.deps.org.yaml.snakeyaml.events;

import com.newrelic.deps.org.yaml.snakeyaml.error.Mark;

/**
 * @see <a href="http://pyyaml.org/wiki/PyYAML">PyYAML</a> for more information
 */
public abstract class CollectionStartEvent extends NodeEvent {
    private final String tag;
    // The implicit flag of a collection start event indicates if the tag may be
    // omitted when the collection is emitted
    private final boolean implicit;
    // flag indicates if a collection is block or flow
    private final Boolean flowStyle;

    public CollectionStartEvent(String anchor, String tag, boolean implicit, Mark startMark,
            Mark endMark, Boolean flowStyle) {
        super(anchor, startMark, endMark);
        this.tag = tag;
        this.implicit = implicit;
        this.flowStyle = flowStyle;
    }

    public String getTag() {
        return this.tag;
    }

    public boolean getImplicit() {
        return this.implicit;
    }

    public Boolean getFlowStyle() {
        return this.flowStyle;
    }

    @Override
    protected String getArguments() {
        return super.getArguments() + ", tag=" + tag + ", implicit=" + implicit;
    }
}
