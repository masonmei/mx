package com.newrelic.agent.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Streams {
    public static final int DEFAULT_BUFFER_SIZE = 8192;

    public static int copy(InputStream input, OutputStream output) throws IOException {
        return copy(input, output, 8192, false);
    }

    public static int copy(InputStream input, OutputStream output, boolean closeStreams) throws IOException {
        return copy(input, output, 8192, closeStreams);
    }

    public static int copy(InputStream input, OutputStream output, int bufferSize) throws IOException {
        return copy(input, output, bufferSize, false);
    }

    public static int copy(InputStream input, OutputStream output, int bufferSize, boolean closeStreams)
            throws IOException {
        try {
            if (0 == bufferSize) {
                return 0;
            }
            byte[] buffer = new byte[bufferSize];
            int count = 0;
            int n = 0;
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
                count += n;
            }
            return count;
        } finally {
            if (closeStreams) {
                input.close();
                output.close();
            }
        }
    }

    public static byte[] read(InputStream input, boolean closeInputStream) throws IOException {
        return read(input, input.available(), closeInputStream);
    }

    public static byte[] read(InputStream input, int expectedSize, boolean closeInputStream) throws IOException {
        if (expectedSize <= 0) {
            expectedSize = 8192;
        }
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(expectedSize);
        copy(input, outStream, expectedSize, closeInputStream);
        return outStream.toByteArray();
    }
}