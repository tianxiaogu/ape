package com.android.commands.monkey.ape.naming;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;

import com.android.commands.monkey.ape.model.Model;
import com.android.commands.monkey.ape.model.ModelAction;
import com.android.commands.monkey.ape.model.State;
import com.android.commands.monkey.ape.model.StateKey;
import com.android.commands.monkey.ape.model.StateTransition;
import com.android.commands.monkey.ape.tree.GUITree;

import android.content.ComponentName;

public interface NamingManager extends Serializable {

    /**
     * Debug
     */
    void dump();

    /**
     * NamingManager is evolving during testing. The version will increment if some naming has been updated.
     * @return
     */
    int getVersion();

    /**
     * Get the current naming manager of the GUI tree
     * If there is no such a GUI tree, try to get the naming manager using its document
     * @param tree
     * @return
     */
    Naming getNaming(GUITree tree);

    /**
     * A NamingManager may need the activity name and the document tree to
     * decide the Naming
     * 
     * @param activityName
     * @param tree
     * @return
     */
    Naming getNaming(GUITree tree, ComponentName activityName, Document document);

    /**
     * A NamingManager may need the activity name and the document tree to
     * decide the Naming
     * 
     * @param activityName
     * @param tree
     * @param existing
     * @param newOne
     */
    void updateNaming(GUITree tree, ComponentName activityName, Document dom, Naming existing, Naming newOne);

    void updateNaming(GUITree tree, Naming newOne);

    /**
     * Used for sanity check only
     * @param trees
     * @return
     */
    Map<GUITree, Naming> sync(Iterator<GUITree> trees);

    /**
     * Do the state abstraction for the given tree
     * @param naming
     * @param tree
     * @return
     */
    StateKey getStateKey(Naming naming, GUITree tree);

    /**
     * the default abstraction function
     * @return
     */
    Naming getBaseNaming();

    /**
     * the finest abstraction function
     * @return
     */
    Naming getTopNaming();

    /**
     * the coarsest abstraction function
     * @return
     */
    Naming getBottomNaming();

    Model resolveNonDeterminism(Model model, StateTransition st);

    Model actionRefinement(Model model, ModelAction action);

    Model stateAbstraction(Model model, Naming naming, State target, Naming parentNaming, Set<State> states);

    void release(GUITree removed);

}
