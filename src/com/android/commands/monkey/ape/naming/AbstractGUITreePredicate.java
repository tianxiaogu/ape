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

import java.util.List;

import com.android.commands.monkey.ape.tree.GUITreeTransition;

public abstract class AbstractGUITreePredicate extends AbstractPredicate {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    List<List<GUITreeTransition>> transitions;

    public AbstractGUITreePredicate(Naming updatedNaming, List<List<GUITreeTransition>> transitions) {
        super(updatedNaming);
        this.transitions = transitions;
    }

}
