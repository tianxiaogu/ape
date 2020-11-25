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

public class CrashAction extends Action {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public final Crash crash;

    public CrashAction(Crash crash) {
        super(ActionType.PHANTOM_CRASH);
        this.crash = crash;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject jAction = super.toJSONObject();
        jAction.put("crash", this.crash.toJSONObject());
        return jAction;
    }
}
