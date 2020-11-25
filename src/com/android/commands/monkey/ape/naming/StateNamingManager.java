/*
 * Copyright 2020 Advanced Software Technologies Lab at ETH Zurich, Switzerland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    private static final boolean debug = false;
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
                Logger.iformat("getNaming for tree %s: Source: %s, Target: %s, State: %s", tree, source, target, state);
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
        updateNaming(tree, activityName, dom, oldOne, newOne, true);
    }
    public void updateNaming(GUITree tree, ComponentName activityName, Document dom, Naming oldOne, Naming newOne, boolean verbose) {
        if (oldOne == newOne) {
            return;
        }
        Naming existing = getNaming(tree, activityName, dom, true);
        if (existing == newOne) {
            return;
        }
        if (oldOne == newOne.getParent()) { // state refinement
            StateKey state = getStateKey(oldOne, tree);
            Naming check = Utils.addToMapMap(namingToEdge, oldOne, state, newOne);
            if (verbose) {
                Logger.iformat("updateNaming: Add - Source: %s, Target: %s, State: %s", oldOne, newOne, state);
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
                Logger.iformat("updateNaming: Remove - Source: %s, Target: %s.", oldOne, newOne);
            }
            Naming check = getNaming(tree, activityName, dom, true);
            Naming child = oldOne;
            Naming parent = child.getParent();
            while (parent != null) {
                StateKey state = getStateKey(parent, tree);
                Map<StateKey, Naming> edges = namingToEdge.get(parent);
                if (edges == null) {
                    throw new IllegalStateException("Parent Naming should be registered.");
                } else {
                    if (verbose) {
                        Logger.iformat("updateNaming: Parent: %s, State: %s, Child: %s", parent, state, oldOne);
                    }
                    Naming removed = edges.remove(state); // a previous refinement
                    if (removed == null) {
                        // has been updated by the updating of other trees
                        if (existing != newOne) {
                            throw new IllegalStateException();
                        }
                    } else if (removed != child) {
                        Logger.println("=== Dump conflict naming ===");
                        Logger.println(" * Dump check...");
                        check.dump();
                        Logger.println("----------------------------");
                        Logger.println(" * Dump oldOne...");
                        oldOne.dump();
                        Logger.println("----------------------------");
                        Logger.println(" * Dump newOne...");
                        newOne.dump();
                        Logger.println("----------------------------");
                        Logger.println(" * Dump parent...");
                        parent.dump();
                        Logger.println("----------------------------");
                        Logger.println(" * Dump child...");
                        child.dump();
                        Logger.println("----------------------------");
                        Logger.println(" * Dump removed...");
                        removed.dump();
                        Logger.println("============================");
                        throw new IllegalStateException("Removed should be the child.");
                    }
                }
                if (parent == newOne) {
                    break;
                }
                child = parent;
                parent = child.getParent();
            }
            if (parent == null) {
                throw new IllegalStateException("Parent should not be null.");
            }
        } else if (newOne.getParent() == oldOne.getParent()) {
            Naming parent = newOne.getParent();
            if (parent == null) {
                throw new IllegalStateException("Only root can have null parent.");
            }
            if (!isLeaf(newOne) || !isLeaf(oldOne)) {
                throw new IllegalStateException("Only leaf supports replacement.");
            }
            if (verbose) {
                Logger.iformat("updateNaming: Replace - Source: %s, Target: %s.", oldOne, newOne);
            }
            StateKey state = getStateKey(parent, tree);
            Map<StateKey, Naming> edges = namingToEdge.get(parent);
            if (edges == null) {
                throw new IllegalStateException("Parent Naming should be registered.");
            }
            edges.put(state, newOne);
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
                Logger.println("----------------------------");
                Logger.println(" * Dump oldOne...");
                oldOne.dump();
                Logger.println("============================");
                dump();
                throw new RuntimeException("Conflict: not implement yet!");
            }
        }
    }

    public boolean isLeaf(Naming naming) {
        Map<StateKey, Naming> edges = this.namingToEdge.get(naming);
        return edges == null || edges.isEmpty();
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
