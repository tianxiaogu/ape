package com.android.commands.monkey.ape;

import com.android.commands.monkey.ape.tree.GUITreeNode;

public interface NodeVisitor {

    void visit(GUITreeNode node);
}
