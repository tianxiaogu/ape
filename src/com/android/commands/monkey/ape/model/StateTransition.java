package com.android.commands.monkey.ape.model;

import java.util.List;

import com.android.commands.monkey.ape.tree.GUITreeTransition;
import com.android.commands.monkey.ape.utils.Logger;
import com.android.commands.monkey.ape.utils.Utils;

public class StateTransition extends GraphElement {

    /**
     * 
     */
    private static final long serialVersionUID = -7293256167339096483L;

    public final State source;

    public final State target;

    public final ModelAction action;

    private StateTransitionVisitType type;

    private int hittingCount;
    private int missingCount;
    private double theta;

    private int throttle = Integer.MAX_VALUE;

    List<GUITreeTransition> treeTransitions;

    public StateTransition(State source, ModelAction action, State target) {
        if (!source.equals(action.getState())) {
            Logger.println("Source: " + source);
            Logger.println("Action: " + action);
            throw new IllegalStateException();
        }
        this.source = source;
        this.target = target;
        this.action = action;
    }

    public void updateThrottle(int throttle) {
        this.throttle = Math.min(throttle, this.throttle);
    }

    public int getTimestamp() {
        return this.getFirstVisitedTimestamp();
    }

    public void strengthen() {
        hittingCount++;
    }

    public void append(GUITreeTransition tt) {
        this.treeTransitions = Utils.addList(this.treeTransitions, tt);
        tt.setCurrentStateTransition(this);
    }

    public List<GUITreeTransition> getGUITreeTransitions() {
        return this.treeTransitions;
    }

    public GUITreeTransition getLastGUITreeTransition() {
        return treeTransitions.get(treeTransitions.size() - 1);
    }

    public void updateTheta(double v) {
        this.theta += v;
    }

    public double getTheta() {
        return theta;
    }

    public void weaken() {
        missingCount++;
    }

    public int getHittingCount() {
        return this.hittingCount;
    }

    public int getMissingCount() {
        return this.missingCount;
    }

    public int getStrength() {
        return hittingCount - missingCount;
    }

    public boolean isStrong() {
        if (missingCount == 0) {
            return this.hittingCount >= 1;
        }
        return getStrength() >= 2; // at least 1
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((action == null) ? 0 : action.hashCode());
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        result = prime * result + ((target == null) ? 0 : target.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StateTransition other = (StateTransition) obj;
        if (action == null) {
            if (other.action != null)
                return false;
        } else if (!action.equals(other.action))
            return false;
        if (source == null) {
            if (other.source != null)
                return false;
        } else if (!source.equals(other.source))
            return false;
        if (target == null) {
            if (other.target != null)
                return false;
        } else if (!target.equals(other.target))
            return false;
        return true;
    }

    public String toShortString() {
        return String.format("(%s,%s,%s)", source.getGraphId(), action.getGraphId(), target.getGraphId());
    }

    public boolean isCircle() {
        return this.target.equals(source);
    }

    public boolean isNonTrivialCircle() {
        return isCircle() && getTheta() != 0;
    }

    public boolean isNonDeterministic() {
        return false;
    }

    public boolean isBackEdge() {
        int sourceFV = this.source.firstVisitTimestamp;
        int targetFV = this.target.firstVisitTimestamp;
        return sourceFV > targetFV;
    }

    public boolean isSameActivity() {
        return this.target.getActivity().equals(source.getActivity());
    }

    public String toString() {
        return String.format("%s@[H(%d),M(%d),T(%f)] %s =[%s]=> %s", super.toString(), hittingCount, missingCount,
                theta, source.getGraphId(), action.getGraphId(), target.getGraphId());
    }

    public State getSource() {
        return source;
    }

    public State getTarget() {
        return target;
    }

    public ModelAction getAction() {
        return action;
    }

    public StateTransitionVisitType getType() {
        return type;
    }

    public void setType(StateTransitionVisitType type) {
        this.type = type;
    }

    public int getThrottle() {
        return throttle;
    }
}