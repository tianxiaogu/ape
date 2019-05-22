package com.android.commands.monkey.ape.naming;

import java.util.Arrays;
import java.util.EnumSet;

import com.android.commands.monkey.ape.utils.Config;

public enum NamerType {
    TYPE, INDEX, PARENT, TEXT, ANCESTOR;

    public static final NamerType[] used;
    static {
        if (Config.useAncestorNamer) {
            used = new NamerType[] { TYPE, INDEX, PARENT, TEXT, ANCESTOR };
        } else {
            used = new NamerType[] { TYPE, INDEX, PARENT, TEXT };
        }
    }

    public static final EnumSet<NamerType> allOf() {
        return EnumSet.copyOf(Arrays.asList(used));
    }

    public static final EnumSet<NamerType> noneOf() {
        return EnumSet.noneOf(NamerType.class);
    }

    public static final EnumSet<NamerType> complementOf(EnumSet<NamerType> set) {
        EnumSet<NamerType> empty = noneOf();
        for (NamerType n : used) {
            empty.add(n);
        }
        return empty;
    }

    public static final EnumSet<NamerType> add(EnumSet<NamerType> set, NamerType type) {
        EnumSet<NamerType> ret = EnumSet.copyOf(set);
        ret.add(type);
        return ret;
    }

    public boolean isLocal() {
        return this != PARENT && this != ANCESTOR;
    }
}
