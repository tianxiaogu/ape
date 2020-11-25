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
