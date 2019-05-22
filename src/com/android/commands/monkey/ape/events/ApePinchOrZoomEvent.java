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

public class ApePinchOrZoomEvent extends AbstractApeEvent {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    float[] values;

    public ApePinchOrZoomEvent(PointF[] points) {
        this.values = fromPointsArray(points);
        if (points.length < 4) {
            throw new IllegalArgumentException();
        }
    }

    public ApePinchOrZoomEvent(float[] values) {
        this.values = values;
    }

    @Override
    public List<MonkeyEvent> generateMonkeyEvents() {
        int index = 0;
        PointF[] points = toPointsArray(values);
        int size = points.length;
        long downAt = SystemClock.uptimeMillis();
        List<MonkeyEvent> events = new ArrayList<MonkeyEvent>(size);
        PointF p = points[index++];
        events.add(new MonkeyTouchEvent(MotionEvent.ACTION_DOWN).setDownTime(downAt).addPointer(0, p.x, p.y)
                .setIntermediateNote(false));
        PointF p1 = points[index++];
        PointF p2 = points[index++];
        events.add(new MonkeyTouchEvent(MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT)).
                setDownTime(downAt).addPointer(0, p1.x, p1.y).addPointer(1, p2.x, p2.y).setIntermediateNote(true));
        for (;index < size - 3;) {
            p1 = points[index++];
            p2 = points[index++];
            events.add(new MonkeyTouchEvent(MotionEvent.ACTION_MOVE).setDownTime(downAt).addPointer(0, p1.x, p1.y)
                    .addPointer(1, p2.x, p2.y).setIntermediateNote(true));
        }
        p1 = points[index++];
        p2 = points[index++];
        events.add(new MonkeyTouchEvent(MotionEvent.ACTION_POINTER_UP | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT))
                .setDownTime(downAt).addPointer(0, p1.x, p1.y).addPointer(1, p2.x, p2.y).setIntermediateNote(true));
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
        return new ApePinchOrZoomEvent(values);
    }
}
