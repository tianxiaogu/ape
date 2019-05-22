package com.android.commands.monkey;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.commands.monkey.ape.model.Action;
import com.android.commands.monkey.ape.utils.Logger;
import com.android.commands.monkey.ape.utils.Utils;

import android.content.Intent;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * Basic record-and-replay.
 * @author txgu
 *
 */
public class ApeRRFormatter {

    private static void formatMotionEvent(PrintWriter pw, MonkeyMotionEvent e) {
        MotionEvent me = e.getEvent();
        StringBuilder msg = new StringBuilder();
        msg.append(e.getTypeLabel().toLowerCase()).append(" ");
        switch (me.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
            msg.append("ACTION_DOWN");
            break;
        case MotionEvent.ACTION_MOVE:
            msg.append("ACTION_MOVE");
            break;
        case MotionEvent.ACTION_UP:
            msg.append("ACTION_UP");
            break;
        case MotionEvent.ACTION_CANCEL:
            msg.append("ACTION_CANCEL");
            break;
        case MotionEvent.ACTION_POINTER_DOWN:
            msg.append("ACTION_POINTER_DOWN ").append(me.getPointerId(me.getActionIndex()));
            break;
        case MotionEvent.ACTION_POINTER_UP:
            msg.append("ACTION_POINTER_UP ").append(me.getPointerId(me.getActionIndex()));
            break;
        default:
            msg.append(me.getAction());
            break;
        }
        int pointerCount = me.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            msg.append(" ").append(me.getPointerId(i));
            msg.append(" ").append(me.getX(i)).append(",").append(me.getY(i));
        }
        pw.print(msg.toString());
    }

    public static void logDrop(PrintWriter pw, MonkeyEvent e) {
        format(pw, "drop", e);
    }

    public static void logProduce(PrintWriter pw, MonkeyEvent e) {
        format(pw, "produce", e);
    }

    public static void logConsume(PrintWriter pw, MonkeyEvent e) {
        format(pw, "consume", e);
    }

    private static void format(PrintWriter pw, String type, MonkeyEvent e) {
        if (e instanceof MonkeyWaitEvent && ((MonkeyWaitEvent) e).mWaitTime <= 0) {
            return;
        } else if (e instanceof MonkeyThrottleEvent && ((MonkeyThrottleEvent) e).mThrottle <= 0) {
            return;
        }
        pw.print(type);
        pw.print(' ');
        pw.print(e.getEventId());
        pw.print(' ');
        switch (e.getEventType()) {
        case MonkeyEvent.EVENT_TYPE_ACTIVITY:
            formatActivityEvent(pw, (MonkeyActivityEvent) e);
            break;
        case MonkeyEvent.EVENT_TYPE_TRACKBALL:
        case MonkeyEvent.EVENT_TYPE_TOUCH:
            formatMotionEvent(pw, (MonkeyMotionEvent) e);
            break;
        case MonkeyEvent.EVENT_TYPE_THROTTLE:
            if (e instanceof MonkeyWaitEvent) {
                pw.format("throttle %d", ((MonkeyWaitEvent) e).mWaitTime);
            } else {
                pw.format("throttle %d", ((MonkeyThrottleEvent) e).mThrottle);
            }
            break;
        case MonkeyEvent.EVENT_TYPE_KEY:
            formatKeyEvent(pw, (MonkeyKeyEvent) e);
            break;
        case MonkeyEvent.EVENT_TYPE_ROTATION:
            MonkeyRotationEvent mre = (MonkeyRotationEvent) e;
            pw.format("%s%d", (mre.mPersist ? "persist " : ""), mre.mRotationDegree);
            break;
        default:
            pw.print("TODO: unsupported MonkeyEventType: " + e.getClass().getSimpleName());
            break;
        }
        pw.println();
    }

    private static void formatKeyEvent(PrintWriter pw, MonkeyKeyEvent e) {
        int keyCode = e.getKeyCode();
        if (e.getAction() == KeyEvent.ACTION_UP) {
            pw.format("key_up %d %s", keyCode, MonkeySourceRandom.getKeyName(keyCode));
        } else {
            pw.format("key_down %d %s", keyCode, MonkeySourceRandom.getKeyName(keyCode));
        }
    }

    private static void formatActivityEvent(PrintWriter pw, MonkeyActivityEvent e) {
        Intent i = e.getEvent();
        pw.format("start %s %s", i.getComponent().getPackageName(), i.getComponent().getClassName());
    }

    public static String formatRect(Rect rect) {
        return String.format("%d,%d,%d,%d", rect.left, rect.top, rect.right, rect.bottom);
    }

    public static Rect parseRect(String input) {
        String[] tokens = input.split(",");
        return new Rect(Integer.valueOf(tokens[0]), Integer.valueOf(tokens[1]), Integer.valueOf(tokens[2]),
                Integer.valueOf(tokens[3]));
    }

    public static void startLogAction(PrintWriter pw, Action action, long clockTime, long timestamp) {
        pw.format("%d %s\n", clockTime, recordAction(action, timestamp));
        pw.flush();
    }

    private static final Pattern decimalNumber = Pattern.compile("[0-9]+");

    /**
     * @param jsonString
     * @return null if jsonString is invalid
     */
    public static JSONObject readAction(String jsonString) {
        return Utils.toJSON(jsonString);
    }

    /**
     * type
     * full name
     * current name
     * bounds
     * @param action
     * @return
     */
    public static JSONObject recordAction(Action action, long timestamp) {
        try {
            JSONObject jAction = action.toJSONObject();
            jAction.put("timestamp", timestamp);
            return jAction;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<JSONObject> readActions(String logfile) {
        List<JSONObject> actions = new ArrayList<JSONObject>(); 
        try (BufferedReader br = new BufferedReader(new FileReader(logfile))) {
            String line;
            while ((line = br.readLine()) != null) {
                int index = line.indexOf(' ');
                if (index != -1) {
                    String head = line.substring(0, index);
                    if (decimalNumber.matcher(head).matches()) {
                        String tail = line.substring(index + 1);
                        JSONObject action = readAction(tail);
                        if (action == null) {
                            Logger.wformat("Fail to parse action line: %s", line);
                            continue;
                        }
                        actions.add(action);
                    }
                }
            }
        } catch (IOException e) {

        }
        return actions;
    }

    public static void endLogAction(PrintWriter pw, Action action, long timestamp) {
    }

    private static final boolean simplify = true;

    public static void toVisTimeline(File logFile, File output) {
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile));
                BufferedWriter writer = new BufferedWriter(new FileWriter(output));) {

            writer.write("var groups = new vis.DataSet();");
            writer.newLine();
            writer.write("groups.add({id: 0, content: \"SATA Action\"});");
            writer.newLine();
            if (!simplify) {
                writer.write("groups.add({id: 1, content: \"Monkey Event\"});");
                writer.newLine();
            }

            writer.write("var items = new vis.DataSet([");
            writer.newLine();
            String line = null;
            int id = 0;
            long startTime = Long.MAX_VALUE;
            long endTime = Long.MIN_VALUE;
            while ((line = reader.readLine()) != null) {
                int index = line.indexOf(" ");
                String type = line.substring(0, index);
                if (type.equals("produce")) {
                    continue;
                }
                long clockTimestamp = 0;
                try {
                    clockTimestamp = Long.valueOf(type); // millisecond
                } catch (NumberFormatException e) {
                    continue;
                }
                if (startTime > clockTimestamp) {
                    startTime = clockTimestamp;
                }
                if (endTime < clockTimestamp) {
                    endTime = clockTimestamp;
                }
                JSONObject jAction = readAction(line.substring(index + 1).trim());
                int step = jAction.getInt("timestamp");
                String screenURL = String.format("step-%d.png", step);
                String content = String.format("%s@%d", jAction.getString("actionType"), step);
                String rect = "";
                if (jAction.has("bounds")) {
                    rect = "[" + jAction.getString("bounds") + "]";
                }
                int group = 0;
                writer.write(String.format(
                        "{start: new Date(%d), title: \"%s@%d\", group: %d, screenURL: \"%s\", id: %d, content: \"%s\", rect: %s},",
                        clockTimestamp, new Date(clockTimestamp), id + 1, group, screenURL, id++, content, rect));
                writer.newLine();
            }
            writer.write("]);");
            writer.newLine();

            if (startTime != Long.MAX_VALUE) {
                writer.write(String.format("var startTime = new Date(%d);", startTime - 1000));
                writer.newLine();
                writer.write(String.format("var endTime = new Date(%d);", endTime + 1000));
                writer.newLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.wformat("Cannot transform (%s) into vis timeline (%s).", logFile, output);
        }
    }
}
