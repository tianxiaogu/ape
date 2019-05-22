package com.android.commands.monkey.ape.naming;

import java.io.Serializable;
import java.util.EnumSet;

public abstract class AbstractNamer implements Namer, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    protected EnumSet<NamerType> namerType;

    AbstractNamer(EnumSet<NamerType> namerType) {
        this.namerType = namerType;
    }

    public EnumSet<NamerType> getNamerTypes() {
        return namerType;
    }

    public boolean refinesTo(Namer namer) {
        return this.namerType.containsAll(namer.getNamerTypes());
    }
}
