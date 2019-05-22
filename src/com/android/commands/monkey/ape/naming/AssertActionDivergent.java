package com.android.commands.monkey.ape.naming;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.android.commands.monkey.ape.tree.GUITree;
import com.android.commands.monkey.ape.tree.GUITreeNode;
import com.android.commands.monkey.ape.tree.GUITreeTransition;
import com.android.commands.monkey.ape.utils.Logger;

public class AssertActionDivergent extends AbstractGUITreePredicate {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public AssertActionDivergent(Naming updatedNaming, List<List<GUITreeTransition>> transitions) {
        super(updatedNaming, transitions);
    }

    @Override
    public boolean eval(NamingManager nm, Set<GUITree> affected, final Naming naming) {
        Set<Name> actions = new HashSet<>();
        for (List<GUITreeTransition> trees : transitions) {
            Set<Name> temp = new HashSet<>();
            for (GUITreeTransition tt :  trees) {
                GUITree source = tt.getSource();
                GUITreeNode node = tt.getAction().getGUITreeNode();
                Name tmp = getName(nm, affected, naming, source, node);
                if (temp.add(tmp)) { // new action in this partition
                    if (!actions.add(tmp)) { // already added by another partition
                        Logger.iformat("> eval %s: same action %s generated in different partitions", this, tmp);
                        return false; // reject
                    }
                }
            }
        }
        return true;
    }

    @Override
    public Type getType() {
        return Predicate.Type.ACTION_REFINEMENT;
    }

}
