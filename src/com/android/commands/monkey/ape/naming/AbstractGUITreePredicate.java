package com.android.commands.monkey.ape.naming;

import java.util.List;

import com.android.commands.monkey.ape.tree.GUITreeTransition;

public abstract class AbstractGUITreePredicate extends AbstractPredicate {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    List<List<GUITreeTransition>> transitions;

    public AbstractGUITreePredicate(Naming updatedNaming, List<List<GUITreeTransition>> transitions) {
        super(updatedNaming);
        this.transitions = transitions;
    }

}
