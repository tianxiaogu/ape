package com.android.commands.monkey.ape.naming;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.android.commands.monkey.ape.model.StateKey;
import com.android.commands.monkey.ape.tree.GUITree;
import com.android.commands.monkey.ape.tree.GUITreeTransition;
import com.android.commands.monkey.ape.utils.Logger;

public class AssertTargetsFewerThan extends AbstractGUITreePredicate {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    int threshold;

    public AssertTargetsFewerThan(Naming updatedNaming, List<List<GUITreeTransition>> transitions, int threshold) {
        super(updatedNaming, transitions);
        this.threshold = threshold;
    }

    @Override
    public boolean eval(NamingManager nm, Set<GUITree> affected, Naming naming) {
        StateKey state = null;
        Set<StateKey> states = new HashSet<>();
        for (List<GUITreeTransition> trees : transitions) {
            Iterator<GUITree> it = GUITreeTransition.targetTreeIterator(trees);
            while (it.hasNext()) {
                GUITree tree = it.next();
                state = getState(nm, affected, naming, tree);
                if (states.add(state)) {
                    if (states.size() > threshold) {
                        Logger.iformat("> eval %s: too many states %d > %d", this, states.size(), threshold);
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public Type getType() {
        return Predicate.Type.STATE_ABSTRACTION;
    }
}
