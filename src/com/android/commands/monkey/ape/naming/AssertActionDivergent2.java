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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.android.commands.monkey.ape.tree.GUITree;
import com.android.commands.monkey.ape.tree.GUITreeNode;
import com.android.commands.monkey.ape.utils.Logger;

public class AssertActionDivergent2 extends AbstractPredicate {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private GUITree tree;
    private List<List<GUITreeNode>> nodes;

    public AssertActionDivergent2(Naming updatedNaming, GUITree tree, List<List<GUITreeNode>> nodes) {
        super(updatedNaming);
        this.tree = tree;
        this.nodes = nodes;
    }

    @Override
    public boolean eval(NamingManager nm, Set<GUITree> affected, Naming naming) {
        Set<Name> actions = new HashSet<>();
        for (List<GUITreeNode> partition : nodes) {
            Set<Name> temp = new HashSet<>();
            for (GUITreeNode node : partition) {
                Name tmp = getName(nm, affected, naming, tree, node);
                if (temp.add(tmp)) { // new action in this partition
                    if (!actions.add(tmp)) { // already added by another partition
                        Logger.iformat("> eval %s: same action %s generated in different partitions", this, tmp);
                        return false; // reject
                    }
                }
            }
        }
        return true;
    }

    @Override
    public Type getType() {
        return Predicate.Type.ACTION_REFINEMENT;
    }
}
