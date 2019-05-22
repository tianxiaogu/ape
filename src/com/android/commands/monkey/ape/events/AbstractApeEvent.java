package com.android.commands.monkey.ape.events;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.PointF;

public abstract class AbstractApeEvent implements ApeEvent {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public JSONObject toJSONObject() throws JSONException {
        JSONObject jEvent = new JSONObject();
        jEvent.put("type", getClass().getSimpleName());
        return jEvent;
    }

    static float[] fromPointsArray(PointF[] points) {
        float[] results = new float[points.length << 1];
        int index = 0;
        for (PointF p : points) {
            results[index++] = p.x;
            results[index++] = p.y;
        }
        return results;
    }

    static PointF[] toPointsArray(float [] a) {
        PointF[] results = new PointF[a.length >> 1];
        int index = 0;
        for (int i = 0; i < results.length; i++) {
            float x = a[index++];
            float y = a[index++];
            results[i] = new PointF(x, y);
        }
        return results;
    }
}
