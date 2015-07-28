package com.newrelic.deps.org.reflections.scanners;

import com.newrelic.deps.com.google.common.base.Predicate;
import com.newrelic.deps.com.google.common.base.Predicates;
import com.newrelic.deps.com.google.common.collect.Multimap;
import com.newrelic.deps.org.reflections.Configuration;
import com.newrelic.deps.org.reflections.ReflectionsException;
import com.newrelic.deps.org.reflections.adapters.MetadataAdapter;
import com.newrelic.deps.org.reflections.vfs.Vfs;

import static com.newrelic.deps.org.reflections.Reflections.log;

/**
 *
 */
@SuppressWarnings({"RawUseOfParameterizedType", "unchecked"})
public abstract class AbstractScanner implements Scanner {

	private Configuration configuration;
	private Multimap<String, String> store;
	private Predicate<String> resultFilter = Predicates.alwaysTrue(); //accept all by default

    public boolean acceptsInput(String file) {
        return getMetadataAdapter().acceptsInput(file);
    }

    public Object scan(Vfs.File file, Object classObject) {
        if (classObject == null) {
            try {
                classObject = configuration.getMetadataAdapter().getOfCreateClassObject(file);
            } catch (Exception e) {
                throw new ReflectionsException("could not create class object from file " + file.getRelativePath());
            }
        }
        scan(classObject);
        return classObject;
    }

    public abstract void scan(Object cls);

    //
    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(final Configuration configuration) {
        this.configuration = configuration;
    }

    public Multimap<String, String> getStore() {
        return store;
    }

    public void setStore(final Multimap<String, String> store) {
        this.store = store;
    }

    public Predicate<String> getResultFilter() {
        return resultFilter;
    }

    public void setResultFilter(Predicate<String> resultFilter) {
        this.resultFilter = resultFilter;
    }

    public Scanner filterResultsBy(Predicate<String> filter) {
        this.setResultFilter(filter); return this;
    }

    //
    public boolean acceptResult(final String fqn) {
		return fqn != null && resultFilter.apply(fqn);
	}

	protected MetadataAdapter getMetadataAdapter() {
		return configuration.getMetadataAdapter();
	}

    //
    @Override public boolean equals(Object o) {
        return this == o || o != null && getClass() == o.getClass();
    }

    @Override public int hashCode() {
        return getClass().hashCode();
    }
}
