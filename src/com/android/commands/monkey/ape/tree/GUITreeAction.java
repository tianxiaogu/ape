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
