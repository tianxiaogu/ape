package com.android.commands.monkey.ape.naming;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NamerLattice {

    private Map<EnumSet<NamerType>, Namer> typeToNamer = new HashMap<EnumSet<NamerType>, Namer>();
    private Map<EnumSet<NamerType>, Collection<Namer>> typeToUpper = new HashMap<EnumSet<NamerType>, Collection<Namer>>();
    private Map<EnumSet<NamerType>, Collection<Namer>> typeToLower = new HashMap<EnumSet<NamerType>, Collection<Namer>>();

    private Map<EnumSet<NamerType>, Collection<Namer>> typeToNotAbove = new HashMap<EnumSet<NamerType>, Collection<Namer>>();
    private Map<EnumSet<NamerType>, Collection<Namer>> typeToNotBelow = new HashMap<EnumSet<NamerType>, Collection<Namer>>();

    private Namer bottomNamer;
    private Namer topNamer;

    NamerLattice(List<Namer> namers) {
        for (Namer namer : namers) {
            if (typeToNamer.put(namer.getNamerTypes(), namer) != null) {
                throw new IllegalArgumentException("Duplicated namer: " + namer.getNamerTypes());
            }
        }
        bottomNamer = typeToNamer.get(NamerType.noneOf());
        topNamer = typeToNamer.get(NamerType.allOf());
        if (bottomNamer == null || topNamer == null) {
            throw new IllegalArgumentException("Incomplete lattice");
        }
        for (Namer namer : namers) {
            EnumSet<NamerType> compl = NamerType.complementOf(namer.getNamerTypes());
            if (!typeToNamer.containsKey(compl)) {
                throw new IllegalArgumentException("Incomplete lattice");
            }
        }
        for (EnumSet<NamerType> type : typeToNamer.keySet()) {
            List<Namer> upper = new ArrayList<Namer>();
            List<Namer> lower = new ArrayList<Namer>();
            List<Namer> notAbove = new ArrayList<Namer>();
            List<Namer> notBelow = new ArrayList<Namer>();
            for (Namer namer : all()) {
                EnumSet<NamerType> otherType = namer.getNamerTypes();
                if (otherType.equals(type)) {
                    continue; // skip self
                }
                if (otherType.containsAll(type)) {
                    upper.add(namer);
                    notBelow.add(namer);
                } else if (type.containsAll(otherType)) {
                    lower.add(namer);
                    notAbove.add(namer);
                } else {
                    notBelow.add(namer);
                    notAbove.add(namer);
                }
            }
            typeToUpper.put(type, upper);
            typeToLower.put(type, lower);
            typeToNotAbove.put(type, notAbove);
            typeToNotBelow.put(type, notBelow);
        }
    }

    Namer join(Namer namer1, Namer namer2) {
        EnumSet<NamerType> type = EnumSet.copyOf(namer1.getNamerTypes());
        type.addAll(namer2.getNamerTypes());
        return getNamer(type);
    }

    Namer meet(Namer namer1, Namer namer2) {
        EnumSet<NamerType> type1 = namer1.getNamerTypes();
        EnumSet<NamerType> type2 = namer2.getNamerTypes();
        EnumSet<NamerType> type = EnumSet.copyOf(type1);
        for (NamerType t : type1) {
            if (!type2.contains(t)) {
                type.remove(t);
            }
        }
        return getNamer(type);
    }

    static Comparator<Namer> comparator = new Comparator<Namer>() {

        @Override
        public int compare(Namer o1, Namer o2) {
            EnumSet<NamerType> t1 = o1.getNamerTypes();
            EnumSet<NamerType> t2 = o2.getNamerTypes();
            int ret = t1.size() - t2.size();
            if (ret != 0) {
                return ret;
            }
            int c1 = 0;
            int c2 = 0;
            for (NamerType n : t1) {
                c1 += n.ordinal();
            }
            for (NamerType n : t2) {
                c2 += n.ordinal();
            }
            return c1 - c2;
        }

    };

    Collection<Namer> sort(Collection<Namer> namers) {
        List<Namer> sorted = new ArrayList<Namer>(namers);
        Collections.sort(sorted, comparator);
        return sorted;
    }

    Collection<Namer> getNotAbove(Namer namer) {
        return typeToNotAbove.get(namer.getNamerTypes());
    }

    Collection<Namer> getNotBelow(Namer namer) {
        return typeToNotBelow.get(namer.getNamerTypes());
    }

    Collection<Namer> getLower(Namer namer) {
        return typeToLower.get(namer.getNamerTypes());
    }

    Collection<Namer> getUpper(Namer namer) {
        return typeToUpper.get(namer.getNamerTypes());
    }

    Namer getNamer(EnumSet<NamerType> types) {
        return typeToNamer.get(types);
    }

    Namer getNamer(NamerType first, NamerType... others) {
        EnumSet<NamerType> t = EnumSet.of(first, others);
        return typeToNamer.get(t);
    }

    Namer getBottomNamer() {
        return bottomNamer;
    }

    Namer getTopNamer() {
        return topNamer;
    }

    Collection<Namer> all() {
        return typeToNamer.values();
    }

    public List<Namer> getSortedAbove(Namer namer) {
        Collection<Namer> namers = getUpper(namer);
        List<Namer> sorted = new ArrayList<Namer>(namers);
        Collections.sort(sorted, comparator);
        return sorted;
    }
}
