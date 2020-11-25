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
package com.android.commands.monkey.ape.model;

import static com.android.commands.monkey.ape.utils.Config.trivialActivityStateThreshold;
import static com.android.commands.monkey.ape.utils.Config.trivialActivityVisitThreshold;

import java.util.HashSet;
import java.util.Set;

public class ActivityNode extends GraphElement implements Comparable<ActivityNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;


    public final String activity;

    private Set<State> states = new HashSet<>();

    public ActivityNode(String activity) {
        this.activity = activity;
    }

    public void addState(State state) {
        this.states.add(state);
    }

    public boolean removeState(State state) {
        boolean removed = this.states.remove(state);
        return removed;
    }

    static String formatHeader() {
        return "FVT\tLVT\tVC\tSC\tName #FVT=first-visited-timestamp,LVT=last-visited-timestamp,VC=visited-count,SC=state-count";
    }

    public String toString() {
        return String.format("%s[%d,%d][%d][S=%d]", activity, this.getFirstVisitedTimestamp(),
                this.getLastVisitedTimestamp(), this.getVisitedCount(), states.size());
    }

    @Override
    public int compareTo(ActivityNode that) {
        int d = this.getVisitedCount() - that.getVisitedCount();
        if (d != 0) {
            return d;
        }
        d = this.getLastVisitedTimestamp() - that.getLastVisitedTimestamp();
        if (d != 0) {
            return d;
        }
        return activity.compareTo(that.activity);
    }

    public Set<State> getStates() {
        return states;
    }

    public boolean isTrivialActivity() {
        return this.states.size() < trivialActivityStateThreshold && getVisitedCount() < trivialActivityVisitThreshold;
    }

    public float getVisitedRate() {
        int totalAction = 0;
        int totalVisited = 0;
        for (State s : states) {
            for (ModelAction a : s.getActions()) {
                totalAction++;
                if (a.isVisited()) {
                    totalVisited++;
                }
            }
        }
        if (totalAction == 0) {
            return 0F;
        }
        return (float) totalVisited / totalAction;
    }
}
