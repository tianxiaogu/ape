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
package com.android.commands.monkey.ape.model.xpathaction;

import java.util.List;

public class XPathActionSequence {

    final double probability;
    final List<XPathAction> actions;

    public XPathActionSequence(double probability, List<XPathAction> actions) {
        this.probability = probability;
        this.actions = actions;
    }

    public double getProbability() {
        return probability;
    }

    public List<XPathAction> getActions() {
        return actions;
    }

    public boolean isEmpty() {
        return actions.isEmpty();
    }

    public XPathAction get(int i) {
        return actions.get(i);
    }

    public int size() {
        return actions.size();
    }
}
