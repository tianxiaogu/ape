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

import org.json.JSONException;
import org.json.JSONObject;

import com.android.commands.monkey.ape.utils.Logger;

public class Crash implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public final String processName;
    public final int pid;
    public final String shortMsg;
    public final String longMsg;
    public final long timeMillis;
    public final String stackTrace;

    public Crash(String processName, int pid, String shortMsg, String longMsg, long timeMillis, String stackTrace) {
        this.processName = processName;
        this.pid = pid;
        this.shortMsg = shortMsg;
        this.longMsg = longMsg;
        this.timeMillis = timeMillis;
        this.stackTrace = stackTrace;
    }

    public String toString() {
        return "" + processName + "@" + timeMillis + "@" + shortMsg + "@" + longMsg;
    }

    public void print() {
        Logger.println("// CRASH: " + processName + " (pid " + pid + ") (elapsed nanos: "
                + timeMillis + ")");
        Logger.println("// Short Msg: " + shortMsg);
        Logger.println("// Long Msg: " + longMsg);
        Logger.println("// " + stackTrace.replace("\n", "\n" + Logger.TAG + "// "));
    }
    
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jAction = new JSONObject();
        jAction.put("processName", this.processName);
        jAction.put("pid", this.pid);
        jAction.put("shortMsg", this.shortMsg);
        jAction.put("longMsg", this.longMsg);
        jAction.put("timeMillis", this.timeMillis);
        jAction.put("stackTrace", this.stackTrace);
        return jAction;
    }
}
