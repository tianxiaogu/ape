package com.android.commands.monkey.ape;

import com.android.commands.monkey.ape.model.ModelAction;
import com.android.commands.monkey.ape.model.State;
import com.android.commands.monkey.ape.tree.GUITreeWidgetDiffer;

public class OnlyAddedUnsaturatedActionFilter extends BaseActionFilter {

    GUITreeWidgetDiffer differ = new GUITreeWidgetDiffer();

    public int diff(State s1, State s2) {
        return differ.diff(s1, s2);
    }

    @Override
    public boolean include(ModelAction action) {
        if (action.isSaturated()) {
            return false;
        }
        if (!action.isValid()) {
            return false;
        }
        if (!action.requireTarget()) {
            return false; // Include BACK
        }
        if (!differ.hasAdded()) {
            return true; // No added, include all
        }
        return differ.isAdded(action.getTarget());
    }

    public boolean hasAdded() {
        return differ.hasAdded();
    }

    public void print() {
        differ.print();
    }

}
