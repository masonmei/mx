package com.newrelic.agent.util;

import java.io.UnsupportedEncodingException;

import com.newrelic.org.apache.axis.encoding.Base64;

public class Obfuscator {
    public static String obfuscateNameUsingKey(String name, String key) throws UnsupportedEncodingException {
        byte[] encodedBytes = name.getBytes("UTF-8");
        byte[] keyBytes = key.getBytes();
        return Base64.encode(encode(encodedBytes, keyBytes));
    }

    private static byte[] encode(byte[] bytes, byte[] keyBytes) {
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = ((byte) (bytes[i] ^ keyBytes[(i % keyBytes.length)]));
        }
        return bytes;
    }

    public static String deobfuscateNameUsingKey(String name, String key) throws UnsupportedEncodingException {
        byte[] bytes = Base64.decode(name);
        byte[] keyBytes = key.getBytes();
        return new String(encode(bytes, keyBytes), "UTF-8");
    }
}