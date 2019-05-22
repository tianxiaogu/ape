package com.android.commands.monkey.ape.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.android.commands.monkey.ape.ActionFilter;
import com.android.commands.monkey.ape.utils.Logger;

public class StateActionDiffer {

    static Comparator<ModelAction> comparator = new Comparator<ModelAction>() {

        @Override
        public int compare(ModelAction o1, ModelAction o2) {
            int ret = o1.getTarget().compareTo(o2.getTarget());
            if (ret != 0) {
                return ret;
            }
            return o1.getType().compareTo(o2.getType());
        }
    };

    public List<ModelAction> getUnsaturated(State from, State to) {
        return getUnsaturated(from, to, false);
    }

    protected void checkAndAdd(List<ModelAction> results, ModelAction action) {
        if (ActionFilter.ENABLED_VALID.include(action)) {
            results.add(action);
        }
    }
    
    public List<ModelAction> getUnsaturated(State from, State to, boolean verbose) {
        if (from == null) {
            return to.getUnsaturatedActions();
        }
        if (!from.getActivity().equals(to.getActivity())) {
            return to.getUnsaturatedActions();
        }
        List<ModelAction> added = new ArrayList<>();
        List<ModelAction> matched = new ArrayList<>();
        List<ModelAction> deleted = new ArrayList<>();

        List<ModelAction> results = new ArrayList<>();
        List<ModelAction> fromActions = from.targetedActions();
        List<ModelAction> toActions   = to.targetedActions();
        Collections.sort(fromActions, comparator);
        Collections.sort(toActions, comparator);

        int i1 = 0;
        int i2 = 0;
        while (i1 < fromActions.size() && i2 < toActions.size()) {
            ModelAction a1 = fromActions.get(i1);
            ModelAction a2 = toActions.get(i2);
            int cmp = comparator.compare(a1, a2);
            if (cmp == 0) {
                if (!(a1.isSaturated() || a2.isSaturated())) {
                    checkAndAdd(results, a2);
                }
                i1++;
                i2++;
                matched.add(a1);
                matched.add(a2);
            } else if (cmp < 0) {
                deleted.add(a1);
                i1++;
            } else {
                if (!a2.isSaturated()) {
                    checkAndAdd(results, a2);
                }
                added.add(a2);
                i2++;
            }
        }

        while (i1 < fromActions.size()) {
            ModelAction a = fromActions.get(i1++);
            deleted.add(a);
        }
        while (i2 < toActions.size()) {
            ModelAction a = toActions.get(i2++);
            if (!a.isSaturated()) {
                checkAndAdd(results, a);
            }
            added.add(a);
        }

        if (verbose) {
            Logger.println("##################################");
            int count = 0;
            for (ModelAction d : deleted) {
                Logger.format("%3d - %s", count++, d);
            }
            Logger.println("----------------------------------");
            count = 0;
            for (int i = 0; i < matched.size(); ) {
                Logger.format("%3d = %s", count++, matched.get(i++));
                Logger.format("    = %s", matched.get(i++));
            }
            Logger.println("----------------------------------");
            count = 0;
            for (ModelAction a : added) {
                Logger.format("%3d + %s", count++, a);
            }
            Logger.println("++++++++++++++++++++++++++++++++++");
            count = 0;
            for (ModelAction r : results) {
                Logger.format("%3d * %s", count++, r);
            }
            Logger.println("##################################");
        }
        return results;
    }
}
