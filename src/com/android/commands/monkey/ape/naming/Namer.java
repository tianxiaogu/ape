package com.android.commands.monkey.ape.naming;

import java.util.EnumSet;

import com.android.commands.monkey.ape.tree.GUITreeNode;

/**
 * Attribute path reducer
 * @author txgu
 *
 */
public interface Namer {
    EnumSet<NamerType> getNamerTypes();
    Name naming(GUITreeNode node);
    boolean refinesTo(Namer namer);
}
