package com.android.commands.monkey.ape.tree;

import java.io.Serializable;

import com.android.commands.monkey.ape.model.ModelAction;
import com.android.commands.monkey.ape.model.ActionType;

public class GUITreeAction implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -8585007298035419054L;
    private GUITree tree; // concrete state
    private GUITreeNode node; // concrete
    private ModelAction action;

    private int throttle;

    public GUITreeAction(GUITree tree, GUITreeNode node, ModelAction action) {
        this.tree = tree;
        this.node = node;
        this.action = action;
    }

    public ModelAction getModelAction() {
        return action;
    }

    public ActionType getActionType() {
        return action.getType();
    }

    public GUITreeNode getGUITreeNode() {
        return node;
    }

    public GUITree getGUITree() {
        return tree;
    }

    public int getThrotlle() {
        return throttle;
    }

    public void setThrottle(int throttle) {
        this.throttle = throttle;
    }

    public void rebuild(ModelAction action) {
        this.action = action;
    }
}
