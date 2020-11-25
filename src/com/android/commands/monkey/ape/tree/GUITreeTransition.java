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
