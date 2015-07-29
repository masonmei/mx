package com.newrelic.agent.command;

public enum XmlInstrumentOptions {
    FILE_PATH("file", true, "The full path to the xml extension file to be validated. This must be set.") {
        public void validateAndAddParameter(XmlInstrumentParams pInstrument, String[] pValue, String pTagName) {
            pInstrument.setFile(pValue, pTagName);
        }
    },
    DEBUG_FLAG("debug", true, "Set this flag to true for more debuging information. The default is false.") {
        public void validateAndAddParameter(XmlInstrumentParams pInstrument, String[] pValue, String pTagName) {
            pInstrument.setDebug(pValue, pTagName);
        }
    };

    private final String flagName;
    private final String description;
    private boolean argRequired;

    private XmlInstrumentOptions(String pFlagName, boolean pRequired, String pDescription) {
        this.flagName = pFlagName;
        this.argRequired = pRequired;
        this.description = pDescription;
    }

    public abstract void validateAndAddParameter(XmlInstrumentParams var1, String[] var2, String var3);

    public String getFlagName() {
        return this.flagName;
    }

    public boolean isArgRequired() {
        return this.argRequired;
    }

    public String getDescription() {
        return this.description;
    }
}