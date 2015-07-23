package com.newrelic.agent.service;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicInteger;

public class ServiceUtils {
    private static final int ROTATED_BIT_SHIFT = 31;
    private static final String PATH_HASH_SEPARATOR = ";";

    public static int calculatePathHash(String appName, String txName, Integer optionalReferringPathHash) {
        int referringPathHash = optionalReferringPathHash == null ? 0 : optionalReferringPathHash.intValue();
        int rotatedReferringPathHash = referringPathHash << 1 | referringPathHash >>> 31;
        return rotatedReferringPathHash ^ getHash(appName, txName);
    }

    public static int reversePathHash(String appName, String txName, Integer optionalReferringPathHash) {
        int referringPathHash = optionalReferringPathHash == null ? 0 : optionalReferringPathHash.intValue();
        int rotatedReferringPathHash = referringPathHash ^ getHash(appName, txName);
        return rotatedReferringPathHash >>> 1 | rotatedReferringPathHash << 31;
    }

    private static int getHash(String appName, String txName) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest((appName + ";" + txName).getBytes("UTF-8"));

            return (digest[12] & 0xFF) << 24 | (digest[13] & 0xFF) << 16 | (digest[14] & 0xFF) << 8 | digest[15] & 0xFF;
        } catch (NoSuchAlgorithmException e) {
            return 0;
        } catch (UnsupportedEncodingException e) {
        }
        return 0;
    }

    public static String intToHexString(int val) {
        return String.format("%08x", new Object[] {Integer.valueOf(val)});
    }

    public static int hexStringToInt(String val) {
        return (int) Long.parseLong(val, 16);
    }

    public static void readMemoryBarrier(AtomicInteger i) {
        if (i.get() == -1) {
            i.set(0);
        }
    }

    public static void writeMemoryBarrier(AtomicInteger i) {
        i.incrementAndGet();
    }
}