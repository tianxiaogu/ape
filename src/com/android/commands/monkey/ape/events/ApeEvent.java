package com.android.commands.monkey.ape.events;

import java.io.Serializable;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.commands.monkey.MonkeyEvent;

public interface ApeEvent extends Serializable {
    List<MonkeyEvent> generateMonkeyEvents();
    JSONObject toJSONObject() throws JSONException;
}
