package com.android.commands.monkey.ape.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.android.commands.monkey.ape.model.State;
import com.android.commands.monkey.ape.naming.Name;
import com.android.commands.monkey.ape.naming.NamerFactory;
import com.android.commands.monkey.ape.utils.Logger;

public class GUITreeWidgetDiffer {
    private final List<Name> added = new ArrayList<>();
    private final List<Name> deleted = new ArrayList<>();
    private final List<Name> unchanged = new ArrayList<>();

    public boolean hasAdded() {
        return !added.isEmpty();
    }

    public boolean hasDeleted() {
        return !deleted.isEmpty();
    }

    public int diff(State s1, State s2) {
        deleted.clear();
        unchanged.clear();
        added.clear();
        // widgets are ordered
        Name[] ws1 = s1.getWidgets();
        Name[] ws2 = s2.getWidgets();
        int i1 = 0;
        int i2 = 0;
        while (i1 < ws1.length && i2 < ws2.length) {
            Name w1 = ws1[i1];
            Name w2 = ws2[i2];

            int cmp = w1.compareTo(w2);
            if (cmp == 0) {
                if (NamerFactory.hasAction(w1)) {
                    this.unchanged.add(w1);
                }
                i1++;
                i2++;
            } else if (cmp < 0) {
                if (NamerFactory.hasAction(w1)) {
                    this.deleted.add(w1);
                }
                i1++;
            } else {
                if (NamerFactory.hasAction(w2)) {
                    added.add(w2);
                }
                i2++;
            }
        }

        while (i1 < ws1.length) {
            Name w = ws1[i1++];
            if (NamerFactory.hasAction(w)) {
                deleted.add(w);
            }
        }

        while (i2 < ws2.length) {
            Name w = ws2[i2++];
            if (NamerFactory.hasAction(w)) {
                added.add(w);
            }
        }
        return 0;
    }

    public void print() {
        Logger.println("##################################");
        int count = 0;
        for (Name d : deleted) {
            Logger.format("%3d - %s", count++, d);
        }
        Logger.println("----------------------------------");
        count = 0;
        for (Name a : unchanged) {
            Logger.format("%3d = %s", count++, a);
        }
        Logger.println("----------------------------------");
        count = 0;
        for (Name a : added) {
            Logger.format("%3d + %s", count++, a);
        }
        Logger.println("##################################");
    }

    public boolean isAdded(Name widget) {
        if (added.size() < 4) {
            return added.contains(widget);
        }
        return Collections.binarySearch(added, widget) >= 0;
    }

    public List<Name> getAdded() {
        return added;
    }

}
