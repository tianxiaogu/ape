package com.android.commands.monkey.ape.naming;

import java.io.Serializable;
import java.util.Set;

import com.android.commands.monkey.ape.model.StateKey;
import com.android.commands.monkey.ape.tree.GUITree;
import com.android.commands.monkey.ape.tree.GUITreeBuilder;
import com.android.commands.monkey.ape.tree.GUITreeNode;
import com.android.commands.monkey.ape.utils.Logger;

public abstract class AbstractPredicate implements Predicate, Comparable<Predicate>, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    Naming updatedNaming;
    public AbstractPredicate(Naming updatedNaming) {
        this.updatedNaming = updatedNaming;
    }

    public int compareTo(Predicate p) {
        int ret = getType().compareTo(p.getType());
        if (ret != 0) {
            return ret;
        }
        return 0;
    }

    public Naming getUpdatedNaming() {
        return this.updatedNaming;
    }

    public String toString() {
        return "" + getClass().getSimpleName() + "[" + updatedNaming + "]";
    }

    public final StateKey getState(NamingManager nm, Set<GUITree> affected, Naming naming, GUITree tree) {
        StateKey state;
        if (affected.contains(tree)) {
            state = GUITreeBuilder.getStateKey(naming, tree);
        } else {
            Naming n = nm.getNaming(tree);
            if (n != tree.getCurrentNaming()) {
                throw new IllegalStateException("Sanity check failed!");
            }
            state = tree.getCurrentState().getStateKey();
        }
        return state;
    }

    public final Name getName(NamingManager nm, Set<GUITree> affected, Naming naming, GUITree tree, GUITreeNode node) {
        Name tmp;
        if (affected.contains(tree)) {
            tmp = GUITreeBuilder.getNodeName(naming, tree, node); // naming.getName(tree.getDocument(), node.getDomNode());
        } else {
            Naming n = nm.getNaming(tree);
            if (n != tree.getCurrentNaming()) {
                Logger.wprintln("Error of tree #" + tree.getTimestamp());
                for (GUITree t : affected) {
                    Logger.wprintln("Affected tree #" + t.getTimestamp());
                }
                if (n != null) {
                    n.dump();
                } else {
                    Logger.wprintln("n is null");
                }
                if (tree.getCurrentNaming() != null) {
                    tree.getCurrentNaming().dump();;
                } else {
                    Logger.wprintln("current naming is null");
                }
                throw new IllegalStateException("Sanity check failed!");
            }
            tmp = GUITreeBuilder.getNodeName(n, tree, node);
        }
        return tmp;
    }
}
