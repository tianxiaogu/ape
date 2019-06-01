/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.commands.monkey;

import static com.android.commands.monkey.ape.utils.Config.defaultGUIThrottle;
import static com.android.commands.monkey.ape.utils.Config.doFuzzing;
import static com.android.commands.monkey.ape.utils.Config.fuzzingRate;
import static com.android.commands.monkey.ape.utils.Config.imageWriterCount;
import static com.android.commands.monkey.ape.utils.Config.refectchInfoCount;
import static com.android.commands.monkey.ape.utils.Config.refectchInfoWaitingInterval;
import static com.android.commands.monkey.ape.utils.Config.swipeDuration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import com.android.commands.monkey.ape.Agent;
import com.android.commands.monkey.ape.AndroidDevice;
import com.android.commands.monkey.ape.ImageWriterQueue;
import com.android.commands.monkey.ape.StopTestingException;
import com.android.commands.monkey.ape.agent.ApeAgent;
import com.android.commands.monkey.ape.agent.ReplayAgent;
import com.android.commands.monkey.ape.events.ApeEvent;
import com.android.commands.monkey.ape.model.Action;
import com.android.commands.monkey.ape.model.ActionType;
import com.android.commands.monkey.ape.model.FuzzAction;
import com.android.commands.monkey.ape.model.ModelAction;
import com.android.commands.monkey.ape.model.StartAction;
import com.android.commands.monkey.ape.tree.GUITreeNode;
import com.android.commands.monkey.ape.utils.Logger;
import com.android.commands.monkey.ape.utils.RandomHelper;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.UiAutomation;
import android.app.UiAutomationConnection;
import android.content.ComponentName;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.display.DisplayManagerGlobal;
import android.os.Build;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * monkey event queue
 */
public class MonkeySourceApe implements MonkeyEventSource {

    private static long CLICK_WAIT_TIME = 0L;
    private static long LONG_CLICK_WAIT_TIME = 1000L;

    private static final boolean useRandomClick = false;



    /** Possible screen rotation degrees **/
    private static final int[] SCREEN_ROTATION_DEGREES = { Surface.ROTATION_0, Surface.ROTATION_90,
            Surface.ROTATION_180, Surface.ROTATION_270, };

    private List<ComponentName> mMainApps;
    private Map<String, String[]> packagePermissions;
    private int mEventCount = 0; // total number of events generated so far
    private MonkeyEventQueue mQ;
    private int mVerbose = 0;
    private long mThrottle = defaultGUIThrottle;
    private boolean mRandomizeThrottle = false;
    private MonkeyPermissionUtil mPermissionUtil;
    private Random mRandom;

    // private boolean mKeyboardOpen = false;
    private Agent mAgent;
    private int mEventId = 0;
    private int statusBarHeight = -1;
    private File mOutputDirectory;
    private PrintWriter mEventProduceLogger;
    private PrintWriter mEventConsumeLogger;
    private File mEventProduceLoggerFile;
    private File mEventConsumeLoggerFile;
    private ImageWriterQueue[] mImageWriters;

    // Counter

    int nullInfoCounter = 0;
    int lostFocusedCounter = 0;

    /**
     * UiAutomation client and connection
     */
    protected final HandlerThread mHandlerThread = new HandlerThread("MonkeySourceApe");
    protected UiAutomation mUiAutomation;

    public static String getKeyName(int keycode) {
        return KeyEvent.keyCodeToString(keycode);
    }

    /**
     * Looks up the keyCode from a given KEYCODE_NAME. NOTE: This may be an
     * expensive operation.
     *
     * @param keyName
     *            the name of the KEYCODE_VALUE to lookup.
     * @returns the intenger keyCode value, or KeyEvent.KEYCODE_UNKNOWN if not
     *          found
     */
    public static int getKeyCode(String keyName) {
        return KeyEvent.keyCodeFromString(keyName);
    }

    public static boolean hasKey(int key) {
        return KeyCharacterMap.deviceHasKey(key);
    }

    public int getStatusBarHeight() {
        if (this.statusBarHeight == -1) {
            Display display = DisplayManagerGlobal.getInstance().getRealDisplay(Display.DEFAULT_DISPLAY);
            DisplayMetrics dm = new DisplayMetrics();
            display.getMetrics(dm);
            this.statusBarHeight = (int) (24 * dm.density);
        }
        return this.statusBarHeight;
    }

    /**
     * Connect to AccessibilityService
     */
    public void connect() {
        if (mHandlerThread.isAlive()) {
            throw new IllegalStateException("Already connected!");
        }
        mHandlerThread.start();
        mUiAutomation = new UiAutomation(mHandlerThread.getLooper(), new UiAutomationConnection());
        mUiAutomation.connect();

        AccessibilityServiceInfo info = mUiAutomation.getServiceInfo();
        // Compress this node
        info.flags &= ~AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;

        mUiAutomation.setServiceInfo(info);

        mImageWriters = new ImageWriterQueue[imageWriterCount];
        for (int i = 0; i < 3; i++) {
            mImageWriters[i] = new ImageWriterQueue();
            Thread imageThread = new Thread(mImageWriters[i]);
            imageThread.start();
        }
    }

    public int getEventCount() {
        return mEventCount;
    }

    /**
     * Disconnect to AccessibilityService
     */
    public void disconnect() {
        if (!mHandlerThread.isAlive()) {
            throw new IllegalStateException("Already disconnected!");
        }
        mUiAutomation.disconnect();
        mHandlerThread.quit();
    }

    public void tearDown() {
        this.disconnect();
        this.mAgent.tearDown();
        for (ImageWriterQueue writer : mImageWriters) {
            writer.tearDown();
        }
        this.mEventProduceLogger.close();
        this.mEventConsumeLogger.close();
        File visOutput = new File(getOutputDirectory(), "sataTimeline.vis.js");
        ApeRRFormatter.toVisTimeline(mEventProduceLoggerFile, visOutput);
    }

    public MonkeySourceApe(Random random,
            List<ComponentName> MainApps, long throttle, boolean randomizeThrottle, boolean permissionTargetSystem,
            File outputDirectory) {
        mRandom = random;
        mMainApps = MainApps;
        packagePermissions = new HashMap<>();
        for (ComponentName app : MainApps) {
            packagePermissions.put(app.getPackageName(), AndroidDevice.getGrantedPermissions(app.getPackageName()));
        }
        mThrottle = throttle;
        if (mThrottle == 0) {
            mThrottle = defaultGUIThrottle;
        }
        mRandomizeThrottle = randomizeThrottle;
        mQ = new MonkeyEventQueue(random, 0, false); // we manage throttle
        mPermissionUtil = new MonkeyPermissionUtil();
        mPermissionUtil.setTargetSystemPackages(permissionTargetSystem);
        // mPermissionUtil.populatePermissionsMapping();
        mOutputDirectory = outputDirectory;
        mEventProduceLoggerFile = new File(mOutputDirectory, "produce.log");
        mEventProduceLogger = openWriter(mEventProduceLoggerFile);
        mEventConsumeLoggerFile = new File(mOutputDirectory, "consume.log");
        mEventConsumeLogger = openWriter(mEventConsumeLoggerFile);

        mAgent = ApeAgent.createAgent(this);
        connect();
    }

    static PrintWriter openWriter(File logFile) {
        try {
            return new PrintWriter(new BufferedWriter(new FileWriter(logFile)));
        } catch (IOException e) {
            e.printStackTrace();
            Logger.wprintln("Cannot open " + logFile);
            System.exit(1);
        }
        return null;
    }

    public Agent getAgent() {
        return mAgent;
    }

    public File getOutputDirectory() {
        if (!this.mOutputDirectory.exists()) {
            if (!this.mOutputDirectory.mkdirs()) {
                Logger.wprintln("Fail to create output directory at " + this.mOutputDirectory);
                Logger.wprintln("Use /data/local/tmp instead.");
                return new File("/data/local/tmp");
            }
        }
        return this.mOutputDirectory;
    }

    public String getTopActivityPackageName() {
        ComponentName cn = getTopActivityComponentName();
        if (cn != null) {
            return cn.getPackageName();
        }
        return Monkey.currentPackage;
    }

    public String getTopActivityClassName() {
        ComponentName cn = getTopActivityComponentName();
        if (cn != null) {
            return cn.getClassName();
        }
        return Monkey.currentPackage;
    }

    public ComponentName getTopActivityComponentName() {
        return AndroidDevice.getTopActivityComponentName();
    }

    /**
     * Get visible bounds of a given node.
     */
    public Rect getVisibleBounds(Rect nodeRect) {
        Rect visibleBounds = getVisibleBounds();

        if (!visibleBounds.intersect(nodeRect)) {
            return null;
        }
        return visibleBounds;
    }

    /**
     * Always return a fresh rect
     * 
     * @return
     */
    protected Rect getVisibleBounds() {
        Rect bounds = mAgent.getCurrentRootNodeBounds();
        if (bounds == null) {
            Display display = DisplayManagerGlobal.getInstance().getRealDisplay(Display.DEFAULT_DISPLAY);
            Point size = new Point();
            display.getSize(size);
            bounds = new Rect();
            bounds.top = 0;
            bounds.left = 0;
            bounds.right = size.x;
            bounds.bottom = size.y;
        } else {
            // avoid modification
            bounds = new Rect(bounds);
            Rect displayBounds = AndroidDevice.getDisplayBounds();
            if (!bounds.intersect(displayBounds)) {
                return displayBounds;
            }
        }

        return bounds;
    }



    int outOfBoundsCounter = 0;

    private boolean waitForActivity;

    private boolean clearPackageOnGeneratingActivity;

    private int lastStartTimestamp = -1;

    private boolean waitForActivityFromClean;

    protected void generateClickEventAt(Rect nodeRect, long waitTime) {
        generateClickEventAt(nodeRect, waitTime, useRandomClick);
    }

    protected void generateClickEventAt(Rect nodeRect, long waitTime, boolean useRandomClick) {
        Rect bounds = getVisibleBounds(nodeRect);
        if (bounds == null) {
            Logger.wprintln("Error to fetch bounds.");
            bounds = AndroidDevice.getDisplayBounds();
        }

        PointF p1;
        if (useRandomClick) {
            int width = bounds.width() > 0 ? getRandom().nextInt(bounds.width()) : 0;
            int height = bounds.height() > 0 ? getRandom().nextInt(bounds.height()) : 0;
            p1 = new PointF(bounds.left + width, bounds.top + height);
        } else {
            p1 = new PointF(bounds.exactCenterX(), bounds.exactCenterY());
        }
        if (!bounds.contains((int) p1.x, (int) p1.y)) {
            // throw new RuntimeException("Bug");
            Logger.wformat("Invalid bounds: %s", bounds);
            return;
        }
        long downAt = SystemClock.uptimeMillis();

        addEvent(new MonkeyTouchEvent(MotionEvent.ACTION_DOWN).setDownTime(downAt).addPointer(0, p1.x, p1.y)
                .setIntermediateNote(false));

        if (waitTime > 0) {
            MonkeyWaitEvent we = new MonkeyWaitEvent(waitTime);
            addEvent(we);
        }

        addEvent(new MonkeyTouchEvent(MotionEvent.ACTION_UP).setDownTime(downAt).addPointer(0, p1.x, p1.y)
                .setIntermediateNote(false));
    }

    protected void generateKeyBackEvent() {
        generateKeyEvent(KeyEvent.KEYCODE_BACK);
    }

    protected void generateKeyMenuEvent() {
        generateKeyEvent(KeyEvent.KEYCODE_MENU);
        generateThrottleEvent(mThrottle);
    }

    /**
     * Generate a key event at specific key.
     */
    protected void generateKeyEvent(int key) {
        if (mVerbose > 0) {
            if (!hasKey(key)) {
                Logger.println("Device has no key " + getKeyName(key));
            }
        }
        MonkeyKeyEvent e = new MonkeyKeyEvent(KeyEvent.ACTION_DOWN, key);
        addEvent(e);

        e = new MonkeyKeyEvent(KeyEvent.ACTION_UP, key);
        addEvent(e);
    }

    /**
     * ActiveWindow may not belong to activity package.
     * 
     * @return
     */
    public AccessibilityNodeInfo getRootInActiveWindow() {
        return mUiAutomation.getRootInActiveWindow();
    }

    public AccessibilityNodeInfo getRootInActiveWindowSlow() {
        try {
            mUiAutomation.waitForIdle(1000, 1000 * 10);
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        return mUiAutomation.getRootInActiveWindow();
    }

    /* private */ static final int GESTURE_TAP = 0;
    private static final int GESTURE_DRAG = 1;
    private static final int GESTURE_PINCH_OR_ZOOM = 2;
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
    protected void generateFuzzingMajorNavKeyEvents() {
        int lastKey = MAJOR_NAV_KEYS[mRandom.nextInt(MAJOR_NAV_KEYS.length)];
        generateKeyEvent(lastKey);
    }

    protected void generateFuzzingNavKeyEvents() {
        int lastKey = NAV_KEYS[mRandom.nextInt(NAV_KEYS.length)];
        generateKeyEvent(lastKey);
    }

    protected void generateFuzzingSysKeyEvents() {
        int lastKey = SYS_KEYS[mRandom.nextInt(SYS_KEYS.length)];
        generateKeyEvent(lastKey);
    }

    protected PointF randomPoint(Random random, int width, int height) {
        return new PointF(random.nextInt(width), random.nextInt(height));
    }

    protected PointF randomVector(Random random) {
        return new PointF((random.nextFloat() - 0.5f) * 50, (random.nextFloat() - 0.5f) * 50);
    }

    protected void randomWalk(Random random, int width, int height, PointF point, PointF vector) {
        point.x = (float) Math.max(Math.min(point.x + random.nextFloat() * vector.x, width), 0);
        point.y = (float) Math.max(Math.min(point.y + random.nextFloat() * vector.y, height), 0);
    }

    protected void generatePointerEvent(PointF p1, Random random, int gesture) {
        Rect bounds = getVisibleBounds();
        int width = bounds.right;
        int height = bounds.bottom;

        if (p1 == null) {
            p1 = randomPoint(random, width, height);
        }
        PointF v1 = randomVector(random);

        long downAt = SystemClock.uptimeMillis();

        addEvent(new MonkeyTouchEvent(MotionEvent.ACTION_DOWN).setDownTime(downAt).addPointer(0, p1.x, p1.y)
                .setIntermediateNote(false));

        // sometimes we'll move during the touch
        if (gesture == GESTURE_DRAG) {
            int count = random.nextInt(10);
            for (int i = 0; i < count; i++) {
                randomWalk(random, width, height, p1, v1);

                addEvent(new MonkeyTouchEvent(MotionEvent.ACTION_MOVE).setDownTime(downAt).addPointer(0, p1.x, p1.y)
                        .setIntermediateNote(true));
            }
        } else if (gesture == GESTURE_PINCH_OR_ZOOM) {
            PointF p2 = randomPoint(random, width, height);
            PointF v2 = randomVector(random);

            randomWalk(random, width, height, p1, v1);
            addEvent(new MonkeyTouchEvent(
                    MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT)).setDownTime(downAt)
                    .addPointer(0, p1.x, p1.y).addPointer(1, p2.x, p2.y).setIntermediateNote(true));

            int count = random.nextInt(10);
            for (int i = 0; i < count; i++) {
                randomWalk(random, width, height, p1, v1);
                randomWalk(random, width, height, p2, v2);

                addEvent(new MonkeyTouchEvent(MotionEvent.ACTION_MOVE).setDownTime(downAt).addPointer(0, p1.x, p1.y)
                        .addPointer(1, p2.x, p2.y).setIntermediateNote(true));
            }

            randomWalk(random, width, height, p1, v1);
            randomWalk(random, width, height, p2, v2);
            addEvent(
                    new MonkeyTouchEvent(MotionEvent.ACTION_POINTER_UP | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT))
                    .setDownTime(downAt).addPointer(0, p1.x, p1.y).addPointer(1, p2.x, p2.y)
                    .setIntermediateNote(true));
        }

        randomWalk(random, width, height, p1, v1);
        addEvent(new MonkeyTouchEvent(MotionEvent.ACTION_UP).setDownTime(downAt).addPointer(0, p1.x, p1.y)
                .setIntermediateNote(false));
    }

    private boolean validateClickAction(ModelAction action) {
        return validateBounds(action);
    }

    private boolean validateBounds(ModelAction action) {
        GUITreeNode node = action.getResolvedNode();
        return !node.isEmpty() && !node.isOutOfRoot();
    }

    private boolean validateScrollAction(ModelAction action) {
        return validateBounds(action);
    }

    public boolean validateResolvedAction(ModelAction action) {
        switch (action.getType()) {
        case EVENT_START:
        case EVENT_RESTART:
        case EVENT_CLEAN_RESTART:
        case EVENT_NOP:
        case EVENT_ACTIVATE:
            return true;
        case MODEL_BACK:
            return true;
        case MODEL_CLICK:
        case MODEL_LONG_CLICK:
            return validateClickAction(action);
        case MODEL_SCROLL_BOTTOM_UP:
        case MODEL_SCROLL_TOP_DOWN:
        case MODEL_SCROLL_LEFT_RIGHT:
        case MODEL_SCROLL_RIGHT_LEFT:
            return validateScrollAction(action);
        default:
            throw new RuntimeException("Should not reach here");
        }
    }

    protected void generateActivateEvent() {
        Logger.iprintln("Activating: generate app switch events.");
        generateAppSwitchEvent();
        mAgent.startNewEpisode();
    }

    void generateRotationEvent(Random random) {
        addEvent(new MonkeyRotationEvent(SCREEN_ROTATION_DEGREES[random.nextInt(SCREEN_ROTATION_DEGREES.length)],
                random.nextBoolean()));
    }

    void resetRotation() {
        addEvent(new MonkeyRotationEvent(Surface.ROTATION_0, false));
    }

    private final boolean hasEvent() {
        return !mQ.isEmpty();
    }

    private final void addEvent(MonkeyEvent event) {
        mQ.addLast(event);
        event.setEventId(mEventId++);
        ApeRRFormatter.logProduce(mEventProduceLogger, event);
    }

    private final void clearEvent() {
        while (!mQ.isEmpty()) {
            MonkeyEvent e = mQ.removeFirst();
            ApeRRFormatter.logDrop(mEventConsumeLogger, e);
        }
    }

    private final MonkeyEvent popEvent() {
        return mQ.removeFirst();
    }

    void generateTrackballEvent() {
        Random random = mRandom;
        for (int i = 0; i < 10; ++i) {
            // generate a small random step
            int dX = random.nextInt(10) - 5;
            int dY = random.nextInt(10) - 5;

            addEvent(new MonkeyTrackballEvent(MotionEvent.ACTION_MOVE).addPointer(0, dX, dY).setIntermediateNote(i > 0));
        }

        // 10% of trackball moves end with a click
        if (0 == random.nextInt(10)) {
            long downAt = SystemClock.uptimeMillis();

            addEvent(new MonkeyTrackballEvent(MotionEvent.ACTION_DOWN).setDownTime(downAt).addPointer(0, 0, 0)
                    .setIntermediateNote(true));

            addEvent(new MonkeyTrackballEvent(MotionEvent.ACTION_UP).setDownTime(downAt).addPointer(0, 0, 0)
                    .setIntermediateNote(false));
        }
    }

    protected void generateScrollEventAt(Action action) {
        Rect displayBounds = AndroidDevice.getDisplayBounds();
        Rect nodeRect = action.getResolvedNode().getBoundsInScreen();
        if (!nodeRect.intersect(displayBounds)) {
            Logger.wformat("Action " + action + " should be validated first!");
            nodeRect = displayBounds;
        }
        PointF start = new PointF(nodeRect.exactCenterX(), nodeRect.exactCenterY());
        PointF end = null;
        ActionType type = action.getType();
        switch (type) {
        case MODEL_SCROLL_BOTTOM_UP:
            int top = getStatusBarHeight();
            if (top < displayBounds.top) {
                top = displayBounds.top;
            }
            end = new PointF(start.x, top); // top is inclusive
            break;
        case MODEL_SCROLL_TOP_DOWN:
            end = new PointF(start.x, displayBounds.bottom - 1); // bottom is
            // exclusive
            break;
        case MODEL_SCROLL_LEFT_RIGHT:
            end = new PointF(displayBounds.right - 1, start.y); // right is
            // exclusive
            break;
        case MODEL_SCROLL_RIGHT_LEFT:
            end = new PointF(displayBounds.left, start.y); // left is inclusive
            break;
        default:
            throw new RuntimeException("Should not reach here");
        }
        long downAt = SystemClock.uptimeMillis();

        addEvent(new MonkeyTouchEvent(MotionEvent.ACTION_DOWN).setDownTime(downAt).addPointer(0, start.x, start.y)
                .setIntermediateNote(false));

        long duration = swipeDuration;
        int steps = 10;
        long waitTime = duration / steps;
        for (int i = 0; i < steps; i++) {
            float alpha = i / (float) steps;
            addEvent(new MonkeyTouchEvent(MotionEvent.ACTION_MOVE).setDownTime(downAt)
                    .addPointer(0, lerp(start.x, end.x, alpha), lerp(start.y, end.y, alpha)).setIntermediateNote(true));
            addEvent(new MonkeyWaitEvent(waitTime));
        }

        addEvent(new MonkeyTouchEvent(MotionEvent.ACTION_UP).setDownTime(downAt).addPointer(0, end.x, end.y)
                .setIntermediateNote(false));
    }

    private static final float lerp(float a, float b, float alpha) {
        return (b - a) * alpha + a;
    }

    void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static void printInfo(String tab, int index, AccessibilityNodeInfo info) {
        if (info == null) {
            Logger.format("%s[%d]null info", tab, index);
            return;
        }
        Logger.format("%s[%d] %s %s %s", tab, index, info.getPackageName(), info.getClassName(),
                info.getBoundsInScreen());
        tab = tab + "\t";
        for (int i = 0; i < info.getChildCount(); i++) {
            AccessibilityNodeInfo child = info.getChild(i);
            printInfo(tab, i, child);
        }
    }

    /**
     * generate a random event based on mFactor
     */
    protected void generateEvents() {
        if (hasEvent()) {
            return;
        }

        ComponentName topComp = null;
        AccessibilityNodeInfo info = null;
        int repeat = refectchInfoCount;

        Action action = null;
        while (repeat-- > 0) {
            topComp = this.getTopActivityComponentName();
            info = getRootInActiveWindow();
            // this two operations may not be the same
            if (info == null) {
                sleep(refectchInfoWaitingInterval);
                continue;
            }
            if (info != null) {
                nullInfoCounter = 0;
                // if (!checkVirtualKeyboard()) { // first do text input
                action = mAgent.updateState(topComp, info);
                if (action == null) {
                    throw new NullPointerException("Resolved action should not be null");
                }
                break;
                // }
            }
            // if (hasEvent()) { // input events
            //     break;
            // }
        }
        if (info == null) {
            Logger.wprintln("Null info root node returned by UiTestAutomationBridge, generate activate action...");
            nullInfoCounter++;
            if (nullInfoCounter > 10) {
                stopTopActivity();
                nullInfoCounter = 0;
            }
            mAgent.onVoidGUITree(nullInfoCounter);
            AndroidDevice.checkInteractive();
            action = Action.ACTIVATE;
        }
        if (action == null) {
            action = Action.NOP;
        }
        generateEventsForAction(action);
        if (doFuzzing && RandomHelper.toss(fuzzingRate)) {
            if (mAgent.canFuzzing()) {
                Action fuzzingAction = mAgent.generateFuzzingAction();
                generateEventsForAction(fuzzingAction);
            }
        }
    }

    protected void startRandomMainApp() {
        generateEventsForAction(Action.getStartAction(ActionType.EVENT_START, randomlyPickMainApp()));
    }

    private void generateEventsForActionInternal(Action action) {
        switch (action.getType()) {
        case FUZZ:
            generateFuzzingEvents((FuzzAction) action);
            break;
        case EVENT_START:
            generateActivityEvents(((StartAction) action).getActivity(), false);
            break;
        case EVENT_RESTART:
            restartPackage(((StartAction) action).getActivity(), false, "start action");
            break;
        case EVENT_CLEAN_RESTART:
            restartPackage(((StartAction) action).getActivity(), true, "start action");
            break;
        case EVENT_NOP:
            generateThrottleEvent(action.getThrottle());
            break;
        case EVENT_ACTIVATE:
            generateActivateEvent();
            break;
        case MODEL_BACK:
            generateKeyBackEvent();
            break;
        case MODEL_CLICK:
            GUITreeNode node = action.getResolvedNode();
            generateClickEventAt(action.getResolvedNode().getBoundsInScreen(), CLICK_WAIT_TIME);
            if (node != null && node.getInputText() != null) {
                doInput(action, node);
            }
            break;
        case MODEL_LONG_CLICK:
            generateClickEventAt(action.getResolvedNode().getBoundsInScreen(), LONG_CLICK_WAIT_TIME);
            break;
        case MODEL_SCROLL_BOTTOM_UP:
        case MODEL_SCROLL_TOP_DOWN:
        case MODEL_SCROLL_LEFT_RIGHT:
        case MODEL_SCROLL_RIGHT_LEFT:
            generateScrollEventAt(action);
            break;
        default:
            throw new RuntimeException("Should not reach here");
        }
    }

    private void generateFuzzingEvents(FuzzAction action) {
        List<ApeEvent> events = action.getFuzzingEvents();
        long throttle = mThrottle + action.getThrottle();
        for (ApeEvent event : events) {
            List<MonkeyEvent> monkeyEvents = event.generateMonkeyEvents();
            for (MonkeyEvent me : monkeyEvents) {
                if (me == null) {
                    throw new RuntimeException();
                }
                addEvent(me);
            }
            generateThrottleEvent(throttle);
        }
        Logger.iprintln("Fuzzing: reset rotation.");
        resetRotation();
        // avoid non-deterministic actions
        mAgent.startNewEpisode();
    }

    private void generateEventsForAction(Action action) {
        long clockTimestamp = System.currentTimeMillis();
        startLogAction(clockTimestamp, action);
        mAgent.appendToActionHistory(clockTimestamp, action);
        generateEventsForActionInternal(action);
        long throttle = mThrottle + action.getThrottle();
        generateThrottleEvent(throttle);
        endLogAction(action);
    }

    protected boolean checkPackage(ComponentName topComp, AccessibilityNodeInfo info) {
        String packageName = topComp.getPackageName();
        String infoPkg = info.getPackageName().toString();
        if (!infoPkg.equals(packageName)) {
            Logger.wformat("Different packages: top(%s) v.s. info(%s).", packageName, infoPkg);
            return false;
        }
        if (!MonkeyUtils.getPackageFilter().checkEnteringPackage(packageName)) {
            Logger.format("Disallowed package: %s", packageName);
            return false;
        }
        return true;
    }

    public void clearPackage(String packageName) {
        String[] permissions = this.packagePermissions.get(packageName);
        if (permissions == null) {
            Logger.wprintln("Stop clearing untracked package: " + packageName);
            return;
        }
        AndroidDevice.clearPackage(packageName, permissions);
    }

    public void grantRuntimePermissions(String packageName, String reason) {
        String[] permissions = this.packagePermissions.get(packageName);
        if (permissions == null) {
            Logger.wprintln("Stop granting permissions to untracked package: " + packageName);
            return;
        }
        AndroidDevice.grantRuntimePermissions(packageName, permissions, reason);
    }

    public void grantRuntimePermissions(String reason) {
        for (ComponentName cn : mMainApps) {
            grantRuntimePermissions(cn.getPackageName(), reason);
        }
    }

    protected void restartPackage(ComponentName cn, boolean clearPackage, String reason) {
        String packageName = cn.getPackageName();
        Logger.iprintln("Try to restart package " + packageName + " for " + reason);
        stopPackage(cn.getPackageName());
        generateActivityEvents(cn, clearPackage);
    }

    /**
     * Stop the foreground activity
     */
    public void stopTopActivity() {
        boolean killed = false;
        try {
            List<RunningAppProcessInfo> processes = AndroidDevice.iActivityManager.getRunningAppProcesses();
            if (!processes.isEmpty()) {
                RunningAppProcessInfo process = processes.get(0);
                Logger.format("Try to stop process %s(%d) ", process.processName, process.pid);
                // mDevice.mAm.killPids(new int[] {process.pid}, "Killed by
                // ape", true);
                if (AndroidDevice.killPids(process.pid) == 0) { // only when you are
                    // rooted
                    killed = true;
                    Logger.format("Process %s(%d) is killed", process.processName, process.pid);
                }
            }
            mAgent.onActivityStopped();
        } catch (RemoteException e1) {
            e1.printStackTrace();
            killed = false;
        }

        if (!killed) {
            stopPackages();
        }
        AndroidDevice.checkInteractive();
    }

    void stopPackages() {
        for (ComponentName cn : mMainApps) {
            stopPackage(cn.getPackageName());
        }
    }

    void stopPackage(String packageName) {
        if (AndroidDevice.stopPackage(packageName)) {
            Logger.iformat("Package %s has been stopped", packageName);
        } else {
            Logger.wformat("Package %s has NOT been stopped", packageName);
        }
    }

    protected void generateFuzzingEvents() {
        int repeat = RandomHelper.nextBetween(10, 20);
        while (repeat > 0) {
            repeat--;
            int eventType = RandomHelper.nextInt(20);
            switch (eventType) {
            case 0:
                Logger.iprintln("Fuzzing: generate rotation events.");
                generateRotationEvent(getRandom());
                break;
            case 1:
                Logger.iprintln("Fuzzing: generate app switch events.");
                generateAppSwitchEvent();
                break;
            case 2:
            case 3:
            case 4:
            case 5:
                Logger.iprintln("Fuzzing: generate major navigation events.");
                if (RandomHelper.nextBoolean()) {
                    generateTrackballEvent();
                }
                for (int i = 0; i < 5; i++) {
                    if (RandomHelper.nextBoolean()) {
                        generateFuzzingNavKeyEvents();
                    } else {
                        generateFuzzingMajorNavKeyEvents();
                    }
                }
                if (RandomHelper.nextBoolean()) {
                    generateTrackballEvent();
                }
                break;
            case 6:
            case 7:
            case 8:
                Logger.iprintln("Fuzzing: generate system key events.");
                generateFuzzingSysKeyEvents();
                break;
            case 9:
            case 10:
                Logger.iprintln("Fuzzing: generate system key events.");
                int lastKey;
                while (true) {
                    lastKey = 1 + mRandom.nextInt(KeyEvent.getMaxKeyCode() - 1);
                    if (lastKey != KeyEvent.KEYCODE_POWER && lastKey != KeyEvent.KEYCODE_ENDCALL
                            && lastKey != KeyEvent.KEYCODE_SLEEP && PHYSICAL_KEY_EXISTS[lastKey]) {
                        break;
                    }
                }
                generateKeyEvent(lastKey);
                break;
            case 11:
            case 12:
                Logger.iprintln("Fuzzing: generate trackball.");
                generateTrackballEvent();
                break;
            default:
                switch (eventType % 3) {
                case 0:
                    Logger.iprintln("Fuzzing: generate drag.");
                    generatePointerEvent(null, mRandom, GESTURE_DRAG);
                    break;
                case 1:
                    Logger.iprintln("Fuzzing: generate pinch or zoom.");
                    generatePointerEvent(null, mRandom, GESTURE_PINCH_OR_ZOOM);
                    break;
                default:
                    Logger.iprintln("Fuzzing: generate random click.");
                    generateRandomClick(RandomHelper.toss(0.1D));
                    break;
                }
            }
            generateThrottleEvent(mThrottle);
        }
        Logger.iprintln("Fuzzing: reset rotation.");
        resetRotation();
        // avoid non-deterministic actions
        mAgent.startNewEpisode();
    }

    void generateRandomClick(boolean longClick) {
        Rect rect = getVisibleBounds();
        generateClickEventAt(rect, (longClick ? LONG_CLICK_WAIT_TIME : CLICK_WAIT_TIME), true);
    }

    private void generateAppSwitchEvent() {
        generateKeyEvent(KeyEvent.KEYCODE_APP_SWITCH);
        generateThrottleEvent(500);
        if (RandomHelper.nextBoolean()) {
            Logger.println("Press HOME after app switch");
            generateKeyEvent(KeyEvent.KEYCODE_HOME);
        } else {
            Logger.println("Press BACK after app switch");
            generateKeyBackEvent();
        }
        generateThrottleEvent(mThrottle);
    }

    private void startLogAction(long clockTimestamp, Action action) {
        ApeRRFormatter.startLogAction(mEventProduceLogger, action, clockTimestamp, mAgent.getTimestamp());
    }

    private void endLogAction(Action action) {
        ApeRRFormatter.endLogAction(mEventProduceLogger, action, mAgent.getTimestamp());
    }

    protected void generateThrottleEvent(long base) {
        long throttle = base;
        if (mRandomizeThrottle && (mThrottle > 0)) {
            throttle = mRandom.nextLong();
            if (throttle < 0) {
                throttle = -throttle;
            }
            throttle %= base;
            ++throttle;
        }
        if (throttle < 0) {
            throttle = -throttle;
        }
        addEvent(new MonkeyThrottleEvent(throttle));
    }

    public boolean validate() {
        return mHandlerThread.isAlive();
    }

    public void setVerbose(int verbose) {
        mVerbose = verbose;
    }

    public synchronized void requestClearPackage() {
        this.clearPackageOnGeneratingActivity = true;
    }


    public ComponentName randomlyPickMainApp() {
        int total = mMainApps.size();
        int index = mRandom.nextInt(total);
        ComponentName app = mMainApps.get(index);
        return app;
    }

    /**
     * generate an activity event
     */
    protected void generateActivityEvents(ComponentName app, boolean clearPackage) {
        int timestamp = this.mAgent.getTimestamp();
        boolean cleared = false;
        if (clearPackage) {
            clearPackage(app.getPackageName());
            cleared = true;
        } else {
            synchronized (this) {
                if (this.clearPackageOnGeneratingActivity) {
                    clearPackage(app.getPackageName());
                    cleared = true;
                    clearPackageOnGeneratingActivity = false;
                }
            }
        }
        if (timestamp == lastStartTimestamp && !cleared) {
            Logger.wformat("lastStartTimestamp [%d] is not updated. Try to clear package.", lastStartTimestamp);
            clearPackage(app.getPackageName());
            cleared = true;
        }
        MonkeyActivityEvent e = new MonkeyActivityEvent(app);
        addEvent(e);
        generateThrottleEvent(2000); // waiting for the loading of apps
        this.mAgent.startNewEpisode();
        Logger.iprintln("Let's wait for activity loading...");
        this.waitForActivity = true;
        if (mEventCount == 0) {
            this.waitForActivityFromClean = true;  // TODO: assume that the first activity is cleared
        } else {
            this.waitForActivityFromClean = cleared;
        }
        this.lastStartTimestamp = timestamp;
    }

    protected void checkAppActivity() {
        ComponentName cn = getTopActivityComponentName();
        if (cn == null) {
            clearEvent();
            startRandomMainApp();
            return;
        }
        String pkg = cn.getPackageName();
        boolean allow = MonkeyUtils.getPackageFilter().isPackageValid(pkg);
        if (allow) {
            if (this.waitForActivity) {
                Logger.iformat("Expected activity package [%s] is loaded...", pkg);
                // needed.
                mAgent.onAppActivityStarted(cn, this.waitForActivityFromClean);
                this.waitForActivity = false; // we found the activity we
                this.waitForActivityFromClean = false;
            }
            return;
        }
        if (this.waitForActivity) {
            Logger.iprintln("We are still waiting for activity loading. Let's wait for another 100ms...");
            generateThrottleEvent(100);
            return;
        }
        if (cn.getPackageName().equals("com.android.systemui")
                && cn.getClassName().equals("com.android.systemui.recents.RecentsActivity")) {
            if (hasEvent()) {
                Logger.dformat("The top component [%s] belongs to systemui.", cn);
                return;
            }
        } else {
            Logger.dformat("The top activity package %s is not allowed.", pkg);
        }
        mAgent.onActivityBlocked(cn);
        clearEvent();
        startRandomMainApp();
        return;
    }

    protected void generateClearEvent(GUITreeNode node) {
        Rect bounds = node.getBoundsInScreen();
        generateClickEventAt(bounds, LONG_CLICK_WAIT_TIME);
        generateKeyEvent(KeyEvent.KEYCODE_DEL);
    }

    int lastInputTimestamp;

    private void doInput(Action action, GUITreeNode node) {
        if (lastInputTimestamp == mAgent.getTimestamp()) {
            Logger.wprintln("checkVirtualKeyboard: Input only once.");
        } else {
            lastInputTimestamp = mAgent.getTimestamp();
        }
        String inputText = node.getInputText();
        if (inputText != null) {
            Logger.iprintln("Input text is " + inputText);
            // clearText(node);
            generateClearEvent(node);
            if (!AndroidDevice.sendText(inputText)) {
                attempToSendTextByKeyEvents(inputText);
            }
        } else {
            if (node.isEditText() || AndroidDevice.isVirtualKeyboardOpened()) {
                generateKeyEvent(KeyEvent.KEYCODE_ESCAPE);
            }
        }
    }

    private void attempToSendTextByKeyEvents(String inputText) {
        char[] szRes = inputText.toCharArray(); // Convert String to Char array

        KeyCharacterMap CharMap;
        if (Build.VERSION.SDK_INT >= 11) // My soft runs until API 5
            CharMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
        else
            CharMap = KeyCharacterMap.load(KeyCharacterMap.ALPHA);

        KeyEvent[] events = CharMap.getEvents(szRes);

        for (int i = 0; i < events.length; i += 2) {
            generateKeyEvent(events[i].getKeyCode());
        }
        generateKeyEvent(KeyEvent.KEYCODE_ENTER);
    }

    /**
     * if the queue is empty, we generate events first
     * 
     * @return the first event in the queue
     */
    public MonkeyEvent getNextEvent() {
        if (!(mAgent instanceof ReplayAgent)) {
            checkAppActivity();
        }
        if (!hasEvent()) {
            try {
                generateEvents();
            } catch (StopTestingException e) {
                clearEvent();
                return null;
            }
        }
        mEventCount++;
        MonkeyEvent e = popEvent();
        ApeRRFormatter.logConsume(mEventConsumeLogger, e);
        return e;
    }

    public Random getRandom() {
        return mRandom;
    }

    private ImageWriterQueue nextImageWriter() {
        return mImageWriters[mRandom.nextInt(mImageWriters.length)];
    }

    public boolean takeScreenshot(File screenshotFile) {
        Bitmap map = mUiAutomation.takeScreenshot();
        nextImageWriter().add(map, screenshotFile);
        return true;
    }

    public void takeScreenshot(String fileName) {
        takeScreenshot(new File(fileName));
    }

    public long getThrottle() {
        return this.mThrottle;
    }

    public Bitmap captureBitmap() {
        Bitmap map = mUiAutomation.takeScreenshot();
        return map;
    }
}
