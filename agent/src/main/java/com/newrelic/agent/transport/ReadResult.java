package com.newrelic.agent.transport;

public class ReadResult {
    private final int responseCode;
    private final String responseBody;

    protected ReadResult(int responseCode, String responseBody) {
        this.responseCode = responseCode;
        this.responseBody = responseBody;
    }

    public static ReadResult create(int responseCode, String responseBody) {
        return new ReadResult(responseCode, responseBody);
    }

    protected int getResponseCode() {
        return responseCode;
    }

    protected String getResponseBody() {
        return responseBody;
    }
}