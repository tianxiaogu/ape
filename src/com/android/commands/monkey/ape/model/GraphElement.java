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
