package com.android.commands.monkey.ape.events;

import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.commands.monkey.MonkeyEvent;
import com.android.commands.monkey.MonkeyKeyEvent;

import android.view.KeyEvent;

public class ApeKeyEvent extends AbstractApeEvent {

    /**
     * 
     */
    private static final long serialVersionUID = -672478591540471589L;
    final int keyCode;

    public ApeKeyEvent(int keyCode) {
        this.keyCode = keyCode;
    }

    @Override
    public List<MonkeyEvent> generateMonkeyEvents() {
        MonkeyEvent down = new MonkeyKeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        MonkeyEvent up = new MonkeyKeyEvent(KeyEvent.ACTION_UP, keyCode);
        return Arrays.asList(down, up);
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject jEvent = super.toJSONObject();
        jEvent.put("key", keyCode);
        return jEvent;
    }

    public static ApeEvent fromJSONObject(JSONObject jEvent) throws JSONException {
        int key = jEvent.getInt("key");
        return new ApeKeyEvent(key);
    }
}
