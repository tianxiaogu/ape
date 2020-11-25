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

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.commands.monkey.ape.naming.Name;
import com.android.commands.monkey.ape.tree.GUITreeNode;
import com.android.commands.monkey.ape.utils.Config;
import com.android.commands.monkey.ape.utils.PriorityObject;

import android.content.ComponentName;

/**
 * To keep logging simple, we make everything that is worth logging into an action.
 * We have four 
 * @author txgu
 *
 */
public class Action extends GraphElement implements PriorityObject {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static final Action NOP = new Action(ActionType.EVENT_NOP);
    public static final Action ACTIVATE = new Action(ActionType.EVENT_ACTIVATE);

    static {
        NOP.setThrottle(Config.getInteger("ape.nopActionThrottle", 1000));
    }

    private static Map<StartAction, StartAction> startActions = new HashMap<>();

    public static StartAction getStartAction(ActionType actionType, ComponentName activity) {
        StartAction key = new StartAction(actionType, activity);
        StartAction val = startActions.get(key);
        if (val == null) {
            val = key;
            startActions.put(key, val);
        }
        return val;
    }

    private final ActionType type;
    private boolean enabled = true;
    private boolean valid;
    private int priority;
    private int throttle;

    public Action(ActionType type) {
        this.type = type;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * The widget of a GUI action may be enabled or not. 
     * @param value
     */
    public void setEnabled(boolean value) {
        this.enabled = value;
    }

    /**
     * Is this action a scroll action?
     * @return
     */
    public boolean isScroll() {
        return this.type.isScroll();
    }

    /**
     * Is this action a fuzzing action.
     * @return
     */
    public boolean isFuzz() {
        return this.type == ActionType.FUZZ;
    }

    public ActionType getType() {
        return type;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * Used by random selection
     * @param prority
     */
    public void setPriority(int prority) {
        this.priority = prority;
    }

    public boolean isBack() {
        return this.type == ActionType.MODEL_BACK;
    }

    public boolean isClick() {
        return type == ActionType.MODEL_CLICK;
    }

    public boolean isNop() {
        return type == ActionType.EVENT_NOP;
    }

    public boolean canStartApp() {
        return type.canStartApp();
    }

    public boolean isModelAction() {
        return type.isModelAction();
    }

    public boolean isCrash() {
        return type == ActionType.PHANTOM_CRASH;
    }

    public boolean requireTarget() {
        return type.requireTarget();
    }

    /**
     * Wait interval after the action is performed.
     * @return
     */
    public int getThrottle() {
        return throttle;
    }

    public void setThrottle(int throttle) {
        this.throttle = throttle;
    }

    /**
     * Add extra throttle to the current throttle.
     * @param extra
     */
    public void appendThrottle(int extra) {
        this.throttle += extra;
    }

    /**
     * The widget of the GUI action
     * @return null if the action is not a GUI action
     */
    public Name getTarget() {
        return null;
    }

    /**
     * Get the GUI tree node of the resolved widget of the action.
     * @return null if the action is not a GUI action
     */
    public GUITreeNode getResolvedNode() {
        if (requireTarget()) {
            throw new IllegalStateException("Impossible!!!");
        }
        return null;
    }

    /**
     * This is used by a strategy.
     * @param value
     */
    public void setValid(boolean value) {
        this.valid = value;
    }

    public boolean isValid() {
        return this.valid;
    }

    public String toString() {
        if (!isModelAction()) {
            return type.toString();
        }
        return super.toString() + '@' + type.toString(); // + String.format("[V=%f]", value);
    }

    protected String resolvedInfo() {
        return String.format("[P=%d][T=%d][%s%s%s]", priority, throttle,  (isValid() ? "" : ", INVALID"),
                (isEnabled() ? "" : ", DISABLED"), (isVisited() ? "" : ", UNVISITED"));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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
        Action other = (Action) obj;
        if (type != other.type)
            return false;
        return true;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject jAction = new JSONObject();
        jAction.put("actionType", getType());
        jAction.put("throttle", getThrottle());
        return jAction;
    }

}
