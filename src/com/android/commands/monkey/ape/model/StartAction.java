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

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ComponentName;

public class StartAction extends Action {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public final String packageName;
    public final String className;

    public StartAction(ActionType type, ComponentName activity) {
        super(type);
        this.packageName = activity.getPackageName();
        this.className = activity.getClassName();
    }

    public StartAction(ActionType type, String packageName, String className) {
        super(type);
        this.packageName = packageName;
        this.className = className;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((className == null) ? 0 : className.hashCode());
        result = prime * result + ((packageName == null) ? 0 : packageName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        StartAction other = (StartAction) obj;
        if (className == null) {
            if (other.className != null)
                return false;
        } else if (!className.equals(other.className))
            return false;
        if (packageName == null) {
            if (other.packageName != null)
                return false;
        } else if (!packageName.equals(other.packageName))
            return false;
        return true;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject jAction = super.toJSONObject();
        jAction.put("packageName", packageName);
        jAction.put("className", className);
        return jAction;
    }

    public static Action fromJSON(JSONObject jAction) throws JSONException {
        ActionType actionType = ActionType.valueOf(jAction.getString("actionType"));
        String packageName = jAction.getString("packageName");
        String className = jAction.getString("className");
        Action action = new StartAction(actionType, packageName, className);
        int throttle = jAction.getInt("throttle");
        action.setThrottle(throttle);
        return action;
    }

    public String toString() {
        return super.toString() + "@" + this.className;
    }

    public ComponentName getActivity() {
        return new ComponentName(packageName, className);
    }
}
