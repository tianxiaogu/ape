package com.android.commands.monkey.ape.events;

import java.util.ArrayList;
import java.util.List;

import com.android.commands.monkey.ape.AndroidDevice;
import com.android.commands.monkey.ape.utils.Logger;
import com.android.commands.monkey.ape.utils.RandomHelper;

import android.graphics.PointF;
import android.graphics.Rect;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.Surface;

public class ApeFuzzer {


    /** Possible screen rotation degrees **/
    private static final int[] SCREEN_ROTATION_DEGREES = { Surface.ROTATION_0, Surface.ROTATION_90,
            Surface.ROTATION_180, Surface.ROTATION_270, };
    /* private */ static final int GESTURE_TAP = 0;
    private static final int[] NAV_KEYS = { KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, };
    /**
     * Key events that perform major navigation options (so shouldn't be sent as
     * much).
     */
    private static final int[] MAJOR_NAV_KEYS = { KeyEvent.KEYCODE_MENU, /*
     * KeyEvent
     * .
     * KEYCODE_SOFT_RIGHT,
     */
            KeyEvent.KEYCODE_DPAD_CENTER, };
    /** Key events that perform system operations. */
    private static final int[] SYS_KEYS = {
            // KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_BACK,
            // KeyEvent.KEYCODE_CALL, KeyEvent.KEYCODE_ENDCALL,
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_MUTE, };
    /** If a physical key exists? */
    private static final boolean[] PHYSICAL_KEY_EXISTS = new boolean[KeyEvent.getMaxKeyCode() + 1];
    static {
        for (int i = 0; i < PHYSICAL_KEY_EXISTS.length; ++i) {
            PHYSICAL_KEY_EXISTS[i] = true;
        }
        // Only examine SYS_KEYS
        for (int i = 0; i < SYS_KEYS.length; ++i) {
            PHYSICAL_KEY_EXISTS[SYS_KEYS[i]] = KeyCharacterMap.deviceHasKey(SYS_KEYS[i]);
        }
    }

    public static List<ApeEvent> generateFuzzingEvents() {
        List<ApeEvent> events = new ArrayList<ApeEvent>();
        int repeat = RandomHelper.nextBetween(5, 15);
        while (repeat > 0) {
            repeat--;
            int eventType = RandomHelper.nextInt(20);
            switch (eventType) {
            case 0:
                Logger.iprintln("Fuzzing: generate rotation events.");
                generateRotationEvent(events);
                break;
            case 1:
                Logger.iprintln("Fuzzing: generate app switch events.");
                generateAppSwitchEvent(events);
                break;
            case 2:
            case 3:
            case 4:
            case 5:
                Logger.iprintln("Fuzzing: generate major navigation events.");
                if (RandomHelper.nextBoolean()) {
                    generateTrackballEvent(events);
                }
                for (int i = 0; i < 5; i++) {
                    if (RandomHelper.nextBoolean()) {
                        generateFuzzingNavKeyEvent(events);
                    } else {
                        generateFuzzingMajorNavKeyEvent(events);
                    }
                }
                if (RandomHelper.nextBoolean()) {
                    generateTrackballEvent(events);
                }
                break;
            case 6:
            case 7:
            case 8:
                Logger.iprintln("Fuzzing: generate system key events.");
                generateFuzzingSysKeyEvent(events);
                break;
            case 9:
            case 10:
                Logger.iprintln("Fuzzing: generate system key events.");
                int lastKey;
                while (true) {
                    lastKey = 1 + RandomHelper.nextInt(KeyEvent.getMaxKeyCode() - 1);
                    if (lastKey != KeyEvent.KEYCODE_POWER && lastKey != KeyEvent.KEYCODE_ENDCALL
                            && lastKey != KeyEvent.KEYCODE_SLEEP && PHYSICAL_KEY_EXISTS[lastKey]) {
                        break;
                    }
                }
                generateKeyEvent(events, lastKey);
                break;
            case 11:
            case 12:
                Logger.iprintln("Fuzzing: generate trackball.");
                generateTrackballEvent(events);
                break;
            default:
                switch (eventType % 3) {
                case 0:
                    Logger.iprintln("Fuzzing: generate drag.");
                    generateDragEvent(events);
                    break;
                case 1:
                    Logger.iprintln("Fuzzing: generate pinch or zoom.");
                    generatePinchOrZoomEvent(events);
                    break;
                default:
                    Logger.iprintln("Fuzzing: generate random click.");
                    generateClickEvent(events, RandomHelper.toss(0.1D));
                    break;
                }
            }
        }
        return events;
    }

    private static void generateClickEvent(List<ApeEvent> events, boolean longClick) {
        Rect displayBounds = AndroidDevice.getDisplayBounds();
        int x = RandomHelper.nextInt(displayBounds.right);
        int y = RandomHelper.nextInt(displayBounds.bottom);
        events.add(new ApeClickEvent(new PointF(x, y), longClick));
    }

    protected static PointF randomPoint(int width, int height) {
        return new PointF(RandomHelper.nextInt(width), RandomHelper.nextInt(height));
    }

    protected static PointF randomVector() {
        return new PointF((RandomHelper.nextFloat() - 0.5f) * 50, (RandomHelper.nextFloat() - 0.5f) * 50);
    }

    protected static PointF randomWalk(int width, int height, PointF point, PointF vector) {
        float x = (float) Math.max(Math.min(point.x + RandomHelper.nextFloat() * vector.x, width), 0);
        float y = (float) Math.max(Math.min(point.y + RandomHelper.nextFloat() * vector.y, height), 0);
        return new PointF(x, y);
    }

    private static void generatePinchOrZoomEvent(List<ApeEvent> events) {
        Rect displayBounds = AndroidDevice.getDisplayBounds();
        int width = displayBounds.right;
        int height = displayBounds.bottom;
        int count = RandomHelper.nextInt(10);
        int index = 0;
        PointF[] points = new PointF[4 + count << 1];
        PointF p1 = randomPoint(width, height);
        points[index++] = p1; // first action down

        p1 = randomPoint(width, height);
        PointF p2 = randomPoint(width, height);
        points[index++] = p1;
        points[index++] = p2;

        PointF v1 = randomVector();
        PointF v2 = randomVector();
        for (int i = 0; i <= count; i++) {
            p1 = randomWalk(width, height, p1, v1);
            p2 = randomWalk(width, height, p2, v2);
            points[index++] = p1;
            points[index++] = p2;
        }
        p1 = randomWalk(width, height, p1, v1);
        points[index++] = p1;
    }

    private static void generateDragEvent(List<ApeEvent> events) {
        Rect displayBounds = AndroidDevice.getDisplayBounds();
        int width = displayBounds.right;
        int height = displayBounds.bottom;
        int count = RandomHelper.nextInt(10);
        int index = 0;
        PointF[] points = new PointF[2 + count];
        PointF p = randomPoint(width, height);
        points[index++] = p;
        PointF v1 = randomVector();
        for (; index < points.length; index++) {
            p = randomWalk(width, height, p, v1);
            points[index] = p;
        }
        events.add(new ApeDragEvent(points));
    }

    private static void generateKeyEvent(List<ApeEvent> events, int key) {
        events.add(new ApeKeyEvent(key));
    }

    private static void generateFuzzingSysKeyEvent(List<ApeEvent> events) {
        int key = SYS_KEYS[RandomHelper.nextInt(SYS_KEYS.length)];
        generateKeyEvent(events, key);
    }

    private static void generateFuzzingMajorNavKeyEvent(List<ApeEvent> events) {
        int key = MAJOR_NAV_KEYS[RandomHelper.nextInt(MAJOR_NAV_KEYS.length)];
        generateKeyEvent(events, key);
    }

    private static void generateFuzzingNavKeyEvent(List<ApeEvent> events) {
        int key = NAV_KEYS[RandomHelper.nextInt(NAV_KEYS.length)];
        generateKeyEvent(events, key);
    }

    private static void generateTrackballEvent(List<ApeEvent> events) {
        int moves = 10;
        int[] deltaX = new int[moves];
        int[] deltaY = new int[moves];
        for (int i = 0; i < moves; ++i) {
            deltaX[i] = RandomHelper.nextInt(10) - 5;
            deltaY[i] = RandomHelper.nextInt(10) - 5;
        }
        events.add(new ApeTrackballEvent(deltaX, deltaY, RandomHelper.nextBoolean()));
    }

    private static void generateAppSwitchEvent(List<ApeEvent> events) {
        events.add(new ApeAppSwitchEvent(RandomHelper.nextBoolean()));
    }

    private static void generateRotationEvent(List<ApeEvent> events) {
        int degree = SCREEN_ROTATION_DEGREES[RandomHelper.nextInt(SCREEN_ROTATION_DEGREES.length)];
        events.add(new ApeRotationEvent(degree, RandomHelper.nextBoolean()));
    }
}
