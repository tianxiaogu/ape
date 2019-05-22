package com.android.commands.monkey.ape.naming;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.w3c.dom.Document;

import com.android.commands.monkey.ape.model.StateKey;
import com.android.commands.monkey.ape.tree.GUITree;
import com.android.commands.monkey.ape.tree.GUITreeBuilder;
import com.android.commands.monkey.ape.utils.Logger;
import com.android.commands.monkey.ape.utils.Utils;

import android.content.ComponentName;
/**
 * A new naming for each state created by an old naming.
 * @author txgu
 *
 */
public class StateNamingManager extends AbstractNamingManager implements Cloneable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final boolean debug = true;
    private Map<Naming, Map<StateKey, Naming>> namingToEdge = new HashMap<>();

    public StateNamingManager(NamingFactory nf) {
        super(nf);
    }

    public StateKey getStateKey(Naming naming, GUITree tree) {
        return GUITreeBuilder.getStateKey(naming, tree);
    }

    public Naming getNaming(GUITree tree, ComponentName activityName, Document document) {
        return getNaming(tree, activityName, document, false);
    }

    public Naming getNaming(GUITree tree, ComponentName activityName, Document document, boolean verbose) {
        Naming source = getBaseNaming();
        while (true) {
            StateKey state = getStateKey(source, tree);
            Naming target = Utils.getFromMapMap(namingToEdge, source, state);
            if (verbose) {
                Logger.iformat("getNaming: Source: %s, Target: %s, State: %s", source, target, state);
            }
            if (target == null) {
                return source;
            }
            source = target;
        }
    }

    public StateNamingManager clone() {
        StateNamingManager that = (StateNamingManager) super.clone();
        that.namingToEdge = new HashMap<>(this.namingToEdge);
        return that;
    }

    private void reportError(Naming oldOne, Naming newOne) {
        Logger.println("=== Dump conflict naming ===");
        Logger.println(" * Dump oldOne...");
        oldOne.dump();
        Logger.println("----------------------------");
        Logger.println(" * Dump newOne...");
        newOne.dump();
        Logger.println("============================");
        dump();
        throw new RuntimeException("Conflict: not implement yet!");
    }

    public void updateNaming(GUITree tree, ComponentName activityName, Document dom, Naming oldOne, Naming newOne) {
        updateNaming(tree, activityName, dom, oldOne, newOne, false);
    }
    public void updateNaming(GUITree tree, ComponentName activityName, Document dom, Naming oldOne, Naming newOne, boolean verbose) {
        if (oldOne == newOne) {
            return;
        }
        if (oldOne.isAncestor(newOne)) { // state refinement
            StateKey state = getStateKey(oldOne, tree);
            Naming check = Utils.addToMapMap(namingToEdge, oldOne, state, newOne);
            if (verbose) {
                Logger.iformat("updateNaming: Source: %s, Target: %s, State: %s", oldOne, newOne, state);
            }
            if (check != null && check != newOne) {
                Logger.println("=== Dump conflict naming ===");
                Logger.println(" * Dump oldOne...");
                oldOne.dump();
                Logger.println("----------------------------");
                Logger.println(" * Dump newOne...");
                newOne.dump();
                Logger.println("----------------------------");
                Logger.println(" * Dump check...");
                check.dump();
                Logger.println("============================");
                throw new RuntimeException("Conflict: not implement yet!");
            }
        } else if (newOne.isAncestor(oldOne)) { // state abstraction
            if (verbose) {
                Logger.iformat("updateNaming: Source: %s, Target: %s.", oldOne, newOne);
            }
            Naming parent = oldOne.getParent();
            while (parent != null) {
                StateKey state = getStateKey(parent, tree);
                Map<StateKey, Naming> edges = namingToEdge.get(parent);
                if (edges == null) {
                    throw new IllegalStateException("Parent Naming should be registered.");
                } else {
                    if (verbose) {
                        Logger.iformat("updateNaming: Parent: %s, State: %s, Leaf: %s", parent, state, oldOne);
                    }
                    edges.remove(state); // a previous refinement
                }
                if (parent == newOne) {
                    break;
                }
                parent = parent.getParent();
            }
            if (parent == null) {
                throw new IllegalStateException("");
            }
        } else if (newOne.getParent() == oldOne.getParent()) {
            updateNaming(tree, activityName, dom, oldOne, newOne.getParent());
            updateNaming(tree, activityName, dom, newOne.getParent(), newOne);
        } else {
            reportError(oldOne, newOne);
        }
        if (debug) {
            Naming check = getNaming(tree, activityName, dom);
            if (check != newOne) {
                getNaming(tree, activityName, dom, true);
                Logger.println("=== Dump conflict naming ===");
                Logger.println(" * Dump check...");
                check.dump();
                Logger.println("----------------------------");
                Logger.println(" * Dump newOne...");
                newOne.dump();
                Logger.println("============================");
                dump();
                throw new RuntimeException("Conflict: not implement yet!");
            }
        }
    }

    @Override
    public void dump() {
        for (Entry<Naming, Map<StateKey, Naming>> entry : this.namingToEdge.entrySet()) {
            Logger.format("%s: %d", entry.getKey(), entry.getValue().size());
            for (Entry<StateKey, Naming> e2 : entry.getValue().entrySet()) {
                Logger.format("    - %s %s", e2.getValue(), e2.getKey() );
            }
        }
    }
}
