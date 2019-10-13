package com.android.commands.monkey.ape.utils;

import org.w3c.dom.Document;

public class Logger {

    private static final boolean debug = false;

    public static final String TAG = "[APE] ";

    public static void println(Object message) {
        System.out.format("[APE] %s\n", message);
    }

    public static void format(String format, Object... args) {
        System.out.format("[APE] " + format + "\n", args);
    }

    public static void dformat(String format, Object... args) {
        if (debug)
            System.out.format("[APE] *** DEBUG *** " + format + "\n", args);
    }

    public static void wformat(String format, Object... args) {
        System.out.format("[APE] *** WARNING *** " + format + "\n", args);
    }

    public static void iformat(String format, Object... args) {
        System.out.format("[APE] *** INFO *** " + format + "\n", args);
    }

    public static void wprintln(Object message) {
        System.out.format("[APE] *** WARNING *** %s\n", message);
    }

    public static void dprintln(Object message) {
        if (debug) System.out.format("[APE] *** DEBUG *** %s\n", message);
    }

    public static void iprintln(Object message) {
        System.out.format("[APE] *** INFO *** %s\n", message);
    }

    public static void printXml(Document document) {
        try {
            Utils.printXml(System.out, document);
            System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
