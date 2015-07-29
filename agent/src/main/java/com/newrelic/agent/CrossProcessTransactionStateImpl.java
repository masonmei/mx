package com.newrelic.agent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.service.ServiceUtils;
import com.newrelic.agent.tracers.AbstractExternalComponentTracer;
import com.newrelic.agent.tracers.CrossProcessNameFormat;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.util.Obfuscator;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.OutboundHeaders;
import com.newrelic.api.agent.Response;
import com.newrelic.deps.com.google.common.collect.MapMaker;
import com.newrelic.deps.com.google.common.collect.Sets;
import com.newrelic.deps.org.json.simple.JSONArray;
import com.newrelic.deps.org.json.simple.JSONValue;

public class CrossProcessTransactionStateImpl implements CrossProcessTransactionState {
    private static final boolean OPTIMISTIC_TRACING = false;
    private static final int ALTERNATE_PATH_HASH_MAX_COUNT = 10;
    private static final String UNKNOWN_HOST = "Unkown";
    private final ITransaction tx;
    private final Object lock;
    private final Set<String> alternatePathHashes;
    private volatile String tripId;
    private volatile boolean isCatOriginator = false;
    private volatile boolean processOutboundResponseDone = false;

    private CrossProcessTransactionStateImpl(ITransaction tx) {
        this.tx = tx;
        if ((tx instanceof Transaction)) {
            lock = ((Transaction) tx).getLock();
        } else {
            lock = new Object();
        }

        MapMaker factory = new MapMaker().initialCapacity(8).concurrencyLevel(4);
        alternatePathHashes = Sets.newSetFromMap(new LazyMapImpl<String, Boolean>(factory));
    }

    public static CrossProcessTransactionStateImpl create(ITransaction tx) {
        return tx == null ? null : new CrossProcessTransactionStateImpl(tx);
    }

    public void writeResponseHeaders() {
        if (tx.isIgnore()) {
            return;
        }
        Dispatcher dispatcher = tx.getDispatcher();
        if (dispatcher == null) {
            return;
        }
        Response response = dispatcher.getResponse();
        long contentLength = tx.getInboundHeaderState().getRequestContentLength();
        processOutboundResponseHeaders(response, contentLength);
    }

    public void processOutboundResponseHeaders(OutboundHeaders outboundHeaders, long contentLength) {
        if (outboundHeaders != null) {
            OutboundHeadersMap metadata = new OutboundHeadersMap(outboundHeaders.getHeaderType());
            boolean populated = populateResponseMetadata(metadata, contentLength);

            if ((populated) && (obfuscateMetadata(metadata))) {
                for (Entry entry : metadata.entrySet()) {
                    outboundHeaders.setHeader((String) entry.getKey(), (String) entry.getValue());
                }
            }
        }
    }

    private boolean obfuscateMetadata(Map<String, String> metadata) {
        if ((metadata == null) || (metadata.isEmpty())) {
            return false;
        }

        String encodingKey = tx.getCrossProcessConfig().getEncodingKey();
        if (encodingKey == null) {
            Agent.LOG.finer("Metadata obfuscation failed. Encoding key is null");
            return false;
        }

        for (Entry<String, String> entry : metadata.entrySet()) {
            try {
                String obfuscatedValue = Obfuscator.obfuscateNameUsingKey(entry.getValue(), encodingKey);
                entry.setValue(obfuscatedValue);
            } catch (UnsupportedEncodingException e) {
                Agent.LOG.finest(MessageFormat.format("Metadata obfuscation failed. {0}", e));
                return false;
            }
        }

        return true;
    }

    private boolean populateResponseMetadata(OutboundHeaders headers, long contentLength) {
        if (!tx.getCrossProcessConfig().isCrossApplicationTracing()) {
            return false;
        }

        synchronized(lock) {
            if ((tx.isIgnore()) || (!tx.getInboundHeaderState().isTrustedCatRequest())
                        || (processOutboundResponseDone)) {
                return false;
            }

            tx.freezeTransactionName();

            long durationInNanos = tx.getRunningDurationInNanos();
            recordClientApplicationMetric(durationInNanos);

            writeCrossProcessAppDataResponseHeader(headers, durationInNanos, contentLength);
            processOutboundResponseDone = true;
        }
        return true;
    }

    public void processOutboundRequestHeaders(OutboundHeaders outboundHeaders) {
        if (outboundHeaders != null) {
            OutboundHeadersMap metadata = new OutboundHeadersMap(outboundHeaders.getHeaderType());
            populateRequestMetadata(metadata);

            if (obfuscateMetadata(metadata)) {
                for (Entry entry : metadata.entrySet()) {
                    outboundHeaders.setHeader((String) entry.getKey(), (String) entry.getValue());
                }
            }
        }
    }

    public void populateRequestMetadata(OutboundHeaders headers) {
        if ((tx.getInboundHeaderState().isTrustedSyntheticsRequest()) &&
                    (tx.isInProgress()) && (!tx.isIgnore())) {
            String synHeader = tx.getInboundHeaderState().getUnparsedSyntheticsHeader();
            if (synHeader != null) {
                HeadersUtil.setSyntheticsHeader(headers, synHeader);
            }

        }

        if (!tx.getCrossProcessConfig().isCrossApplicationTracing()) {
            return;
        }

        synchronized(lock) {
            if ((null == tx.getDispatcher()) || (tx.isIgnore())) {
                return;
            }
            String crossProcessId = tx.getCrossProcessConfig().getEncodedCrossProcessId();

            if (crossProcessId != null) {
                if (Agent.LOG.isFinerEnabled()) {
                    Agent.LOG.log(Level.FINER, "Sending ID header: {0}", crossProcessId);
                }
                isCatOriginator = true;

                HeadersUtil.setIdHeader(headers, tx.getCrossProcessConfig().getCrossProcessId());

                String transactionHeaderValue = getTransactionHeaderValue();
                HeadersUtil.setTransactionHeader(headers, transactionHeaderValue);
            }
        }
    }

    private void doProcessInboundResponseHeaders(TracedMethod tracer, CrossProcessNameFormat crossProcessFormat,
                                                 String host, boolean addRollupMetrics) {
        if (crossProcessFormat != null) {
            if ((tracer instanceof DefaultTracer)) {
                DefaultTracer dt = (DefaultTracer) tracer;
                String transactionId = crossProcessFormat.getTransactionId();
                if ((transactionId != null) && (transactionId.length() > 0)) {
                    dt.setAttribute("transaction_guid", transactionId);
                }
                dt.setMetricNameFormat(crossProcessFormat);
                if (Agent.LOG.isFinestEnabled()) {
                    Agent.LOG
                            .log(Level.FINEST, "Received APP_DATA cross process response header for external call: {0}",
                                        crossProcessFormat.toString());
                }

            }

            if ((addRollupMetrics) && (!UNKNOWN_HOST.equals(host))) {
                tracer.addRollupMetricName(crossProcessFormat.getHostCrossProcessIdRollupMetricName());
            }
        }

        if (addRollupMetrics) {
            tracer.addRollupMetricName("External", host, "all");
            tracer.addRollupMetricName("External/all");
            if (Transaction.getTransaction().isWebTransaction()) {
                tracer.addRollupMetricName("External/allWeb");
            } else {
                tracer.addRollupMetricName("External/allOther");
            }
        }
    }

    public void processInboundResponseHeaders(InboundHeaders inboundHeaders, TracedMethod tracer, String host,
                                              String uri, boolean addRollupMetrics) {
        if (!tx.getCrossProcessConfig().isCrossApplicationTracing()) {
            return;
        }
        if ((inboundHeaders == null) || (tracer == null)) {
            return;
        }

        String encodedAppData = HeadersUtil.getAppDataHeader(inboundHeaders);
        String encodingKey = tx.getCrossProcessConfig().getEncodingKey();
        CrossProcessNameFormat crossProcessFormat =
                CrossProcessNameFormat.create(host, uri, encodedAppData, encodingKey);

        doProcessInboundResponseHeaders(tracer, crossProcessFormat, host, addRollupMetrics);
    }

    synchronized String getTransactionHeaderValue() {
        synchronized(lock) {
            String json =
                    getTransactionHeaderJson(tx.getGuid(), getForceTransactionTrace(), getTripId(), generatePathHash());

            if (Agent.LOG.isFinerEnabled()) {
                Agent.LOG.log(Level.FINER, "Sending TRANSACTION header: {0} obfuscated: {1}", json);
            }

            return json;
        }
    }

    private String getTransactionHeaderJson(String guid, boolean forceTransactionTrace, String trip, int pathHash) {
        List args = Arrays.asList(guid, forceTransactionTrace, trip, ServiceUtils.intToHexString(pathHash));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(out);
        try {
            JSONArray.writeJSONString(args, writer);
            writer.close();
            return out.toString();
        } catch (IOException e) {
            String msg = MessageFormat.format("Error getting JSON: {0}", e);
            Agent.LOG.error(msg);
        }
        return null;
    }

    private void writeCrossProcessAppDataResponseHeader(OutboundHeaders headers, long durationInNanos,
                                                        long contentLength) {
        String json = getCrossProcessAppDataJson(durationInNanos, contentLength);

        if (Agent.LOG.isLoggable(Level.FINER)) {
            Agent.LOG.log(Level.FINER, "Setting APP_DATA response header to: {0}", json);
        }

        if (json == null) {
            return;
        }

        HeadersUtil.setAppDataHeader(headers, json);
    }

    private String getCrossProcessAppDataJson(long durationInNanos, long contentLength) {
        String crossProcessId = tx.getCrossProcessConfig().getCrossProcessId();
        String transactionName = tx.getPriorityTransactionName().getName();
        Float queueTimeInSeconds = (float) tx.getExternalTime() / 1000.0F;
        Float durationInSeconds = (float) durationInNanos / 1.0E+09F;
        List args = Arrays.asList(crossProcessId, transactionName, queueTimeInSeconds, durationInSeconds, contentLength,
                                         tx.getGuid());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(out);
        try {
            JSONArray.writeJSONString(args, writer);
            writer.close();
            return out.toString();
        } catch (IOException e) {
            String msg = MessageFormat.format("Error getting JSON: {0}", e);
            Agent.LOG.error(msg);
        }
        return null;
    }

    private void recordClientApplicationMetric(long durationInNanos) {
        if (tx.getInboundHeaderState().isTrustedCatRequest()) {
            String metricName = MessageFormat.format("ClientApplication/{0}/all",
                                                            tx.getInboundHeaderState().getClientCrossProcessId());

            tx.getTransactionActivity().getTransactionStats().getUnscopedStats().getResponseTimeStats(metricName)
                    .recordResponseTime(durationInNanos, TimeUnit.NANOSECONDS);
        }
    }

    private boolean getForceTransactionTrace() {
        return false;
    }

    public String getTripId() {
        if (tripId == null) {
            tripId = tx.getInboundHeaderState().getInboundTripId();
        }
        if ((tripId == null) && (isCatOriginator)) {
            tripId = tx.getGuid();
        }
        return tripId;
    }

    public int generatePathHash() {
        synchronized(lock) {
            int pathHash = ServiceUtils.calculatePathHash(tx.getApplicationName(),
                                                                 tx.getPriorityTransactionName().getName(),
                                                                 tx.getInboundHeaderState().getReferringPathHash());

            if (alternatePathHashes.size() < ALTERNATE_PATH_HASH_MAX_COUNT) {
                alternatePathHashes.add(ServiceUtils.intToHexString(pathHash));
            }
            return pathHash;
        }
    }

    public String getAlternatePathHashes() {
        synchronized(lock) {
            Set<String> hashes = new TreeSet<String>(alternatePathHashes);
            hashes.remove(ServiceUtils.intToHexString(generatePathHash()));
            StringBuilder result = new StringBuilder();
            for (String alternatePathHash : hashes) {
                result.append(alternatePathHash);
                result.append(",");
            }
            return result.length() > 0 ? result.substring(0, result.length() - 1) : null;
        }
    }

    public String getRequestMetadata() {
        OutboundHeadersMap metadata = new OutboundHeadersMap(HeaderType.MESSAGE);
        populateRequestMetadata(metadata);

        if (metadata.isEmpty()) {
            return null;
        }

        String serializedMetadata = JSONValue.toJSONString(metadata);
        String encodingKey = tx.getCrossProcessConfig().getEncodingKey();
        try {
            return Obfuscator.obfuscateNameUsingKey(serializedMetadata, encodingKey);
        } catch (UnsupportedEncodingException e) {
            Agent.LOG.log(Level.FINEST, "Error encoding metadata {0} using key {1}: {2}", serializedMetadata,
                                 encodingKey, e);
        }
        return null;
    }

    public void processRequestMetadata(String requestMetadata) {
        InboundHeaders headers = decodeMetadata(requestMetadata);
        Transaction.getTransaction().provideRawHeaders(headers);
    }

    public String getResponseMetadata() {
        OutboundHeadersMap metadata = new OutboundHeadersMap(HeaderType.MESSAGE);
        populateResponseMetadata(metadata, -1L);

        if (metadata.isEmpty()) {
            return null;
        }

        String serializedMetadata = JSONValue.toJSONString(metadata);
        String encodingKey = tx.getCrossProcessConfig().getEncodingKey();
        try {
            return Obfuscator.obfuscateNameUsingKey(serializedMetadata, encodingKey);
        } catch (UnsupportedEncodingException e) {
            Agent.LOG.log(Level.SEVERE, "Error encoding metadata {0} using key {1}: {2}", serializedMetadata,
                                 encodingKey, e);
        }
        return null;
    }

    public void processResponseMetadata(String responseMetadata) {
        if (!tx.getCrossProcessConfig().isCrossApplicationTracing()) {
            return;
        }

        if (responseMetadata == null) {
            return;
        }

        Tracer lastTracer = tx.getTransactionActivity().getLastTracer();
        if (lastTracer == null) {
            return;
        }

        if ((lastTracer instanceof AbstractExternalComponentTracer)) {
            AbstractExternalComponentTracer externalTracer = (AbstractExternalComponentTracer) lastTracer;
            String host = externalTracer.getHost();
            String uri = tx.getDispatcher().getUri();

            InboundHeaders NRHeaders = decodeMetadata(responseMetadata);
            if (NRHeaders != null) {
                String decodedAppData = HeadersUtil.getAppDataHeader(NRHeaders);
                CrossProcessNameFormat crossProcessFormat = CrossProcessNameFormat.create(host, uri, decodedAppData);
                doProcessInboundResponseHeaders(lastTracer, crossProcessFormat, host, true);
            }
        }
    }

    private InboundHeaders decodeMetadata(String metadata) {
        String deobfuscatedMetadata;
        try {
            String encodingKey = tx.getCrossProcessConfig().getEncodingKey();

            if (encodingKey == null) {
                return null;
            }

            deobfuscatedMetadata = Obfuscator.deobfuscateNameUsingKey(metadata, encodingKey);
        } catch (UnsupportedEncodingException e) {
            return null;
        }

        Object obj = JSONValue.parse(deobfuscatedMetadata);
        if (obj == null) {
            return null;
        }

        if (!(obj instanceof Map)) {
            return null;
        }

        final Map delegate = (Map) obj;
        return new InboundHeaders() {
            public HeaderType getHeaderType() {
                return HeaderType.MESSAGE;
            }

            public String getHeader(String name) {
                if (delegate.containsKey(name)) {
                    return delegate.get(name).toString();
                }
                return null;
            }
        };
    }
}