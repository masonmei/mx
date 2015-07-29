package com.newrelic.agent.extension;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

public class MultipleExtensionFileFilter implements FileFilter {
    private final List<String> extensions;

    public MultipleExtensionFileFilter(String[] pFileExtn) {
        this.extensions = new ArrayList();
        for (String ext : pFileExtn) {
            if ((ext != null) && (ext.length() != 0) && (!ext.startsWith("."))) {
                this.extensions.add("." + ext);
            } else {
                this.extensions.add(ext);
            }
        }
    }

    public boolean accept(File pFile) {
        String name;
        if ((pFile != null) && (pFile.isFile()) && (pFile.canRead())) {
            name = pFile.getName();
            for (String ext : this.extensions) {
                if (name.endsWith(ext)) {
                    return true;
                }
            }
        }
        return false;
    }
}