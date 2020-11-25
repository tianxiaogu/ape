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
