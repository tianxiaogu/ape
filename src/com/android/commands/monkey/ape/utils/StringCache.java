/*
 * Copyright 2020 Advanced Software Technologies Lab at ETH Zurich, Switzerland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.commands.monkey.ape.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.android.commands.monkey.ape.utils.Config.truncateTextLength;

public class StringCache {


    public static Map<String, String> stringDict = new HashMap<String, String>();
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
                if (stringList.size() < maxStringListSize) {
                    stringList.add(string);
                }
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

    static final int maxStringListSize;

    static {
        File stringFiles = new File("/sdcard/ape.strings");
        if (stringFiles.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(stringFiles))) {
                String line = null;
                while ((line = br.readLine()) != null) {
                    stringList.add(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Fail to load the strings file at " + stringFiles);
            }
        }
        maxStringListSize = stringList.size() + Config.maxStringListSize;
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

    public static String nextString() {
        int i = ThreadLocalRandom.current().nextInt(stringList.size());
        String string = null;
        if (!stringList.isEmpty()) {
            string = stringList.get(i);
            Logger.iformat("Select [%s] %d/%d from string list", string, i, stringList.size());
        }

        if (string == null || RandomHelper.toss(Config.randomFormattedStringProp)) {
            string = RandomHelper.nextFormattedString();
            Logger.iformat("Use random string %s", string);
        }
        return string;
    }
}
