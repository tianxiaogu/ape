package com.android.commands.monkey.ape.tree;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import com.android.commands.monkey.ape.model.StateTransition;

public class GUITreeTransition implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private final GUITree source;
    private final GUITree target;
    private final GUITreeAction action;

    private StateTransition stateTransition;

    private int throttle;

    public GUITreeTransition(GUITree source, GUITreeAction action, GUITree target) {
        this.source = source;
        this.target = target;
        this.action = action;
    }

    public GUITree getSource() {
        return source;
    }

    public GUITree getTarget() {
        return target;
    }

    public GUITreeAction getAction() {
        return action;
    }

    public int getTimestamp() {
        return source.getTimestamp();
    }

    public void setCurrentStateTransition(StateTransition stateTransition) {
        this.stateTransition = stateTransition;
    }

    public StateTransition getCurrentStateTransition() {
        return this.stateTransition;
    }

    private static abstract class GUITreeTransitionIterator<V> implements Iterator<V> {

        protected Iterator<GUITreeTransition> transitionIterator;

        public GUITreeTransitionIterator(Iterator<GUITreeTransition> transitionIterator) {
            this.transitionIterator = transitionIterator;
        }

        @Override
        public boolean hasNext() {
            return transitionIterator.hasNext();
        }

        GUITreeTransition advance() {
            return transitionIterator.next();
        }
    }

    public static Iterator<GUITree> sourceTreeIterator(List<GUITreeTransition> transitions) {
        return new GUITreeTransitionIterator<GUITree>(transitions.iterator()) {
            @Override
            public GUITree next() {
                return advance().getSource();
            }
        };
    }

    public static Iterator<GUITree> targetTreeIterator(List<GUITreeTransition> transitions) {
        return new GUITreeTransitionIterator<GUITree>(transitions.iterator()) {
            @Override
            public GUITree next() {
                return advance().getTarget();
            }
        };
    }

    public static Iterator<GUITreeAction> actionIterator(List<GUITreeTransition> transitions) {
        return new GUITreeTransitionIterator<GUITreeAction>(transitions.iterator()) {
            @Override
            public GUITreeAction next() {
                return advance().getAction();
            }
        };
    }

    public int getThrottle() {
        return throttle;
    }

    public void setThrottle(int throttle) {
        this.throttle = throttle;
    }
}
