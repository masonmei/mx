package com.newrelic.agent.trace;

import java.util.Random;

public class TransactionGuidFactory {
    private static final ThreadLocal<Random> randomHolder = new ThreadLocal() {
        protected Random initialValue() {
            return new Random();
        }
    };

    public static String generateGuid() {
        char[] hexchars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        long random = ((Random) randomHolder.get()).nextLong();
        char[] result = new char[16];
        for (int i = 0; i < 16; i++) {
            result[i] = hexchars[((int) (random & 0xF))];
            random >>= 4;
        }
        return new String(result);
    }
}