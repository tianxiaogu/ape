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

import java.io.Serializable;

public class GraphElement implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    protected int firstVisitTimestamp = -1;
    protected int lastVisitTimestamp;
    protected int visitedCount;

    protected String id = null;

    public String getGraphId() {
        if (id == null) {
            return "";
        }
        return id;
    }

    public String toString() {
        return "" + getGraphId() + '[' + firstVisitTimestamp + ',' + lastVisitTimestamp + "][" + visitedCount + ']';
    }

    void setGraphId(String id) {
        this.id = id;
    }

    public boolean isUnvisited() {
        return firstVisitTimestamp == -1;
    }

    public final boolean isVisited() {
        return !isUnvisited();
    }

    public void visitedAt(int timestamp) {
        lastVisitTimestamp = timestamp;
        if (firstVisitTimestamp == -1) {
            firstVisitTimestamp = timestamp;
            if (firstVisitTimestamp == -1) {
                throw new IllegalStateException("Invalid timestamp " + timestamp);
            }
        }
        visitedCount++;
    }

    public int getFirstVisitedTimestamp() {
        return this.firstVisitTimestamp;
    }

    public int getLastVisitedTimestamp() {
        return this.lastVisitTimestamp;
    }

    public int getVisitedCount() {
        return this.visitedCount;
    }
}
