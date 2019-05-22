package com.android.commands.monkey.ape.naming;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.android.commands.monkey.ape.model.StateKey;
import com.android.commands.monkey.ape.tree.GUITree;
import com.android.commands.monkey.ape.utils.Logger;

public class AssertStatesDivergent extends AbstractPredicate {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    List<GUITree> trees;
    public AssertStatesDivergent(Naming updatedNaming, List<GUITree> trees) {
        super(updatedNaming);
        this.trees = trees;
    }

    @Override
    public boolean eval(NamingManager nm, Set<GUITree> affected, Naming naming) {
        Set<StateKey> states = new HashSet<>();
        for (GUITree tree : trees) {
            StateKey tmp = getState(nm, affected, naming, tree);
            if (states.contains(tmp)) {
                Logger.iformat("> eval %s: same state %s generated in different partitions", this, tmp);
                return false;
            }
        }
        return true;
    }

    @Override
    public Type getType() {
        return Predicate.Type.STATE_REFINEMENT;
    }
}
