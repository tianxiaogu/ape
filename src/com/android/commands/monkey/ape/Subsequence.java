package com.android.commands.monkey.ape;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.android.commands.monkey.ape.model.ModelAction;
import com.android.commands.monkey.ape.model.StateTransition;
import com.android.commands.monkey.ape.model.State;
import com.android.commands.monkey.ape.utils.Logger;

public class Subsequence implements Cloneable {

    private StateTransition[] edges;
    private int hash;
    private State start;

    private boolean closed;

    public boolean isClosed() {
        return this.closed;
    }

    public void close() {
        this.closed = true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        if (hash == 0) {
            int result = 1;
            result = prime * result + Arrays.hashCode(edges);
            result = prime * result + ((start == null) ? 0 : start.hashCode());
            hash = result;
            return result;
        }
        return hash;
    }

    public StateTransition[] getEdges() {
        return edges;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Subsequence other = (Subsequence) obj;
        if (!Arrays.equals(edges, other.edges))
            return false;
        if (start == null) {
            if (other.start != null)
                return false;
        } else if (!start.equals(other.start))
            return false;
        return true;
    }

    static StateTransition[] EMPTY = new StateTransition[0];

    public Subsequence(State start) {
        this.start = start;
        this.edges = EMPTY;
    }

    public Subsequence(Subsequence seq, StateTransition edge) {
        this.start = seq.start;
        int size = seq.size();
        StateTransition[] newStateTransitions = new StateTransition[size + 1];
        this.edges = newStateTransitions;
        System.arraycopy(seq.edges, 0, this.edges, 0, size);
        newStateTransitions[size] = edge;
    }

    public Subsequence(StateTransition edge) {
        this.start = edge.source;
        this.edges = new StateTransition[] { edge };
    }

    public Subsequence(List<StateTransition> edges) {
        this.start = edges.get(0).source;
        int size = edges.size();
        this.edges = new StateTransition[size];
        this.edges = edges.toArray(this.edges);
    }

    public Subsequence(Subsequence seq, StateTransition edge1, StateTransition edge2) {
        this.start = seq.start;
        int size = seq.size();
        StateTransition[] newStateTransitions = new StateTransition[size + 2];
        this.edges = newStateTransitions;
        System.arraycopy(seq.edges, 0, this.edges, 0, size);
        newStateTransitions[size++] = edge1;
        newStateTransitions[size] = edge2;
    }

    public State getLastState() {
        if (edges.length == 0) {
            throw new IllegalStateException("Empty subsequence");
        }
        return edges[edges.length - 1].target;
    }

    public int size() {
        return edges.length;
    }

    public State getStartState() {
        return start;
    }

    public void fillBuffer(LinkedList<StateTransition> actionBuffer) {
        Logger.iprintln("Fill buffer..");
        for (int i = 0; i < edges.length; i++) {
            Logger.iformat(" %3d %s", i, edges[i]);
            actionBuffer.add(edges[i]);
        }
    }

    public ModelAction getFirstAction() {
        if (edges.length == 0) {
            throw new IllegalStateException("Empty subsequence");
        }
        return edges[0].action;
    }

    public boolean isEmpty() {
        return edges.length == 0;
    }

    public StateTransition getLastStateTransition() {
        if (edges.length == 0) {
            throw new IllegalStateException("Empty subsequence");
        }
        return (edges)[edges.length - 1];
    }

    public ModelAction getLastAction() {
        if (edges.length == 0) {
            throw new IllegalStateException("Empty subsequence");
        }
        return (edges)[edges.length - 1].action;
    }

    public boolean contains(State target) {
        for (StateTransition edge : edges) {
            if (edge.target.equals(target)) {
                return true;
            }
        }
        return false;
    }

    public State getLastLastState() {
        if (edges.length == 0) {
            throw new IllegalStateException("Empty subsequence");
        }
        if (edges.length == 1) {
            return start;
        }
        return edges[edges.length - 2].target;
    }

    public boolean contains(ModelAction target) {
        for (StateTransition edge : edges) {
            if (edge.action.equals(target)) {
                return true;
            }
        }
        return false;
    }

    public void print() {
        for (int i = 0, k = 1; i < edges.length; i++, k++) {
            StateTransition edge = edges[i];
            Logger.format("%3d %s", k, edge.toShortString());
        }
    }
}
