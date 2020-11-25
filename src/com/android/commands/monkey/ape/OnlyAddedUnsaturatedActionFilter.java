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
package com.android.commands.monkey.ape;

import com.android.commands.monkey.ape.model.ModelAction;
import com.android.commands.monkey.ape.model.State;
import com.android.commands.monkey.ape.tree.GUITreeWidgetDiffer;

public class OnlyAddedUnsaturatedActionFilter extends BaseActionFilter {

    GUITreeWidgetDiffer differ = new GUITreeWidgetDiffer();

    public int diff(State s1, State s2) {
        return differ.diff(s1, s2);
    }

    @Override
    public boolean include(ModelAction action) {
        if (action.isSaturated()) {
            return false;
        }
        if (!action.isValid()) {
            return false;
        }
        if (!action.requireTarget()) {
            return false; // Include BACK
        }
        if (!differ.hasAdded()) {
            return true; // No added, include all
        }
        return differ.isAdded(action.getTarget());
    }

    public boolean hasAdded() {
        return differ.hasAdded();
    }

    public void print() {
        differ.print();
    }

}
