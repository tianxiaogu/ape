package com.android.commands.monkey.ape.naming;

import java.util.Comparator;
import java.util.EnumSet;

public class NamerComparator implements Comparator<Namer> {

    public static NamerComparator INSTANCE = new NamerComparator();

    private NamerComparator() {
    }

    public int compare(Namer n1, Namer n2) {
        EnumSet<NamerType> t1 = n1.getNamerTypes();
        EnumSet<NamerType> t2 = n2.getNamerTypes();
        int size = t1.size();
        int ret = size - t2.size();
        if (ret != 0) {
            return ret;
        }
        int c1 = 0;
        int c2 = 0;
        for (NamerType t : t1) {
            c1 += t.ordinal();
        }
        for (NamerType t : t2) {
            c2 += t.ordinal();
        }
        return c1 - c2;
    }
}
