package com.android.commands.monkey.ape.events;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.commands.monkey.MonkeyEvent;
import com.android.commands.monkey.MonkeyTouchEvent;

import android.graphics.PointF;
import android.os.SystemClock;
import android.view.MotionEvent;

public class ApeDragEvent extends AbstractApeEvent {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    float[] values;

    public ApeDragEvent(PointF[] points) {
        this.values = fromPointsArray(points);
        if (points.length < 2) {
            throw new IllegalArgumentException();
        }
    }

    private ApeDragEvent(float[] values) {
        this.values = values;
    }

    @Override
    public List<MonkeyEvent> generateMonkeyEvents() {
        int index = 0;
        PointF[] points = toPointsArray(values);
        final int size = points.length;
        List<MonkeyEvent> events = new ArrayList<MonkeyEvent>(size);
        long downAt = SystemClock.uptimeMillis();
        PointF p = points[index++];
        events.add(new MonkeyTouchEvent(MotionEvent.ACTION_DOWN).setDownTime(downAt).addPointer(0, p.x, p.y)
                .setIntermediateNote(false));
        for (;index < size - 1; index ++) {
            p = points[index];
            events.add(new MonkeyTouchEvent(MotionEvent.ACTION_MOVE).setDownTime(downAt).addPointer(0, p.x, p.y)
                    .setIntermediateNote(true));
        }
        p = points[index];
        events.add(new MonkeyTouchEvent(MotionEvent.ACTION_UP).setDownTime(downAt).addPointer(0, p.x, p.y)
                .setIntermediateNote(false));
        return events;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject jEvent = super.toJSONObject();
        jEvent.put("values", values);
        return jEvent;
    }

    public static ApeEvent fromJSONObject(JSONObject jEvent) throws JSONException {
        JSONArray jValues = jEvent.getJSONArray("values");
        float[] values = new float[jValues.length()];
        for (int i = 0; i < values.length; i++) {
            values[i] = (float) jValues.getDouble(i);
        }
        return new ApeDragEvent(values);
    }
}
