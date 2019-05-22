package com.android.commands.monkey.ape.utils;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class RandomHelper {

    public static Random getRandom() {
        return ThreadLocalRandom.current();
    }
    
    public static int nextInt() {
        return getRandom().nextInt();
    }

    public static int nextInt(int b) {
        return getRandom().nextInt(b);
    }

    public static boolean nextBoolean() {
        return getRandom().nextBoolean();
    }

    public static <V extends PriorityObject> V randomPickWithPriority(List<V> list) {
        if (list.isEmpty()) {
            return null;
        }
        int count = 0;
        for (V o : list) {
            if (o.getPriority() <= 0) {
                throw new IllegalStateException("Object should have a positive priority for random pick.");
            }
            count += o.getPriority();
        }
        int index = nextInt(count);
        count = 0;
        for (V o : list) {
            if (o.getPriority() <= 0) {
                throw new IllegalStateException("Object should have a positive priority for random pick.");
            }
            count += o.getPriority();
            if (count > index) {
                return o;
            }
        }
        throw new IllegalStateException("Should not reach here");
    }

    public static <V> V randomPick(List<V> list) {
        if (list.isEmpty()) {
            return null;
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        int index = nextInt(list.size());
        return list.get(index);
    }

    public static int nextBetween(int minValue, int maxValue) {
        if (maxValue <= minValue) {
            throw new IllegalArgumentException("max is not greater than min.");
        }
        return nextInt(maxValue - minValue) + minValue;
    }

    static final String CHARS = " abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()_+~-=`:\";'{}[]|\\'<>,.?/";

    public static String nextString() {
        return nextString(32);
    }

    public static String nextString(int maxLength) {
        int total = nextInt(maxLength);
        char[] value = new char[total];
        int charTotal = CHARS.length();
        for (int i = 0; i < total; i++) {
            value[i] = CHARS.charAt(nextInt(charTotal));
        }
        return String.valueOf(value);
    }

    public static int nextByte() {
        return nextBetween(Byte.MIN_VALUE, Byte.MAX_VALUE);
    }

    public static int nextShort() {
        return nextBetween(Short.MIN_VALUE, Short.MAX_VALUE);
    }

    public static double nextDouble() {
        return getRandom().nextDouble();
    }

    public static Long nextLong() {
        return getRandom().nextLong();
    }

    public static Float nextFloat() {
        return getRandom().nextFloat();
    }

    public static boolean toss(double d) {
        return nextDouble() < d;
    }
}
