package com.android.commands.monkey.ape.naming;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.android.commands.monkey.ape.tree.GUITree;
import com.android.commands.monkey.ape.tree.GUITreeNode;
import com.android.commands.monkey.ape.utils.Logger;

public class AssertActionDivergent2 extends AbstractPredicate {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private GUITree tree;
    private List<List<GUITreeNode>> nodes;

    public AssertActionDivergent2(Naming updatedNaming, GUITree tree, List<List<GUITreeNode>> nodes) {
        super(updatedNaming);
        this.tree = tree;
        this.nodes = nodes;
    }

    @Override
    public boolean eval(NamingManager nm, Set<GUITree> affected, Naming naming) {
        Set<Name> actions = new HashSet<>();
        for (List<GUITreeNode> partition : nodes) {
            Set<Name> temp = new HashSet<>();
            for (GUITreeNode node : partition) {
                Name tmp = getName(nm, affected, naming, tree, node);
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
