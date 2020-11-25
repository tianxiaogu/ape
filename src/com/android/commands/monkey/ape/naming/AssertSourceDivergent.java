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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.android.commands.monkey.ape.model.StateKey;
import com.android.commands.monkey.ape.tree.GUITree;
import com.android.commands.monkey.ape.tree.GUITreeTransition;
import com.android.commands.monkey.ape.utils.Logger;

public class AssertSourceDivergent extends AbstractGUITreePredicate {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public AssertSourceDivergent(Naming updatedNaming, List<List<GUITreeTransition>> transitions) {
        super(updatedNaming, transitions);
    }

    @Override
    public boolean eval(NamingManager nm, Set<GUITree> affected, Naming naming) {
        Set<StateKey> states = new HashSet<>();
        for (List<GUITreeTransition> trees : transitions) {
            Iterator<GUITree> it = GUITreeTransition.sourceTreeIterator(trees);
            List<StateKey> temp = new ArrayList<>();
            while (it.hasNext()) {
                GUITree tree = it.next();
                StateKey tmp = getState(nm, affected, naming, tree);
                if (states.contains(tmp)) {
                    Logger.iformat("> eval %s: same state %s generated in different partitions", this, tmp);
                    return false;
                }
                temp.add(tmp);
            }
            states.addAll(temp);
        }
        return true;
    }

    @Override
    public Type getType() {
        return Predicate.Type.STATE_REFINEMENT;
    }
}
