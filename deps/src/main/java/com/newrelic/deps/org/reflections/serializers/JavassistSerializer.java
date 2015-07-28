package com.newrelic.deps.org.reflections.serializers;

import com.newrelic.deps.org.reflections.Configuration;
import com.newrelic.deps.org.reflections.Reflections;
import com.newrelic.deps.org.reflections.adapters.MetadataAdapter;

import java.io.File;
import java.io.InputStream;

/**
 *
 */
public class JavassistSerializer implements Serializer {

    private final MetadataAdapter javassist;

    public JavassistSerializer(Configuration configuration) {
        javassist = configuration.getMetadataAdapter();
    }

    @Override
    public Reflections read(InputStream inputStream) {
//        new BytecGodeGene
        return null;
    }

    @Override
    public File save(Reflections reflections, String filename) {
        return null;
    }

    @Override
    public String toString(Reflections reflections) {
        return null;
    }
}
