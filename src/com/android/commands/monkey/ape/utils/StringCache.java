package com.android.commands.monkey.ape.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class StringCache {


    public static Map<String, String> stringDict = new HashMap<String, String>();
    static final List<String> strings; // = new ArrayList<String>();
    static final List<String> stringList = new ArrayList<>();

    public static String cacheString(String string) {
        return cacheString(string, false);
    }

    public static String cacheString(String string, boolean addToList) {
        if (string == null) {
            throw new NullPointerException("Cannot cache null string.");
        }
        if (string.length() == 0) {
            return "";
        }
        String existing = stringDict.get(string);
        if (existing == null) {
            stringDict.put(string, string);
            existing = string;
            if (addToList) {
                stringList.add(string);
            }
        }
        return existing;
    }

    public static String cacheStringEmptyOnNull(Object o) {
        return cacheStringEmptyOnNull(o, false);
    }

    public static String cacheStringEmptyOnNull(Object o, boolean addToList) {
        if (o == null) {
            return "";
        }
        String val = o.toString();
        return cacheString(val, addToList);
    }

    static {
        File stringFiles = new File("/sdcard/ape.strings");
        strings = new ArrayList<String>();
        if (stringFiles.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(stringFiles))) {
                String line = null;
                while ((line = br.readLine()) != null) {
                    strings.add(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Fail to load the strings file at " + stringFiles);
            }
        }
    }

    public static final int truncateLength = Config.getInteger("ape.truncateLength", 128);
    public static final int truncateTextLength = Config.getInteger("ape.truncateTextLength", 8);

    public static String truncate(String origin) {
        if (truncateLength < origin.length()) {
            return origin.substring(0, truncateLength);
        }
        return origin;
    }

    public static String removeQuotes(CharSequence input) {
        if (input == null) {
            return "";
        }
        return input.toString().replaceAll("\"", "");
    }

    public static String truncateText(CharSequence input) {
        if (input == null) {
            return "";
        }
        String origin = input.toString();
        if (truncateTextLength < origin.length()) {
            return origin.substring(0, truncateTextLength);
        }
        return origin;
    }

    public static String nextPredefinedString() {
        if (strings.isEmpty()) {
            return "";
        }
        if (strings.size() == 1) {
            return strings.get(0);
        }
        int i = ThreadLocalRandom.current().nextInt(strings.size());
        return strings.get(i);
    }

    public static String nextString() {
        if (!strings.isEmpty()) {
            return nextPredefinedString();
        }
        if (stringList.isEmpty()) {
            return "";
        }
        if (RandomHelper.nextBoolean()) {
            return RandomHelper.nextString();
        }
        int i = ThreadLocalRandom.current().nextInt(stringList.size());
        String string = stringList.get(i);
        int length = RandomHelper.nextInt(Config.maxStringPieceLength) + 1;
        if (string.length() <= length) {
            return string;
        }
        int begin = RandomHelper.nextInt(string.length() - length);
        return string.substring(begin, begin + length);
    }
}
