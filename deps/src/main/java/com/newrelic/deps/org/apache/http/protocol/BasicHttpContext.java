/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package com.newrelic.deps.org.apache.http.protocol;

import java.util.HashMap;
import java.util.Map;

import com.newrelic.deps.org.apache.http.annotation.NotThreadSafe;
import com.newrelic.deps.org.apache.http.util.Args;

/**
 * Default implementation of {@link HttpContext}.
 * <p>
 * Please note methods of this class are not synchronized and therefore may
 * be threading unsafe.
 *
 * @since 4.0
 */
@NotThreadSafe
public class BasicHttpContext implements HttpContext {

    private final HttpContext parentContext;
    private Map<String, Object> map = null;

    public BasicHttpContext() {
        this(null);
    }

    public BasicHttpContext(final HttpContext parentContext) {
        super();
        this.parentContext = parentContext;
    }

    public Object getAttribute(final String id) {
        Args.notNull(id, "Id");
        Object obj = null;
        if (this.map != null) {
            obj = this.map.get(id);
        }
        if (obj == null && this.parentContext != null) {
            obj = this.parentContext.getAttribute(id);
        }
        return obj;
    }

    public void setAttribute(final String id, final Object obj) {
        Args.notNull(id, "Id");
        if (this.map == null) {
            this.map = new HashMap<String, Object>();
        }
        this.map.put(id, obj);
    }

    public Object removeAttribute(final String id) {
        Args.notNull(id, "Id");
        if (this.map != null) {
            return this.map.remove(id);
        } else {
            return null;
        }
    }

    /**
     * @since 4.2
     */
    public void clear() {
        if (this.map != null) {
            this.map.clear();
        }
    }

    @Override
    public String toString() {
        if (this.map != null) {
            return this.map.toString();
        } else {
            return "{}";
        }
    }
}
