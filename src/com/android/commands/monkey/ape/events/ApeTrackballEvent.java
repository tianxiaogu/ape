package com.android.commands.monkey.ape.events;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.commands.monkey.MonkeyEvent;
import com.android.commands.monkey.MonkeyTrackballEvent;

import android.os.SystemClock;
import android.view.MotionEvent;

public class ApeTrackballEvent extends AbstractApeEvent {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    int [] deltaX;
    int [] deltaY;
    boolean doClick;

    public ApeTrackballEvent(int[] deltaX, int[] deltaY, boolean doClick) {
        super();
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.doClick = doClick;
    }

    @Override
    public List<MonkeyEvent> generateMonkeyEvents() {
        List<MonkeyEvent> events = new ArrayList<>((deltaX.length + (doClick ? 2: 0)));
        for (int i = 0; i < deltaX.length; ++i) {
            // generate a small random step
            int dX = deltaX[i];
            int dY = deltaY[i];

            events.add(new MonkeyTrackballEvent(MotionEvent.ACTION_MOVE).addPointer(0, dX, dY).setIntermediateNote(i > 0));
        }

        if (doClick) {
            long downAt = SystemClock.uptimeMillis();

            events.add(new MonkeyTrackballEvent(MotionEvent.ACTION_DOWN).setDownTime(downAt).addPointer(0, 0, 0)
                    .setIntermediateNote(true));
            events.add(new MonkeyTrackballEvent(MotionEvent.ACTION_UP).setDownTime(downAt).addPointer(0, 0, 0)
                    .setIntermediateNote(false));
        }
        return events;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject jEvent = super.toJSONObject();
        JSONArray jDeltaX = new JSONArray();
        for (int x : deltaX) {
            jDeltaX.put(x);
        }
        JSONArray jDeltaY = new JSONArray();
        for (int y : deltaY) {
            jDeltaY.put(y);
        }
        jEvent.put("click", doClick);
        jEvent.put("deltaX", jDeltaX);
        jEvent.put("deltaY", jDeltaY);
        return jEvent;
    }

    public static ApeEvent fromJSONObject(JSONObject jEvent) throws JSONException {
        boolean click = jEvent.getBoolean("click");
        JSONArray jDeltaX = jEvent.getJSONArray("deltaX");
        JSONArray jDeltaY = jEvent.getJSONArray("deltaY");
        int[] deltaX = new int[jDeltaX.length()];
        int[] deltaY = new int[jDeltaY.length()];
        for (int i = 0; i < deltaX.length; i++) {
            deltaX[i] = jDeltaX.getInt(i);
            deltaY[i] = jDeltaY.getInt(i);
        }
        return new ApeTrackballEvent(deltaX, deltaY, click);
    }
}
