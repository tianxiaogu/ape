package com.android.commands.monkey.ape.naming;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.android.commands.monkey.ape.model.StateKey;
import com.android.commands.monkey.ape.tree.GUITree;
import com.android.commands.monkey.ape.tree.GUITreeTransition;
import com.android.commands.monkey.ape.utils.Logger;

public class AssertSourceDivergent extends AbstractGUITreePredicate {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public AssertSourceDivergent(Naming updatedNaming, List<List<GUITreeTransition>> transitions) {
        super(updatedNaming, transitions);
    }

    @Override
    public boolean eval(NamingManager nm, Set<GUITree> affected, Naming naming) {
        Set<StateKey> states = new HashSet<>();
        for (List<GUITreeTransition> trees : transitions) {
            Iterator<GUITree> it = GUITreeTransition.sourceTreeIterator(trees);
            List<StateKey> temp = new ArrayList<>();
            while (it.hasNext()) {
                GUITree tree = it.next();
                StateKey tmp = getState(nm, affected, naming, tree);
                if (states.contains(tmp)) {
                    Logger.iformat("> eval %s: same state %s generated in different partitions", this, tmp);
                    return false;
                }
                temp.add(tmp);
            }
            states.addAll(temp);
        }
        return true;
    }

    @Override
    public Type getType() {
        return Predicate.Type.STATE_REFINEMENT;
    }
}
