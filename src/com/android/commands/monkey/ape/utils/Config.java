package com.android.commands.monkey.ape.utils;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class Config {

    static private final Properties configurations;
    // DO NOT move this static block as it should be executed first.
    static {
        configurations = new Properties(System.getProperties());
        loadConfiguration("/data/local/tmp/ape.properties");
        loadConfiguration("/sdcard/ape.properties");
    }

    /**
     * Readonly
     */
    public static final boolean takeScreenshot = Config.getBoolean("ape.takeScreenshot", true);
    public static final boolean takeScreenshotForNewState = Config.getBoolean("ape.takeScreenshotForNewState", false);
    public static final boolean takeScreenshotForEveryStep = Config.getBoolean("ape.takeScreenshotForEveryStep", true);
    public static final boolean saveGUITreeToXmlEveryStep = Config.getBoolean("ape.saveGUITreeToXmlEveryStep", true);

    public static final int throttleForUnvisitedAction = Config.getInteger("ape.throttleForUnvisitedAction", 200);
    public static final int throttleForActivityTransition = Config.getInteger("ape.throttleForActivityTransition", 500);
    public static final int baseThrottle = Config.getInteger("ape.baseThrottle", 0);
    public static final int maxThrottle = Config.getInteger("ape.maxThrottle", 5000);
    public static final int graphStableRestartThreshold = Config.getInteger("ape.graphStableRestartThreshold", 100);
    public static final int activityStableRestartThreshold = Config.getInteger("ape.activityStableRestartThreshold", Integer.MAX_VALUE);
    public static final int stateStableRestartThreshold = Config.getInteger("ape.stateStableRestartThreshold", 50);
    public static final int maxExtraPriorityAliasedActions = Config.getInteger("ape.maxExtraPriorityAliasedActions", 5);

    public static final boolean saveDotGraph = Config.getBoolean("ape.saveDotGraph", false);
    public static final boolean saveObjModel = Config.getBoolean("ape.saveObjModel", true);
    public static final boolean saveVisGraph = Config.getBoolean("ape.saveVisGraph", true);

    public static final boolean enableXPathAction = Config.getBoolean("ape.enableXPathAction", false);
    public static final boolean evolveModel = Config.getBoolean("ape.evolveModel", true);
    public static final boolean saveStates = Config.getBoolean("ape.saveStates", true);
    public static final int fuzzingActivityVisitThreshold = Config.getInteger("ape.fuzzingActivityVisitThreshold", 10);

    public static final boolean checkRestart = Config.getBoolean("ape.checkRestart", true);
    public static final int restartThresholdMax = Config.getInteger("ape.restartThresholdMax", 300);
    public static final int restartThresholdMin = Config.getInteger("ape.restartThresholdMin", 100);
    public static final double inputRate = Config.getDouble("ape.inputRate", 0.8D);

    public static final String activityManagerType = Config.get("ape.activityManagerType", "state");

    public static final int actionRefinmentThreshold = Config.getInteger("ape.actionRefinmentThreshold", 3);
    public static final int maxInitialNamesPerStateThreshold = Config.getInteger("ape.maxInitialNamesPerStateThreshold", 20);
    public static final boolean actionRefinementFirst = Config.getBoolean("ape.actionRefinementFirst", true);

    public static final boolean alwaysIgnoreWebView = Config.getBoolean("ape.alwaysIgnoreWebView", false); // false;
    public static final boolean alwaysIgnoreWebViewAction = Config.getBoolean("ape.alwaysIgnoreWebViewAction", false); // false;
    public static final int ignoreWebViewThreshold = Config.getInteger("ape.ignoreWebViewThreshold", 64);
    public static final boolean excludeEmptyChild = Config.getBoolean("ape.excludeEmptyChild", true);
    public static final boolean excludeInvisibleNode = Config.getBoolean("ape.excludeInvisibleNode", true);

    public static final boolean patchGUITree = Config.getBoolean("ape.patchGUITree", true);
    public static final boolean computeImageText = Config.getBoolean("ape.computeImageText", true);

    public static final double defaultEpsilon = Config.getDouble("ape.defaultEpsilon", 0.05D); // 0.05D;

    public static final boolean fillTransitionsByHistory = Config.getBoolean("ape.fillTransitionsByHistory", true); // true;
    public static final boolean fallbackToGraphTransition = Config.getBoolean("ape.fallbackToGraphTransition", true);
    public static final int trivialActivityRankThreshold = Config.getInteger("ape.trivialActivityRankThreshold", 3);
    public static final boolean useActionDiffer = Config.getBoolean("ape.useActionDiffer", true);
    public static final boolean doBackToTrivialActivity = Config.getBoolean("ape.doBackToTrivialActivity", false);

    public static final int flushImagesThreshold = Config.getInteger("ape.flushImagesThreshold", 10);
    public static final int imageWriterCount = Config.getInteger("ape.imageWriterCount", 3);
    public static final long defaultGUIThrottle = Config.getLong("ape.defaultGUIThrottle", 200L);
    public static final long swipeDuration = Config.getLong("ape.swipeDuration", 200);
    public static final double fuzzingRate = Config.getDouble("ape.fuzzingRate", 0.02D);
    public static final long refectchInfoWaitingInterval = Config.getLong("ape.refectchInfoWaitingInterval", 50);
    public static final int refectchInfoCount = Config.getInteger("ape.refectchInfoCount", 4);
    public static final boolean doFuzzing = Config.getBoolean("ape.doFuzzing", true);

    public static final boolean ignoreEmpty = Config.getBoolean("ape.ignoreEmpty", true);
    public static final boolean ignoreOutOfBounds = Config.getBoolean("ape.ignoreOutOfBounds", true);

    public static final boolean useAncestorNamer = Config.getBoolean("ape.useAncestorNamer", true);

    public static final int maxStringPieceLength = Config.getInteger("ape.maxStringPieceLength", 32);

    public static final int trivialActivityStateThreshold = Config.getInteger("ape.trivialActivityStateThreshold", 5);
    public static final int trivialActivityVisitThreshold = Config.getInteger("ape.trivialActivityVisitThreshold", 16);

    public static final int trivialStateActionThreshold = Config.getInteger("ape.trivialStateActionThreshold", 5);
    public static final int trivialStateWidgetThreshold = Config.getInteger("ape.trivialStateWidgetThreshold", 5);

    public static final boolean usePatchNamer = Config.getBoolean("ape.usePatchNamer", true);

    private static void loadConfiguration(String fileName) {
        File configFile = new File(fileName);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                configurations.load(fis);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Fail to load the configuration file at " + configFile);
            }
        }
    }

    public static Object set(String key, String value) {
        return configurations.setProperty(key, value);
    }

    public static Object setBoolean(String key, boolean value) {
        return configurations.setProperty(key, Boolean.toString(value));
    }

    public static Object setDouble(String key, double value) {
        return configurations.setProperty(key, Double.toString(value));
    }

    public static String get(String key) {
        return configurations.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        String value = get(key);
        if (value != null) {
            return value;
        }
        configurations.put(key, defaultValue);
        return defaultValue;
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value != null) {
            return Boolean.valueOf(value);
        }
        configurations.put(key, defaultValue);
        return defaultValue;
    }

    public static int getInteger(String key, int defaultValue) {
        String value = get(key);
        if (value != null) {
            try {
                return Integer.valueOf(value);
            } catch (NumberFormatException e) {
            }
        }
        configurations.put(key, defaultValue);
        return defaultValue;
    }

    public static long getLong(String key, long defaultValue) {
        String value = get(key);
        if (value != null) {
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException e) {
            }
        }
        configurations.put(key, defaultValue);
        return defaultValue;
    }

    public static double getDouble(String key, double defaultValue) {
        String value = get(key);
        if (value != null) {
            try {
                return Double.valueOf(value);
            } catch (NumberFormatException e) {
            }
        }
        configurations.put(key, defaultValue);
        return defaultValue;
    }

    public static void printConfigurations() {
        Logger.println("Configurations:");
        int maxLength = Integer.MIN_VALUE;
        List<String> keys = new ArrayList<>(configurations.size());
        for (Object key : configurations.keySet()) {
            int length = key.toString().length();
            if (length > maxLength) {
                maxLength = length;
            }
            keys.add(key.toString());
        }
        Collections.sort(keys);
        String formatter = String.format(" %%%ds: %%s", maxLength);
        for (String key : keys) {
            Logger.format(formatter, key, configurations.get(key));
        }
    }

}
