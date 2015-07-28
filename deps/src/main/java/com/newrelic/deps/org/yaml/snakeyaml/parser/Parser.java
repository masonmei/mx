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
package com.newrelic.deps.org.yaml.snakeyaml.parser;

import java.util.List;

import com.newrelic.deps.org.yaml.snakeyaml.events.Event;

/**
 * @see <a href="http://pyyaml.org/wiki/PyYAML">PyYAML</a> for more information
 */
public interface Parser {
    public boolean checkEvent(List<Class<? extends Event>> choices);

    public boolean checkEvent(Class<? extends Event> choice);

    public Event peekEvent();

    public Event getEvent();

}
