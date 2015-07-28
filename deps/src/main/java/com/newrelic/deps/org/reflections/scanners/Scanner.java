package com.newrelic.deps.org.reflections.scanners;

import com.newrelic.deps.com.google.common.base.Predicate;
import com.newrelic.deps.com.google.common.collect.Multimap;
import com.newrelic.deps.org.reflections.Configuration;
import com.newrelic.deps.org.reflections.vfs.Vfs;

import javax.annotation.Nullable;

/**
 *
 */
public interface Scanner {

    void setConfiguration(Configuration configuration);

    Multimap<String, String> getStore();

    void setStore(Multimap<String, String> store);

    Scanner filterResultsBy(Predicate<String> filter);

    boolean acceptsInput(String file);

    Object scan(Vfs.File file, @Nullable Object classObject);

    boolean acceptResult(String fqn);
}
