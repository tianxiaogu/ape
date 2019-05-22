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
