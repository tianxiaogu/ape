package com.android.commands.monkey.ape.naming;

import org.w3c.dom.Document;

import com.android.commands.monkey.ape.tree.GUITree;

import android.content.ComponentName;

/**
 * A single naming for the all activities/GUI trees.
 * @author txgu
 *
 */
public class MonolithicNamingManager extends AbstractNamingManager {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private Naming current;

    public MonolithicNamingManager(NamingFactory nf) {
        super(nf);
        this.current = getBaseNaming();
    }

    @Override
    public Naming getNaming(GUITree tree, ComponentName activityName, Document document) {
        return current;
    }

    @Override
    public void updateNaming(GUITree tree, ComponentName activityName, Document dom, Naming oldOne, Naming newOne) {
        if (checkReplace(current, oldOne, newOne)) {
            current = newOne;
        }
    }

    @Override
    public void dump() {
        current.dump();
    }

}
