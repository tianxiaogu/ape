package com.android.commands.monkey.ape.naming;

import java.util.Set;

import com.android.commands.monkey.ape.tree.GUITree;

public interface Predicate extends Comparable<Predicate> {
    
    static enum Type { STATE_ABSTRACTION, STATE_REFINEMENT, ACTION_REFINEMENT };

    boolean eval(NamingManager nm, Set<GUITree> affected, Naming naming);
    Naming getUpdatedNaming();
    Type getType();
}
