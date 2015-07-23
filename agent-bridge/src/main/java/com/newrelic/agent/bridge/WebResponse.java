package com.newrelic.agent.bridge;

public interface WebResponse {
    int getStatus();

    void setStatus(int paramInt);

    String getStatusMessage();

    void setStatusMessage(String paramString);

    void freezeStatus();
}