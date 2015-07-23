package com.newrelic.api.agent;

/**
 * Represents a response to a web request.
 * 
 * The implementation of this interface does not need to be servlet specific, but the API is based on the servlet spec.
 * 
 * @author sdaubin
 * 
 * @see NewRelic#setRequestAndResponse(Request, Response)
 * @see javax.servlet.http.HttpServletResponse
 */
public interface Response extends OutboundHeaders {
    /**
     * Returns the status code for this response.
     * 
     * @return The status code.
     * @throws Exception Problem getting the status.
     * @since 2.3.0
     */
    int getStatus() throws Exception;

    /**
     * Returns the error status message, or <code>null</code> if there is none.
     * 
     * @return The error status message.
     * @throws Exception Problem getting status message.
     * @since 2.3.0
     */
    String getStatusMessage() throws Exception;

    /**
     * Returns the response content type, or <code>null</code> if it is not available.
     * 
     * @return The response content type or <code>null</code> if it is not available.
     * @since 2.3.0
     */
    String getContentType();
}
