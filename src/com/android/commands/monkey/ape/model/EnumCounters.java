package com.android.commands.monkey.ape.model;

import java.io.Serializable;

import com.android.commands.monkey.ape.utils.Logger;

public abstract class EnumCounters<E extends Enum<E>> implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -3224038897052376225L;
    private int[] counters;
    private int total;

    public EnumCounters() {
        counters = new int[getEnums().length];
    }

    public void logEvent(E e) {
        int i = e.ordinal();
        counters[i] = counters[i] + 1;
        total++;
    }

    public int getTotal() {
        return total;
    }

    public void print() {
        E[] es = getEnums();
        for (E e : es) {
            Logger.format("%6d  %s", counters[e.ordinal()], e);
        }
    }

    public abstract E[] getEnums();
}
