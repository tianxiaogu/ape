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

import android.content.ComponentName;
import android.graphics.PointF;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManagerGlobal;
import android.os.SystemClock;
import android.os.HandlerThread;
import android.app.UiAutomation;
import android.app.UiAutomationConnection;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.MotionEvent;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

/**
 * monkey event queue
 */
public class MonkeySourceUiAutomatorDFS implements MonkeyEventSource {

    static interface NodeVisitor<E> {
        /**
         * return false if you need to stop the visiting
         * @param node
         * @return
         */
        boolean visit(Node node);
        E getData();
    }

    /**
     * Currently we use a map and array list based implementation.
     * @author t
     *
     */
    static class Node {
        int id;
        List<Node> children;
        Map<String, String> properties;
        Rect bounds = new Rect();
        public Node(int id, Map<String, String> properties, AccessibilityNodeInfo info) {
            this.id = id;
            this.properties = properties;
            children = new ArrayList<>();
            info.getBoundsInScreen(bounds);
        }
        public void addChild(Node node) {
            children.add(node);
        }
        public void toString(StringBuilder sb) {
            sb.append('{');
            for (Entry<String, String> entry : properties.entrySet()) {
                sb.append(entry.getKey());
                sb.append(':');
                sb.append(entry.getValue());
                sb.append(',');
            }
            sb.append("children:[");
            for (Node child : children) {
                child.toString(sb);
                sb.append(',');
            }
            sb.append("]}");
        }
        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb);
            return sb.toString();
        }

        public boolean visitNode(NodeVisitor visitor) {
            boolean cont = visitor.visit(this);
            if (cont) {
                for (Node child : children) {
                    cont = child.visitNode(visitor);
                    if (!cont) {
                        return cont;
                    }
                }
            }
            return cont;
        }


        public boolean isLongClickable() {
            return Boolean.valueOf(properties.get("longClickable"));
        }

        public boolean isClickable() {
            return Boolean.valueOf(properties.get("clickable"));
        }
    }

    static class State {
        Node root;
        String stateString;
        public State(Node node) {
            this.root = node;
            this.stateString = node.toString();
        }

        static State newInitialState() {
            return new State("<init>");
        }

        private State(String state) {
            this.stateString = state;
        }

        public Node getRoot() {
            return root;
        }

        public String getStateString() {
            return stateString;
        }

        public String toString() {
            return getStateString();
        }
        
        @Override
        public int hashCode() {
            return stateString.hashCode();
        }

        public void visitNode(NodeVisitor visitor) {
            root.visitNode(visitor);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            State other = (State) obj;
            return other.stateString.equals(stateString);
        }
    }

    static enum ActionType {
        START,
        BACK,
        CLICK,
        LONG_CLICK,
    }

    static class Action {
        State state;
        Node node;
        ActionType actionType;

        public Action(State state, Node node, ActionType actionType) {
            this.state = state;
            this.node = node;
            this.actionType = actionType;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((actionType == null) ? 0 : actionType.hashCode());
            result = prime * result + ((node == null) ? 0 : node.id);
            result = prime * result + ((state == null) ? 0 : state.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Action other = (Action) obj;
            if (actionType != other.actionType)
                return false;
            if (node == null) {
                if (other.node != null) {
                    return false;
                }
            } else if (other.node == null) {
                return false;
            } else if (node.id != other.node.id) {
                return false;
            }
            if (state == null) {
                if (other.state != null)
                    return false;
            } else if (!state.equals(other.state))
                return false;
            return true;
        }
    }

    static abstract class NodeBuilder {
        private int id = 0;
        
        abstract Map<String, String> buildAttributes(AccessibilityNodeInfo input);
        
        abstract Node buildNode(AccessibilityNodeInfo input);
        
        public Node buildTree(AccessibilityNodeInfo root) {
            Node rootNode = buildNode(root);
            for (int i = 0; i < root.getChildCount(); i++) {
                Node childNode = buildTree(root.getChild(i));
                rootNode.addChild(childNode);
            }
            return rootNode;
        }
        
        public int nextId() {
            return id++;
        }
        
        public int id() {
            return id;
        }
    }

    static class OnlyLayoutBuilder extends NodeBuilder {
        public Node buildNode(AccessibilityNodeInfo input) {
            Map<String, String> attributes = buildAttributes(input);
            return new Node(nextId(), attributes, input);
        }

        public Map<String, String> buildAttributes(AccessibilityNodeInfo input) {
            Map<String, String> attributes = new HashMap<String, String>();

            // Use string as key and must be put in the same order.
            // So, that they can be iterated in the same order.
            // Use string.valueOf
            attributes.put("className", String.valueOf(input.getClassName()));
            attributes.put("packageName", String.valueOf(input.getPackageName()));
            attributes.put("text", String.valueOf(input.getText()));
            attributes.put("viewIdResourceName", String.valueOf(input.getViewIdResourceName()));

            if (input.isCheckable()) {
                attributes.put("checkable", "true");
            }
            if (input.isClickable()) {
                attributes.put("clickable", "true");
            }
            if (input.isContentInvalid()) {
                attributes.put("contentInvalid", "true");
            }
            if (input.isContextClickable()) {
                attributes.put("contextClickable", "true");
            }
            if (input.isDismissable()) {
                attributes.put("dismissable", "true");
            }
            if (input.isEditable()) {
                attributes.put("editable", "true");
            }
            if (input.isLongClickable()) {
                attributes.put("longClickable", "true");
            }
            if (input.isPassword()) {
                attributes.put("password", "true");
            }
            if (input.isScrollable()) {
                attributes.put("scrollable", "true");
            }
/*            attributes.put("checkable", String.valueOf(input.isCheckable()));
            //attributes.put("checked", String.valueOf(input.isChecked()));
            attributes.put("clickable", String.valueOf(input.isClickable()));
            attributes.put("contentInvalid", String.valueOf(input.isContentInvalid()));
            attributes.put("contextClickable", String.valueOf(input.isContextClickable()));
            attributes.put("dismissable", String.valueOf(input.isDismissable()));
            attributes.put("editable", String.valueOf(input.isEditable()));
            attributes.put("longClickable", String.valueOf(input.isLongClickable()));
            attributes.put("multiLine", String.valueOf(input.isMultiLine()));
            attributes.put("password", String.valueOf(input.isPassword()));
            attributes.put("scrollable", String.valueOf(input.isScrollable()));*/

            return attributes;
        }

    }

    private List<ComponentName> mMainApps;
    private int mEventCount = 0;  //total number of events generated so far
    private MonkeyEventQueue mQ;
    private int mVerbose = 0;
    private long mThrottle = 0;
    private MonkeyPermissionUtil mPermissionUtil;
    private Random mRandom;

    private boolean mKeyboardOpen = false;

    private State currentState;
    private Action currentAction;
    private Set<State> states;
    private Map<State, Set<Action>> stateToTransitions;
    private Map<Action, Set<State>> transitionsToTargets;

    static <K1, K2, K3> K3 addToMapMap(Map<K1, Map<K2, K3>> map, K1 k1, K2 k2, K3 k3) {
        Map<K2, K3> result = map.get(k1);
        if (result == null) {
            result = new HashMap<>();
            map.put(k1, result);
        }
        return result.put(k2, k3);
    }
    
    static <K1, K2> boolean addToMapSet(Map<K1, Set<K2>> map, K1 k1, K2 k2) {
        Set<K2> result = map.get(k1);
        if (result == null) {
            result = new HashSet<>();
            map.put(k1, result);
        }
        return result.add(k2);
    }
    
    static <K1, K2> boolean inMapSet(Map<K1, Set<K2>> map, K1 k1) {
        Set<K2> result = map.get(k1);
        if (result == null) {
            return false;
        }
        return result.contains(k1);
    }
    
    /**
     * UiAutomation client and connection
     */
    protected final HandlerThread mHandlerThread = new HandlerThread("MonkeySourceUiAutomatorRandom");
    protected UiAutomation mUiAutomation;

    public static String getKeyName(int keycode) {
        return KeyEvent.keyCodeToString(keycode);
    }

    /**
     * Looks up the keyCode from a given KEYCODE_NAME.  NOTE: This may
     * be an expensive operation.
     *
     * @param keyName the name of the KEYCODE_VALUE to lookup.
     * @returns the intenger keyCode value, or KeyEvent.KEYCODE_UNKNOWN if not found
     */
    public static int getKeyCode(String keyName) {
        return KeyEvent.keyCodeFromString(keyName);
    }

    public static boolean hasKey(int key) {
        return KeyCharacterMap.deviceHasKey(key);
    }

    /**
     * Connect to AccessibilityService
     */
    public void connect() {
        if (mHandlerThread.isAlive()) {
            throw new IllegalStateException("Already connected!");
        }
        mHandlerThread.start();
        mUiAutomation = new UiAutomation(mHandlerThread.getLooper(),
                new UiAutomationConnection());
        mUiAutomation.connect();
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

    public MonkeySourceUiAutomatorDFS(Random random, List<ComponentName> MainApps,
            long throttle, boolean randomizeThrottle, boolean permissionTargetSystem) {
        mRandom = random;
        mMainApps = MainApps;
        mQ = new MonkeyEventQueue(random, throttle, randomizeThrottle);
        mPermissionUtil = new MonkeyPermissionUtil();
        mPermissionUtil.setTargetSystemPackages(permissionTargetSystem);

        currentState = State.newInitialState();
        states = new HashSet<>();
        states.add(currentState);
        currentAction = new Action(currentState, null, ActionType.START);
        stateToTransitions = new HashMap<>();
        transitionsToTargets = new HashMap<>();

        connect();
    }

    private PointF randomPoint(Random random, Display display) {
        return new PointF(random.nextInt(display.getWidth()), random.nextInt(display.getHeight()));
    }

    private PointF randomVector(Random random) {
        return new PointF((random.nextFloat() - 0.5f) * 50, (random.nextFloat() - 0.5f) * 50);
    }

    private void randomWalk(Random random, Display display, PointF point, PointF vector) {
        point.x = (float) Math.max(Math.min(point.x + random.nextFloat() * vector.x,
                display.getWidth()), 0);
        point.y = (float) Math.max(Math.min(point.y + random.nextFloat() * vector.y,
                display.getHeight()), 0);
    }


    /**
     * Get visible bounds of a given node.
     */
    public static Rect getVisibleBoundsInScreen(Rect nodeRect, int width, int height) {
        Rect displayRect = new Rect();
        displayRect.top = 0;
        displayRect.left = 0;
        displayRect.right = width;
        displayRect.bottom = height;

        nodeRect.intersect(displayRect);
        return nodeRect;
    }

    /**
     * Traverse the tree to obtain all clickable nodes.
     */
    private void collectClickable(List<AccessibilityNodeInfo> result, AccessibilityNodeInfo node) {
        if (node.isClickable()) {
            result.add(node);
        }
        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (child.isVisibleToUser()) {
                    collectClickable(result, child);
                    //child.recycle();
                }
            }
        }
    }

    protected void generateClickEventAt(AccessibilityNodeInfo node, long waitTime) {
        // targeted node's bounds
        Rect nodeRect = new Rect();
        node.getBoundsInScreen(nodeRect);

        generateClickEventAt(nodeRect, waitTime);
    }

    protected void generateClickEventAt(Rect nodeRect, long waitTime) {
        Display display = DisplayManagerGlobal.getInstance().getRealDisplay(Display.DEFAULT_DISPLAY);
        Point size = new Point();
        display.getSize(size);

        Rect bounds = getVisibleBoundsInScreen(nodeRect, size.x, size.y);
        PointF p1 = new PointF(bounds.exactCenterX(), bounds.exactCenterY());

        long downAt = SystemClock.uptimeMillis();

        mQ.addLast(new MonkeyTouchEvent(MotionEvent.ACTION_DOWN)
                .setDownTime(downAt)
                .addPointer(0, p1.x, p1.y)
                .setIntermediateNote(false));

        if (waitTime > 0) {
            MonkeyWaitEvent we = new MonkeyWaitEvent(waitTime);
            mQ.addLast(we);
        }

        mQ.addLast(new MonkeyTouchEvent(MotionEvent.ACTION_UP)
                .setDownTime(downAt)
                .addPointer(0, p1.x, p1.y)
                .setIntermediateNote(false));
    }

    protected void generateKeyBackEvent() {
        System.out.println("keyback event");
        generateKeyEvent(KeyEvent.KEYCODE_BACK);
    }

    /**
     * Generate a key event at specific key.
     */
    protected void generateKeyEvent(int key) {
//        if (!hasKey(key)) {
//            throw new IllegalStateException("Device has no key " + getKeyName(key));
//        }
        MonkeyKeyEvent e = new MonkeyKeyEvent(KeyEvent.ACTION_DOWN, key);
        mQ.addLast(e);

        e = new MonkeyKeyEvent(KeyEvent.ACTION_UP, key);
        mQ.addLast(e);
    }

    protected AccessibilityNodeInfo getRootInActiveWindow() {
        return mUiAutomation.getRootInActiveWindow();
    }

    protected void generateGUIEvent(AccessibilityNodeInfo info) {
        OnlyLayoutBuilder builder = new OnlyLayoutBuilder();
        Node rootNode = builder.buildTree(info);
        System.out.println("Tree has " + builder.id() + " nodes.");
        State state = new State(rootNode);
        boolean added = states.add(state);

        if (!added) {
            System.out.println("A visited state");
        }

        boolean ret = addToMapSet(transitionsToTargets, currentAction, state);
        if (ret) {
            System.out.println("A visited action");
        }
        currentState = state;

        Set<Action> actions = stateToTransitions.get(currentState);
        if (actions == null) {
            // This is the first time that we reach this state.
            NodeVisitor<Set<Action>> visitor = new NodeVisitor<Set<Action>>() { 
                Set<Action> actions = new HashSet<>();
                @Override
                public boolean visit(Node node) {
                    List<ActionType> types = getActionTypes(node);

                    for (ActionType type : types) {
                        Action action = new Action(currentState, node, type);
                        if (transitionsToTargets.containsKey(action)) {
                            throw new RuntimeException("Should not be true");
                        }
                        actions.add(action);
                    }

                    return true;
                }

                public Set<Action> getData() {
                    return actions;
                }
            };

            Action previous = currentAction;
            state.visitNode(visitor);
            actions = visitor.getData();
            stateToTransitions.put(state, actions);
        }
        
        if (actions.isEmpty()) {
            currentAction = new Action(currentState, null, ActionType.BACK);
            generateKeyBackEvent();
            return;
        }
        
        
        
        // Now we have a set of actions on this state
        // Do a BFS to find a path to an unvisited action.
        // invariant: if an action has been visited, then its targets should not be null.
        
        // a trivial case, there is an unvisited action in 1-step neighbor
        for (Action act : actions) {
            if (!transitionsToTargets.containsKey(act)) {
                generateEventForAction(act);
                currentAction = act;
                return;
            }
        }
        
        LinkedList<List<Action>> queue = new LinkedList<List<Action>>();
        Set<State> visited = new HashSet<State>();
        
        visited.add(state);
        for (Action act : actions) {
            List<Action> current = new ArrayList<Action>();
            current.add(act);
            queue.addLast(current);
        }
        
        List<Action> result = null;
        while (!queue.isEmpty()) {
            List<Action> current = queue.removeFirst();
            if (current.isEmpty()) {
                throw new RuntimeException("Should not be empty");
            }
            Action last = current.get(current.size() - 1);
            Set<State> targets = transitionsToTargets.get(last);
            if (targets == null) {
                result = current;
                break;
            }
            for (State s : targets) {
                if (visited.add(s)) {
                    Set<Action> to = stateToTransitions.get(s);
                    if (to == null) {
                        throw new RuntimeException("a visited state should not have transitions");
                    }
                    for (Action act : to) {
                        List<Action> newList = new ArrayList<Action>(current.size() + 1);
                        newList.addAll(current);
                        newList.add(act);
                        queue.addLast(newList);
                    }
                }
            }
        }
        
        if (result != null && result.size() > 0) {
            currentAction = result.get(0);
            generateEventForAction(result.get(0));
//            for (Action act : result) {
//                generateEventForAction(act);
//            }
            return;
        }
        
        // no event, we need backtrack
        currentAction = new Action(currentState, null, ActionType.BACK);
        generateKeyBackEvent();
    }
    
    protected void generateEventForAction(Action action) {
        // only generate new action
        switch (action.actionType) {
        case CLICK:
            generateClickEventAt(action.node.bounds, 0L);
            break;
        case LONG_CLICK:
            generateClickEventAt(action.node.bounds, 2000L);
            break;
        default:
            throw new RuntimeException("Should not reach here");
        }
    }

    public List<ActionType> getActionTypes(Node node) {
        List<ActionType> types = new ArrayList<ActionType>();
        if (node.isClickable()) {
            types.add(ActionType.CLICK);
        }
        if (node.isLongClickable()) {
            types.add(ActionType.LONG_CLICK);
        }
        return types;
    }

    /**
     * generate a random event based on mFactor
     */
    private void generateEvents() {
        if (!mQ.isEmpty()) {
            return;
        }
        AccessibilityNodeInfo info = getRootInActiveWindow();
        if (info != null) {
            generateGUIEvent(info);
        } else {
            System.err.println("ERROR: null root node returned by UiTestAutomationBridge, use default events generator.");
        }

        if (mQ.isEmpty()) {
            generateThrottleEvent(100);
        }
    }
    
    protected void generateThrottleEvent(long time) {
        mQ.addLast(new MonkeyThrottleEvent(time));
    }

    public boolean validate() {
        return mHandlerThread.isAlive();
    }

    public void setVerbose(int verbose) {
        mVerbose = verbose;
    }

    /**
     * generate an activity event
     */
    public void generateActivity() {
        MonkeyActivityEvent e = new MonkeyActivityEvent(mMainApps.get(
                mRandom.nextInt(mMainApps.size())));
        mQ.addLast(e);
    }

    /**
     * if the queue is empty, we generate events first
     * @return the first event in the queue
     */
    public MonkeyEvent getNextEvent() {
        if (mQ.isEmpty()) {
            generateEvents();
        }
        mEventCount++;
        MonkeyEvent e = mQ.getFirst();
        mQ.removeFirst();
        return e;
    }
}
