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
