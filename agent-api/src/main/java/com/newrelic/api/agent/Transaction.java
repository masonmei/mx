package com.newrelic.api.agent;

/**
 * A transaction represents a unit of work in an application. It may be a single web request or a scheduled background
 * task. To indicate that a method is the starting point of a transaction, mark it with a {@link Trace} annotation and
 * set {@link Trace#dispatcher()} to true.
 * 
 * <h2>Cross Application Tracing API</h2>
 *
 * Use the cross application tracing API when the agent does not provide cross application tracing support for your
 * application.
 *
 * <p>
 * Example:
 *
 * <table style="width:100%; border-collapse:collapse;" border=1>
 * <tr>
 * <th>Server A</th>
 * <th>Server B</th>
 * </tr>
 *
 * <tr>
 * <td><code>String requestMetadata = NewRelic.getAgent().getTransaction().getRequestMetadata();</code>
 * <p>
 * &#47;&#47; send request containing requestMetadata to Server B.</td>
 * <td></td>
 * </tr>
 *
 * <tr>
 * <td></td>
 * <td>
 * &#47;&#47; read request metadata sent by Server A.
 * <code>NewRelic.getAgent().getTransaction().processRequestMetadata(requestMetadata);</code></td>
 * </tr>
 *
 * <tr>
 * <td></td>
 * <td>
 * <p>
 * &#47;&#47; right before responding to Server A's request: <br>
 * <code>String responseMetadata = NewRelic.getAgent().getTransaction().getResponseMetadata();</code> <br>
 * &#47;&#47; send response containing responseMetadata to Server A.</td>
 * </tr>
 *
 * <tr>
 * <td>&#47;&#47; read response metadata sent by Server B.
 * <code>NewRelic.getAgent().getTransaction().processRequestMetadata(responseMetadata)<code>;</td>
 * <td></td>
 * </tr>
 *
 * </table>
 *
 * <p>
 * Note: not all transports support a response. In these cases, responses are optional.
 *
 * @author sdaubin
 * 
 * @see Agent#getTransaction()
 * @see Trace#dispatcher()
 */
public interface Transaction {

    /**
     * Sets the current transaction's name using the given priority. Higher priority levels are given precedence, and if
     * the name is set many times with the same priority, the first call wins unless override is true.
     * 
     * @param namePriority The priority of the new transaction name.
     * @param override Overrides the current transaction name if it has the same priority level (or lower).
     * @param category The type of transaction. This is the second segment of the full transaction metric name.
     * @param parts The category and all of the parts are concatenated together with / characters to create the full
     *        name.
     * @return Returns true if the transaction name was successfully changed, else false.
     * @since 3.9.0
     */
    boolean setTransactionName(TransactionNamePriority namePriority, boolean override, String category, String... parts);

    /**
     * Returns true if the transaction name has been set. This method is inherently unreliable in the presence of
     * transactions with multiple threads, because another thread may set the transaction name after this method returns
     * but before the caller can act on the return value.
     * 
     * @return True if the transaction name has already been set, else false.
     * @since 3.9.0
     */
    boolean isTransactionNameSet();

    /**
     * Returns this transaction's last tracer.
     * 
     * @deprecated use {@link #getTracedMethod}.
     * @since 3.9.0
     */
    @Deprecated
    TracedMethod getLastTracer();

    /**
     * Returns the {@link TracedMethod} enclosing the caller.
     * 
     * @return The {@link TracedMethod} enclosing the caller. The return value is <code>null</code> if the caller is not
     *         within a transaction.
     * @since 3.9.0
     */
    TracedMethod getTracedMethod();

    /**
     * Ignore this transaction so that none of its data is reported to the New Relic service.
     * @since 3.9.0
     */
    void ignore();

    /**
     * Ignore the current transaction for calculating Apdex score.
     * @since 3.9.0
     */
    void ignoreApdex();

    /**
     * Get transaction metadata to include in an outbound call. Only use this method when the agent doesn't provide
     * cross application tracing support for your application.
     * <p>
     * Server A
     * <p>
     * <b>String requestMetadata = NewRelic.getAgent().getTransaction().getRequestMetadata();</b>
     * <p>
     * ...send requestMetadata to Server B as part of an outbound call.
     * 
     * @return A string representation of the metadata required for linking this transaction to a remote child
     *         transaction, or null if no metadata should be sent.
     * @since 3.16.1
     */
    String getRequestMetadata();

    /**
     * Provide the metadata string from a remote transaction's call to {@link #getRequestMetadata()} to the current
     * transaction.
     *
     * <p>
     * Server B
     * <p>
     * ... get requestMetadata from request.
     * <p>
     * <b>NewRelic.getAgent().getTransaction().processRequestMetadata(requestMetadata);</b>
     * <p>
     *
     * @param requestMetadata metadata string received from an inbound request.
     * @since 3.16.1
     */
    void processRequestMetadata(String requestMetadata);

    /**
     * Get transaction metadata to include in an outbound response. Only use this method when the agent doesn't provide
     * cross application tracing support for your application.
     * <p>
     * <b>This call is time sensitive and should be made as close as possible to the code that writes the response.</b>
     *
     * <p>
     * Server B
     * <p>
     * <b>String responseMetadata = NewRelic.getAgent().getTransaction.getResponseMetadata();</b>
     * <p>
     * ... send response containing responseMetadata to server A.
     *
     * @return A string representation of the metadata required when responding to a remote transaction's call, or null
     *         if no metadata should be sent.
     * @since 3.16.1
     */
    String getResponseMetadata();

    /**
     * Provide the metadata string from a remote transaction's call to {@link #getResponseMetadata()} to the current
     * transaction. This should only be called on the originating transaction as a response to a remote call.
     *
     * <p>
     * Server A
     * <p>
     * ... get response containing responseMetadata from call to server B.
     * <p>
     * <b>NewRelic.getAgent().getTransaction().processResponseMetadata(responseMetadata);</b>
     * 
     * @param responseMetadata metadata string from a remote transaction (generated by {@link #getResponseMetadata()}).
     *
     * @since 3.16.1
     */
    void processResponseMetadata(String responseMetadata);
}
