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

import com.android.commands.monkey.ape.tree.GUITree;
import com.android.commands.monkey.ape.utils.Logger;

import android.content.ComponentName;

/**
 * A naming for each activity.
 * @author txgu
 *
 */
public class ActivityNamingManager extends AbstractNamingManager {

    /**
     * 
     */
    private static final long serialVersionUID = -504890234943918996L;

    private Map<ComponentName, Naming> activityToNamingManager = new HashMap<>();

    public ActivityNamingManager(NamingFactory nf) {
        super(nf);
    }

    @Override
    public Naming getNaming(GUITree tree, ComponentName activityName, Document document) {
        if (activityName == null) {
            throw new NullPointerException("Activity name is null.");
        }
        Naming val = activityToNamingManager.get(activityName);
        if (val == null) {
            val = getBaseNaming();
            activityToNamingManager.put(activityName, val);
            return val;
        }
        return val;
    }

    @Override
    public void updateNaming(GUITree tree, ComponentName activityName, Document dom, Naming oldOne, Naming newOne) {
        if (activityName == null) {
            throw new NullPointerException("Activity name is null.");
        }
        // Naming existing = activityToNamingManager.get(activityName);
        // if (checkReplace(existing, oldOne, newOne)) {
        activityToNamingManager.put(activityName, newOne);
        // }
    }

    @Override
    public void dump() {
        for (Entry<ComponentName, Naming> entry : activityToNamingManager.entrySet()) {
            Logger.println("Activity: " + entry.getKey());
            entry.getValue().dump();
        }
    }

}
