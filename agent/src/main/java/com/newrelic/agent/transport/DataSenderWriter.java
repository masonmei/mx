package com.newrelic.agent.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import com.newrelic.deps.org.json.simple.JSONValue;

import com.newrelic.org.apache.axis.encoding.Base64;

public class DataSenderWriter extends OutputStreamWriter {
    private static final int COMPRESSION_LEVEL = -1;

    protected DataSenderWriter(OutputStream out) {
        super(out);
    }

    public static final String nullValue() {
        return "null";
    }

    public static final boolean isCompressingWriter(Writer writer) {
        return !(writer instanceof DataSenderWriter);
    }

    public static final String getJsonifiedCompressedEncodedString(Object data, Writer writer) {
        return getJsonifiedCompressedEncodedString(data, writer, -1);
    }

    public static final String getJsonifiedCompressedEncodedString(Object data, Writer writer, int compressionLevel) {
        if ((writer instanceof DataSenderWriter)) {
            return toJSONString(data);
        }
        return getJsonifiedCompressedEncodedString(data, compressionLevel);
    }

    public static final String toJSONString(Object obj) {
        ByteArrayOutputStream oStream = new ByteArrayOutputStream();
        Writer writer = new DataSenderWriter(oStream);
        try {
            JSONValue.writeJSONString(obj, writer);
            writer.close();
            return oStream.toString();
        } catch (IOException e) {
        }
        return JSONValue.toJSONString(obj);
    }

    private static final String getJsonifiedCompressedEncodedString(Object data, int compressionLevel) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        OutputStream zipStream = new DeflaterOutputStream(outStream, new Deflater(compressionLevel));
        Writer out = new OutputStreamWriter(zipStream);
        try {
            JSONValue.writeJSONString(data, out);
            out.flush();
            out.close();
            outStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Base64.encode(outStream.toByteArray());
    }
}