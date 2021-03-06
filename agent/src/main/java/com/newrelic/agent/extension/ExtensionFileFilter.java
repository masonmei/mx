package com.newrelic.agent.extension;

import java.io.File;
import java.io.FileFilter;

public class ExtensionFileFilter implements FileFilter {
    private String fileExtension;

    public ExtensionFileFilter(String pFileExt) {
        if ((pFileExt != null) && (pFileExt.length() != 0) && (!pFileExt.startsWith("."))) {
            fileExtension = ("." + pFileExt);
        } else {
            fileExtension = pFileExt;
        }
    }

    public boolean accept(File pFile) {
        return (pFile != null) && (pFile.isFile()) && (pFile.canRead()) && (pFile.getName().endsWith(fileExtension));
    }
}