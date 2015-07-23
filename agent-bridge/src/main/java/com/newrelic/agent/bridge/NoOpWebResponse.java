package com.newrelic.agent.bridge;

public class NoOpWebResponse implements WebResponse {
    public static final WebResponse INSTANCE = new NoOpWebResponse();

    public int getStatus() {
        return 0;
    }

    public void setStatus(int statusCode) {
    }

    public String getStatusMessage() {
        return "";
    }

    public void setStatusMessage(String message) {
    }

    public void freezeStatus() {
    }
}