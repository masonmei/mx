package com.newrelic.agent.command;

import java.io.File;
import java.text.MessageFormat;

public class XmlInstrumentParams {
    private File filePath;
    private boolean debug = false;

    public void setFile(String[] pFile, String tagName) {
        String fileName = verifyOne(pFile, tagName);
        filePath = new File(fileName);
        if (!filePath.exists()) {
            throw new IllegalArgumentException(MessageFormat
                                                       .format("The file specified with the tag {0} does not exist.",
                                                                      tagName));
        }
        if (!filePath.isFile()) {
            throw new IllegalArgumentException(MessageFormat
                                                       .format("The file specified with the tag {0} must be a file "
                                                                       + "and is not.", tagName));
        }
        if (!filePath.canRead()) {
            throw new IllegalArgumentException(MessageFormat
                                                       .format("The file specified with the tag {0} must be readable "
                                                                       + "and is not.", tagName));
        }
    }

    public File getFile() {
        return filePath;
    }

    public void setDebug(String[] pDebug, String tagName) {
        String value = verifyOneOrNone(pDebug, tagName);
        if (value != null) {
            debug = Boolean.parseBoolean(value);
        }
    }

    public boolean isDebug() {
        return debug;
    }

    private String verifyOne(String[] value, String tagName) {
        String toReturn = null;
        if ((value != null) && (value.length == 1)) {
            toReturn = value[0];
            if (toReturn != null) {
                toReturn = toReturn.trim();
            } else {
                throw new IllegalArgumentException(MessageFormat
                                                           .format("One {0}, and only one {0}, must be set.", tagName));
            }
        } else {
            throw new IllegalArgumentException(MessageFormat
                                                       .format("One {0}, and only one {0}, must be set.", tagName));
        }
        return toReturn;
    }

    private String verifyOneOrNone(String[] value, String tagName) {
        String toReturn;
        if (value == null) {
            return null;
        }
        if (value.length == 1) {
            toReturn = value[0];
            if (toReturn == null) {
                return null;
            }
            toReturn = toReturn.trim();
        } else {
            throw new IllegalArgumentException(MessageFormat
                                                       .format("One {0}, and only one {0}, must be set.", tagName));
        }
        return toReturn;
    }
}