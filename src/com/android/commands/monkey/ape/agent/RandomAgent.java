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
package com.android.commands.monkey.ape.agent;

import com.android.commands.monkey.MonkeySourceApe;
import com.android.commands.monkey.ape.Subsequence;
import com.android.commands.monkey.ape.model.Action;
import com.android.commands.monkey.ape.model.Graph;
import com.android.commands.monkey.ape.model.State;

import android.content.ComponentName;

public class RandomAgent extends StatefulAgent {

    public RandomAgent(MonkeySourceApe ape, Graph graph) {
        super(ape, graph);
    }

    @Override
    public void onActivityBlocked(ComponentName blockedActivity) {

    }

    @Override
    public boolean onGraphStable(int counter) {
        return false;
    }

    @Override
    public boolean onStateStable(int counter) {
        return false;

    }

    @Override
    public boolean onVoidGUITree(int counter) {
        return false;
    }

    @Override
    public void onBufferLoss(State actual, State expected) {
    }

    @Override
    public void onRefillBuffer(Subsequence seq) {
    }

    @Override
    protected Action selectNewActionNonnull() {
        Action action = null;
        action = validateNewAction(selectNewActionFromBuffer());
        if (action != null) {
            return action;
        }
        action = validateNewAction(this.selectNewActionRandomly());
        if (action == null) {
            return handleNullAction();
        }
        return action;
    }

    @Override
    public String getLoggerName() {
        return "Random";
    }

    @Override
    public void onBadState(int lastBadStateCount, int badStateCounter) {
        // TODO Auto-generated method stub

    }
}
