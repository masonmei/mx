package com.newrelic.agent.extension;

import java.io.FileFilter;

public enum ExtensionFileTypes {
    XML(new ExtensionFileFilter("xml")),

    YML(new MultipleExtensionFileFilter(new String[] {"yml", "yaml"})),

    JAR(new ExtensionFileFilter("jar"));

    private FileFilter filter;

    private ExtensionFileTypes(FileFilter pFilter) {
        this.filter = pFilter;
    }

    public FileFilter getFilter() {
        return this.filter;
    }
}