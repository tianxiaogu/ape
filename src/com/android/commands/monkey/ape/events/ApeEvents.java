package com.android.commands.monkey.ape.events;

import org.json.JSONException;
import org.json.JSONObject;

public class ApeEvents {

    public static ApeEvent toApeEvent(JSONObject jEvent) throws JSONException {
        String type = jEvent.getString("type");
        if (type.equals(ApeClickEvent.class.getSimpleName())) {
            return ApeClickEvent.fromJSONObject(jEvent);
        }
        if (type.equals(ApeAppSwitchEvent.class.getSimpleName())) {
            return ApeAppSwitchEvent.fromJSONObject(jEvent);
        }
        if (type.equals(ApeDragEvent.class.getSimpleName())) {
            return ApeDragEvent.fromJSONObject(jEvent);
        }
        if (type.equals(ApePinchOrZoomEvent.class.getSimpleName())) {
            return ApePinchOrZoomEvent.fromJSONObject(jEvent);
        }
        if (type.equals(ApeKeyEvent.class.getSimpleName())) {
            return ApeKeyEvent.fromJSONObject(jEvent);
        }
        if (type.equals(ApeRotationEvent.class.getSimpleName())) {
            return ApeRotationEvent.fromJSONObject(jEvent);
        }
        if (type.equals(ApeTrackballEvent.class.getSimpleName())) {
            return ApeTrackballEvent.fromJSONObject(jEvent);
        }
        throw new IllegalArgumentException("Unknown event type: " + type);
    }
}
