package com.android.commands.monkey.ape.events;

import java.util.Collections;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.commands.monkey.MonkeyEvent;
import com.android.commands.monkey.MonkeyRotationEvent;

public class ApeRotationEvent extends AbstractApeEvent {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public final int rotationDegree;
    public final boolean persist;

    public ApeRotationEvent(int degree, boolean persist) {
        this.rotationDegree = degree;
        this.persist = persist;
    }

    @Override
    public List<MonkeyEvent> generateMonkeyEvents() {
        return Collections.<MonkeyEvent>singletonList(new MonkeyRotationEvent(rotationDegree, persist));
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject jEvent = super.toJSONObject();
        jEvent.put("degree", rotationDegree);
        jEvent.put("persist", persist);
        return jEvent;
    }

    public static ApeEvent fromJSONObject(JSONObject jEvent) throws JSONException {
        boolean persist = jEvent.getBoolean("persist");
        int degree = jEvent.getInt("degree");
        return new ApeRotationEvent(degree, persist);
    }
}
