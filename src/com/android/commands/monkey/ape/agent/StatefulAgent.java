/*
 * Copyright 2020 Advanced Software Technologies Lab at ETH Zurich, Switzerland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.commands.monkey.ape.agent;

import static com.android.commands.monkey.ape.utils.Config.activityStableRestartThreshold;
import static com.android.commands.monkey.ape.utils.Config.baseThrottle;
import static com.android.commands.monkey.ape.utils.Config.enableXPathAction;
import static com.android.commands.monkey.ape.utils.Config.evolveModel;
import static com.android.commands.monkey.ape.utils.Config.fuzzingActivityVisitThreshold;
import static com.android.commands.monkey.ape.utils.Config.graphStableRestartThreshold;
import static com.android.commands.monkey.ape.utils.Config.maxExtraPriorityAliasedActions;
import static com.android.commands.monkey.ape.utils.Config.maxThrottle;
import static com.android.commands.monkey.ape.utils.Config.saveDotGraph;
import static com.android.commands.monkey.ape.utils.Config.saveGUITreeToXmlEveryStep;
import static com.android.commands.monkey.ape.utils.Config.saveObjModel;
import static com.android.commands.monkey.ape.utils.Config.saveStates;
import static com.android.commands.monkey.ape.utils.Config.saveVisGraph;
import static com.android.commands.monkey.ape.utils.Config.stateStableRestartThreshold;
import static com.android.commands.monkey.ape.utils.Config.takeScreenshot;
import static com.android.commands.monkey.ape.utils.Config.takeScreenshotForEveryStep;
import static com.android.commands.monkey.ape.utils.Config.takeScreenshotForNewState;
import static com.android.commands.monkey.ape.utils.Config.throttleForActivityTransition;
import static com.android.commands.monkey.ape.utils.Config.throttleForUnvisitedAction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.android.commands.monkey.MonkeySourceApe;
import com.android.commands.monkey.ape.ActionFilter;
import com.android.commands.monkey.ape.BadStateException;
import com.android.commands.monkey.ape.BaseActionFilter;
import com.android.commands.monkey.ape.Subsequence;
import com.android.commands.monkey.ape.model.Action;
import com.android.commands.monkey.ape.model.ActionCounters;
import com.android.commands.monkey.ape.model.ActionType;
import com.android.commands.monkey.ape.model.ActivityNode;
import com.android.commands.monkey.ape.model.Graph;
import com.android.commands.monkey.ape.model.GraphListener;
import com.android.commands.monkey.ape.model.Model;
import com.android.commands.monkey.ape.model.Model.ActionRecord;
import com.android.commands.monkey.ape.model.xpathaction.XPathActionController;
import com.android.commands.monkey.ape.model.ModelAction;
import com.android.commands.monkey.ape.model.State;
import com.android.commands.monkey.ape.model.StateKey;
import com.android.commands.monkey.ape.model.StateTransition;
import com.android.commands.monkey.ape.naming.Name;
import com.android.commands.monkey.ape.naming.Naming;
import com.android.commands.monkey.ape.tree.GUITree;
import com.android.commands.monkey.ape.tree.GUITreeAction;
import com.android.commands.monkey.ape.tree.GUITreeBuilder;
import com.android.commands.monkey.ape.tree.GUITreeNode;
import com.android.commands.monkey.ape.tree.GUITreeWidgetDiffer;
import com.android.commands.monkey.ape.utils.Logger;
import com.android.commands.monkey.ape.utils.Utils;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;

public abstract class StatefulAgent extends ApeAgent implements GraphListener {

    private static final boolean debug = false;


    protected State lastState;
    protected GUITree lastGUITree;
    protected ModelAction lastAction;
    protected GUITreeAction lastGUITreeAction;

    protected State currentState;
    protected GUITree currentGUITree;
    protected ModelAction currentAction;
    protected GUITreeAction currentGUITreeAction;

    protected State newState;
    protected GUITree newGUITree;
    protected ModelAction newAction;
    protected GUITreeAction newGUITreeAction;

    protected StateTransition currentStateTransition;

    protected Model model;

    private ActionCounters actionCounters = new ActionCounters();

    private LinkedList<StateTransition> actionBuffer = new LinkedList<StateTransition>();

    protected int graphStableCounter;
    protected int stateStableCounter;
    protected int activityStableCounter;
    private boolean appActivityJustStarted;


    protected GUITreeWidgetDiffer widgetDiffer = new GUITreeWidgetDiffer();

    protected ActionFilter validatedActionFilter = new BaseActionFilter() {
        @Override
        public boolean include(ModelAction action) {
            return validateNewAction(action) != null;
        }
    };
    private Set<State> refreshStatesCheckingBlacklist = new HashSet<>();
    private boolean currentStateRecovered;
    private boolean appActivityJustStartedFromClean;

    public StatefulAgent(MonkeySourceApe ape, Graph graph) {
        super(ape);
        graph.addListener(this);
        this.model = new Model(graph);
        this.timestamp = graph.getTimestamp();
    }

    public void updateModel(Model newModel) {
        this.model = newModel;
        if (currentState != null) {
            currentState = model.update(currentGUITree);
        }
        if (currentAction != null) {
            currentAction = model.update(currentAction, currentGUITreeAction);
        }
        if (lastState != null) {
            lastState = model.update(lastGUITree);
        }
        if (lastAction != null) {
            lastAction = model.update(lastAction, lastGUITreeAction);
        }
        if (newState != null) {
            newState = model.update(newGUITree);
        }
        if (newAction != null) {
            newAction = model.update(newAction, newGUITreeAction);
        }
        if (!actionBuffer.isEmpty()) {
            Logger.println("Update action buffer...");
            LinkedList<StateTransition> newBuffer = new LinkedList<>();
            for (StateTransition st : actionBuffer) {
                Logger.println("Updating " + st);
                st = newModel.update(st);
                Logger.println("Updated " + st);
                newBuffer.add(st);
            }
            actionBuffer = newBuffer;
        }
        List<ActionRecord> actionHistory = getActionHistory();
        final int size = actionHistory.size();
        for (int i = 0; i < size; i++) {
            ActionRecord actionPair = actionHistory.get(i);
            Action action = actionPair.modelAction;
            GUITreeAction guiAction = actionPair.guiAction;
            if (action.isModelAction() && action.requireTarget()) {
                if (guiAction == null) {
                    throw new RuntimeException("Sanity check failed!");
                }
                action = newModel.update((ModelAction)action, guiAction);
                updateActionHistory(i, new ActionRecord(actionPair.clockTimestamp,
                        actionPair.agentTimestamp, action, guiAction));
            }
        }
    }

    public State getLastState() {
        return lastState;
    }

    public void setLastState(State lastState) {
        this.lastState = lastState;
    }

    public GUITree getLastGUITree() {
        return lastGUITree;
    }

    public void setLastGUITree(GUITree lastGUITree) {
        this.lastGUITree = lastGUITree;
    }

    public ModelAction getLastAction() {
        return lastAction;
    }

    public void setLastAction(ModelAction lastAction) {
        this.lastAction = lastAction;
    }

    public GUITreeAction getLastGUITreeAction() {
        return lastGUITreeAction;
    }

    public void setLastGUITreeAction(GUITreeAction lastGUITreeAction) {
        this.lastGUITreeAction = lastGUITreeAction;
    }

    public State getCurrentState() {
        return currentState;
    }

    public void setCurrentState(State currentState) {
        this.currentState = currentState;
    }

    public GUITree getCurrentGUITree() {
        return currentGUITree;
    }

    public void setCurrentGUITree(GUITree currentGUITree) {
        this.currentGUITree = currentGUITree;
    }

    public GUITreeAction getCurrentGUITreeAction() {
        return currentGUITreeAction;
    }

    public void setCurrentGUITreeAction(GUITreeAction currentGUITreeAction) {
        this.currentGUITreeAction = currentGUITreeAction;
    }

    public State getNewState() {
        return newState;
    }

    public void setNewState(State newState) {
        this.newState = newState;
    }

    public GUITree getNewGUITree() {
        return newGUITree;
    }

    protected ModelAction checkFuzzing(ModelAction action) {
        if (!action.requireTarget()) {
            if (!action.isBack()) {
                return action;
            }
        }
        if (action.getState() == null) {
            return action;
        }
        ActivityNode an = getGraph().getActivityNode(action.getState().getActivity());
        if (an == null) {
            return action;
        }
        if (an.getVisitedCount() < fuzzingActivityVisitThreshold) {
            this.disableFuzzing = true;
        }
        return action;
    }

    public void setNewGUITree(GUITree newGUITree) {
        this.newGUITree = newGUITree;
    }

    public void setNewAction(ModelAction newAction) {
        this.newAction = newAction;
    }

    public GUITreeAction getNewGUITreeAction() {
        return newGUITreeAction;
    }

    public void setNewGUITreeAction(GUITreeAction newGUITreeAction) {
        this.newGUITreeAction = newGUITreeAction;
    }

    public StateTransition getCurrentStateTransition() {
        return currentStateTransition;
    }

    public void setCurrentStateTransition(StateTransition currentStateTransition) {
        this.currentStateTransition = currentStateTransition;
    }

    public void setCurrentAction(ModelAction currentAction) {
        this.currentAction = currentAction;
    }

    public abstract void onBufferLoss(State actual, State expected);

    public abstract void onRefillBuffer(Subsequence path);

    protected void assertEmptyActionBuffer() {
        if (!actionBuffer.isEmpty()) {
            throw new IllegalStateException("Try actions in the buffer first");
        }
    }

    protected void clearBuffer() {
        if (!actionBuffer.isEmpty()) {
            StateTransition transition = actionBuffer.removeFirst();
            getGraph().weakenStateTransition(transition.getSource(), transition.getAction(), transition.getTarget());
            actionBuffer.clear();
        }
    }

    public int actionBufferSize() {
        return this.actionBuffer.size();
    }

    protected ModelAction refillBuffer(Subsequence seq) {
        onRefillBuffer(seq);
        clearBuffer();
        return fillBuffer(seq);
    }

    protected ModelAction fillBuffer(Subsequence seq) {
        seq.fillBuffer(actionBuffer);
        ModelAction action = seq.getFirstAction();
        int throttle = Math.max(action.getThrottle(), actionBuffer.peekFirst().getThrottle());
        action.setThrottle(throttle);
        return action;
    }

    public Graph getGraph() {
        return model.getGraph();
    }

    /**
     * Verify last action and new state
     * 
     * @return
     */
    protected ModelAction selectNewActionFromBuffer() {
        if (enableXPathAction) {
            ModelAction xpathAction = XPathActionController.selectAction(newState, newGUITree);
            if (xpathAction != null) {
                return xpathAction;
            }
        }
        if (actionBuffer.isEmpty()) {
            return null;
        }
        StateTransition t = actionBuffer.removeFirst();
        ModelAction expectedCurrentAction = t.action;
        State expectedNewState = t.target;
        if (!expectedCurrentAction.equals(currentAction)) {
            Logger.iprintln("Inconsistent actions in action buffer: expected " + expectedCurrentAction + ", get "
                    + currentAction);
            clearBuffer();
            return null;
        }
        if (expectedNewState != null && !expectedNewState.equals(newState)) {
            Logger.iprintln("Inconsistent states in action buffer: expected " + expectedNewState + ", get " + newState);
            widgetDiffer.diff(newState, expectedNewState);
            widgetDiffer.print();
            if (!actionBuffer.isEmpty() && currentStateTransition.isSameActivity()) {
                // Two states have the same activity and have the same actions sets.
                ModelAction action = actionBuffer.peekFirst().action;
                ModelAction relocatedAction = newState.relocate(action);
                if (relocatedAction == null) {
                    getGraph().weakenStateTransition(currentState, currentAction, expectedNewState);
                    onBufferLoss(newState, expectedNewState);
                } else {
                    Logger.format("Relocate %s to %s ", action, relocatedAction);
                    int throttle = Math.max(relocatedAction.getThrottle(), actionBuffer.peekFirst().getThrottle()); // transition throttle
                    Logger.iformat("Buffer action throttle: original: %d, tracked: %d", relocatedAction.getThrottle(), actionBuffer.peekFirst().getThrottle());
                    relocatedAction.setThrottle(throttle);
                }
                clearBuffer();
                return relocatedAction;
            }
            getGraph().weakenStateTransition(currentState, currentAction, expectedNewState);
            onBufferLoss(newState, expectedNewState);
            clearBuffer();
            return null;
        }
        if (actionBuffer.isEmpty()) {
            return null;
        }
        ModelAction action = actionBuffer.peekFirst().action;
        Logger.iprintln("Peek an action from buffer " + action);
        ModelAction check;
        if (action.getTarget() != null) {
            check = newState.getAction(action.getTarget(), action.getType());
        } else {
            check = newState.getAction(action.getType());
        }
        if (check != action) {
            expectedNewState.dumpState();
            newState.dumpState();
            clearBuffer();
            return null;
        }
        int throttle = Math.max(action.getThrottle(), actionBuffer.peekFirst().getThrottle()); // transition throttle
        Logger.iformat("Buffer action throttle: original: %d, tracked: %d", action.getThrottle(), actionBuffer.peekFirst().getThrottle());
        action.setThrottle(throttle);
        return action;
    }

    public Rect getCurrentRootNodeBounds() {
        if (currentState == null) {
            return null;
        }
        return currentState.getLatestGUITree().getRootNode().getBoundsInScreen();
    }

    public ModelAction getCurrentAction() {
        return this.currentAction;
    }

    protected void resetTrace() {
        this.clearBuffer();
        this.currentStateTransition = null;
        this.currentState = null;
        this.currentAction = null;
        this.currentGUITree = null;
        this.currentGUITreeAction = null;
        this.newState = null;
        this.newAction = null;
        this.newGUITree = null;
        this.newGUITreeAction = null;
        this.lastAction = null;
        this.lastGUITree = null;
        this.lastGUITreeAction = null;
        this.lastState = null;
    }

    @Override
    public void startNewEpisode() {
        resetTrace();
        this.graphStableCounter = 0;
        this.stateStableCounter = 0;
        this.activityStableCounter = 0;
    }

    @Override
    public boolean appCrashed(String arg0, int arg1, String arg2, String arg3, long arg4, String arg5) {
        return super.appCrashed(arg0, arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public int appEarlyNotResponding(String arg0, int arg1, String arg2) {
        return super.appEarlyNotResponding(arg0, arg1, arg2);
    }

    @Override
    public int appNotResponding(String arg0, int arg1, String arg2) {
        return super.appNotResponding(arg0, arg1, arg2);
    }

    Bitmap captureBitmap() {
        return ape.captureBitmap();
    }

    protected State refreshNewState() {
        ComponentName topComp = newState.getLatestGUITree().getActivityName();
        int retry = 5;
        while (--retry >= 0) {
            Logger.iformat("Checking new state: %s, iteration: #%d", newState, retry);
            long begin = SystemClock.elapsedRealtimeNanos();
            AccessibilityNodeInfo newInfo = ape.getRootInActiveWindowSlow();
            long end = SystemClock.elapsedRealtimeNanos();
            Logger.iformat("getRootInActiveWindowSlow takes %d ms.", TimeUnit.NANOSECONDS.toMillis(end - begin));
            State newNewState = null;
            if (newInfo == null) {
                continue;
            }
            Bitmap newNewBitmap = captureBitmap();
            newNewState = buildState(topComp, newInfo, newNewBitmap); // this will append a new tree to
            if (newNewState == null) {
                continue;
            }
            if (!newNewState.equals(newState)) {
                if (newState.isUnvisited()) {
                    if (!getGraph().remove(newState).isEmpty()) {
                        throw new RuntimeException("An unvisited state has non-empty transitions.");
                    }
                }
                newState = newNewState;
                return newState;
            } else {
                GUITree removed = newState.removeLastLastGUITree();
                if (removed == null) {
                    throw new IllegalStateException("At least two GUI trees.");
                }
                GUITreeBuilder.release(removed);
                model.release(removed);
            }
            if (TimeUnit.NANOSECONDS.toSeconds(end - begin) >= 10) {
                break;
            }
        }
        return null;
    }

    protected void checkAndRefreshNewState() {
        State oldNewState = newState;
        ComponentName topComp = newState.getLatestGUITree().getActivityName();
        if (refreshStatesCheckingBlacklist.contains(newState)) {
            Logger.iformat("State %s is in blacklist for refresh check.", newState);
            return;
        }
        int retry = 5;
        while (--retry >= 0) {
            Logger.iformat("Checking new state: %s, iteration: #%d", newState, retry);
            if (!newState.isTrivialState()) {
                break;
            }
            long begin = SystemClock.elapsedRealtimeNanos();
            AccessibilityNodeInfo newInfo = ape.getRootInActiveWindowSlow();
            long end = SystemClock.elapsedRealtimeNanos();
            Logger.iformat("getRootInActiveWindowSlow takes %d ms.", TimeUnit.NANOSECONDS.toMillis(end - begin));
            State newNewState = null;
            if (newInfo == null) {
                continue;
            }
            Bitmap newNewBitmap = captureBitmap();
            newNewState = buildState(topComp, newInfo, newNewBitmap); // this will append a new tree to
            if (newNewState == null) {
                continue;
            }
            if (!newNewState.equals(newState)) {
                if (newState.isUnvisited()) {
                    Set<StateTransition> transitions = getGraph().remove(newState);
                    if (!transitions.isEmpty()) {
                        Logger.iformat("Non empty transitions on unvisited states: %s", newState);
                        for (StateTransition st : transitions) {
                            Logger.iformat("- %s", st);
                        }
                        throw new RuntimeException("An unvisited state has non-empty transitions.");
                    }
                }
                newState = newNewState;
                retry = Math.min(retry, 0); // at most try once
            } else {
                GUITree removed = newState.removeLastLastGUITree();
                if (removed == null) {
                    throw new IllegalStateException("At least two GUI trees.");
                }
                GUITreeBuilder.release(removed);
                model.release(removed);
                GUITree last = newState.getLatestGUITree();
                if (isTopNamingEquivalent(removed, last)) {
                    Logger.iprintln("Checking trivial new state: top naming equivalent.");
                    retry = Math.min(2, retry); // at most try twice
                } else {
                    Logger.iprintln("Checking trivial new state: NOT top naming equivalent.");
                }
            }
            if (TimeUnit.NANOSECONDS.toSeconds(end - begin) >= 10) {
                break;
            }
        }
        if (oldNewState == newState) {
            Logger.iformat("State %s is blacklisted from refresh check.", newState);
            refreshStatesCheckingBlacklist.add(newState);
        }
    }

    private boolean isTopNamingEquivalent(GUITree tree1, GUITree tree2) {
        Naming naming = model.getNamingManager().getTopNaming();
        StateKey state1 = GUITreeBuilder.getStateKey(naming, tree1);
        StateKey state2 = GUITreeBuilder.getStateKey(naming, tree2);
        return state1.equals(state2);
    }

    protected void preCheckTrivialNewState() {
        if (newState.isTrivialState()) {
            State oldNewState = newState;
            checkAndRefreshNewState();
            if (oldNewState != newState) {
                Logger.iformat("New (trivial) state is updated from %s to %s.", oldNewState, newState);
            }
        }
    }

    protected State buildAndValidateNewState(ComponentName topComp, AccessibilityNodeInfo info) {
        newState = buildState(topComp, info, captureBitmap());
        preCheckTrivialNewState();
        validateAllNewActions();
        newGUITree = newState.getLatestGUITree();
        newGUITree.setTimestamp(getTimestamp());
        return newState;
    }

    /**
     * 
     */
    protected Action updateStateInternal(ComponentName topComp, AccessibilityNodeInfo info) {
        recoverCurrentState();
        buildAndValidateNewState(topComp, info);
        preEvolveModel();
        getGraph().markVisited(newState, getTimestamp());
        saveGUI();
        updateGraph();
        // checkCircleTransition();
        checkNonDeterministicTransitions();
        if (newState.isUnvisited()) {
            getGraph().markVisited(newState, getTimestamp());
        }
        Action action = resolveNewAction();
        if (action.isModelAction()) {
            getGraph().markVisited((ModelAction) action, getTimestamp());
            moveForward();
        } else {
            this.resetTrace();
        }
        if (debug) {
            return Action.NOP;
        }
        return action; // newAction are moved to currentAction in moveForward
    }

    public void notifyActionConsumed() {
        GUITree.releaseLoadedData();
    }

    protected void checkNonDeterministicTransitions() {
        if (!evolveModel) {
            return;
        }
        if (currentStateTransition == null) {
            return;
        }
        if (currentStateRecovered) {
            return;
        }
        Model newModel = model.resolveNonDeterministicTransitions(currentStateTransition);
        if (newModel != null) {
            Logger.iprintln("Model has been refined, reset stateful..");
            updateModel(newModel);
            validateAllNewActions();
        }
    }

    protected State checkUnderAbstractedState() {
        Naming naming = newState.getCurrentNaming();
        if (naming.getParent() == null) {
            return newState;
        }
        int iteration = 0;
        while (true) {
            Logger.iformat("Check under-abstracted states %s: #%d", newState, iteration++);
            State state = newState;
            checkAndAbstractUnderAbstractedState();
            if (state == newState) {
                break;
            }
        }
        return newState;
    }

    protected void checkAndAbstractUnderAbstractedState() {
        Naming naming = newState.getCurrentNaming();
        Naming parentNaming = naming;
        int iteration = 0;
        while (parentNaming.getParent() != null) {
            Set<State> states = getGraph().getAllStates(parentNaming);
            Logger.iformat("Check under-abstracted states collected %d targets for naming %s. The state is %s. #%d",
                    states.size(), parentNaming, newState, iteration++);
            Model newModel = model.stateAbstraction(naming, newState, parentNaming, states);
            if (newModel != null) {
                model = newModel;
                updateModel(model);
                validateAllNewActions();
                return;
            }
            parentNaming = parentNaming.getParent();
        }
    }

    protected void preEvolveModel() {
        if (!evolveModel) {
            return;
        }
        {
            long begin = SystemClock.elapsedRealtimeNanos();
            checkUnderAbstractedState();
            long end = SystemClock.elapsedRealtimeNanos();
            Logger.iformat("Pre-checking under-abstracted states takes %d ms", TimeUnit.NANOSECONDS.toMillis(end - begin));
        }
        {
            long begin = SystemClock.elapsedRealtimeNanos();
            checkOverAbstractedState();
            long end = SystemClock.elapsedRealtimeNanos();
            Logger.iformat("Checking over-abstracted states takes %d ms", TimeUnit.NANOSECONDS.toMillis(end - begin));
        }
        {
            long begin = SystemClock.elapsedRealtimeNanos();
            checkUnderAbstractedState();
            long end = SystemClock.elapsedRealtimeNanos();
            Logger.iformat("Post-checking under-abstracted states takes %d ms", TimeUnit.NANOSECONDS.toMillis(end - begin));
        }
    }

    protected State checkOverAbstractedState() {
        int iteration = 0;
        while (true) {
            Logger.iformat("Check over-abstracted states %s: #%d", newState, iteration++);
            State state = newState;
            checkAndRefineOverAbstractedState();
            if (state == newState) {
                break;
            }
        }
        return newState;
    }

    static int compareArrays(Object[] a1, Object[] a2) {
        if (a1 == null) {
            if (a2 == null) {
                return 0;
            }
            return 1;
        }
        if (a2 == null) {
            return -1;
        }
        return a1.length - a2.length;
    }

    protected void checkAndRefineOverAbstractedState() {
        List<ModelAction> actions = newState.targetedActions();
        Collections.sort(actions, new Comparator<ModelAction>() {

            @Override
            public int compare(ModelAction o1, ModelAction o2) {
                if (o1.requireTarget() && o2.requireTarget()) {
                    return compareArrays(o1.getResolvedNodes(), o2.getResolvedNodes());
                }
                if (!o1.requireTarget() && !o2.requireTarget()) {
                    return o1.getType().compareTo(o2.getType());
                }
                if (o1.requireTarget() && !o2.requireTarget()) {
                    return 1;
                }
                if (!o1.requireTarget() && o2.requireTarget()) {
                    return -1;
                }
                return 0;
            }

        });
        Set<Name> names = new HashSet<>();
        for (ModelAction action : actions) {
            if (!action.requireTarget()) {
                continue;
            }
            if (names.contains(action.getTarget())) {
                continue;
            }
            names.add(action.getTarget());
            Model newModel = model.actionRefinement(action);
            if (newModel != null) {
                model = newModel;
                updateModel(model);
                validateAllNewActions();
                return;
            }
        }
    }

    @Override
    public boolean activityStarting(Intent intent, String pkg) {
        boolean allow = super.activityStarting(intent, pkg);
        return allow;
    }

    protected void saveGUI() {
        if (saveGUITreeToXmlEveryStep) {
            checkOutputDir();
            File xmlFile = new File(checkOutputDir(), String.format("step-%d.xml", getTimestamp()));
            Logger.iformat("Saving GUI tree to %s at step %d", xmlFile, getTimestamp());
            try {
                Utils.saveXml(xmlFile.getAbsolutePath(), newGUITree.getDocument());
            } catch (Exception e) {
                e.printStackTrace();
                Logger.wformat("Fail to save GUI tree to %s at step %d", xmlFile, getTimestamp());
            }
        }
        if (takeScreenshot && takeScreenshotForEveryStep) {
            checkOutputDir();
            File screenshotFile = new File(checkOutputDir(), String.format("step-%d.png", getTimestamp()));
            Logger.iformat("Saving screen shot to %s at step %d", screenshotFile, getTimestamp());
            ape.takeScreenshot(screenshotFile);
        }
    }

    protected State buildState(ComponentName topComp, AccessibilityNodeInfo rootInfo, Bitmap bitmap) {
        return model.getState(topComp, rootInfo, bitmap);
    }

    public void onAppActivityStarted(ComponentName app, boolean clean) {
        String className = app.getClassName();
        Logger.iprintln("App Activity " + className + " started.");
        appActivityJustStarted = true;
        appActivityJustStartedFromClean = clean;
    }

    protected void recoverCurrentState() {
        currentStateRecovered = false;
        if (currentState != null) {
            return;
        }
        List<ActionRecord> history = getActionHistory();
        if (history.isEmpty()) {
            return;
        }
        ActionRecord record = null;
        for (int index = history.size() - 1; index >= 0; index--) {
            record = history.get(index);
            if (record.modelAction.canStartApp()) {
                // do nothing if is start
                return;
            }
            if (record.modelAction.isModelAction()) {
                break;
            }
        }
        if (record == null || !record.modelAction.isModelAction()) {
            return; // no valid action
        }
        ModelAction modelAction = (ModelAction) record.modelAction;
        GUITreeAction guiAction = record.guiAction;
        currentState = modelAction.getState();
        currentAction = modelAction;
        currentGUITree = guiAction.getGUITree();
        currentGUITreeAction = guiAction;
        Logger.iprintln("Recover current states and actions...");
        Logger.iformat("> recovered current state: %s", currentState);
        Logger.iformat("> recovered current action: %s", currentAction);
        currentStateRecovered = true;
    }

    protected void updateGraph() {
        if (currentState == null) {
            if (this.appActivityJustStarted) {
                Logger.iformat("Entry state: %s", newState);
                model.getGraph().addEntryGUITree(newGUITree);
                if (appActivityJustStartedFromClean) {
                    model.getGraph().addCleanEntryGUITree(newGUITree);
                }
                this.appActivityJustStartedFromClean = false;
                this.appActivityJustStarted = false;
            }
        }
        currentStateTransition = model.addTransition(currentState, currentAction, newState, currentGUITree,
                currentGUITreeAction, newGUITree);
        checkStable();
    }

    protected void checkStable() {
        Logger.format("Graph Stable Counter: graph (%d), state (%d), activity (%d)", graphStableCounter,
                stateStableCounter, activityStableCounter);
        if (graphStableCounter > 0) {
            if (onGraphStable(graphStableCounter)) {
                graphStableCounter = 0;
            }
        }
        if (stateStableCounter > 0) {
            if (onStateStable(stateStableCounter)) {
                stateStableCounter = 0;
            }
        }
        if (activityStableCounter > 0) {
            if (onActivityStable(activityStableCounter)) {
                activityStableCounter = 0;
            }
        }
    }

    public boolean onActivityStable(int counter) {
        if (counter > activityStableRestartThreshold) {
            Logger.format("Activity is stable for %d", counter);
            requestRestart();
            return true;
        }
        return false;
    }

    @Override
    public boolean onGraphStable(int counter) {
        if (counter > graphStableRestartThreshold) {
            Logger.format("Graph is stable for %d", counter);
            requestRestart();
            return true;
        }
        return false;
    }

    @Override
    public void onActivityStopped() {
        this.currentAction = null;
        this.currentState = null;
        this.newAction = null;
        this.newState = null;
        this.lastAction = null;
        this.lastState = null;
        clearBuffer();
        clearCounters();
    }

    private void clearCounters() {
        graphStableCounter = 0;
        stateStableCounter = 0;
        activityStableCounter = 0;
    }

    @Override
    public boolean onStateStable(int counter) {
        if (counter > stateStableRestartThreshold) {
            Logger.format("State is stable for %d", counter);
            requestRestart();
            return true;
        }
        return false;
    }

    protected void doMoveForward() {
        // Do switch
        Logger.format("Last  state: %s", lastState);
        Logger.format("Last action: %s", lastAction);
        Logger.format("Curr  state: %s", currentState);
        Logger.format("Curr action: %s", currentAction);
        Logger.format("New   state: %s", newState);
        Logger.format("New  action: %s", newAction);

        lastState = currentState;
        lastAction = currentAction;
        lastGUITree = currentGUITree;
        lastGUITreeAction = currentGUITreeAction;

        currentState = newState;
        currentAction = newAction;
        currentGUITree = newGUITree;
        currentGUITreeAction = newGUITreeAction;

        newState = null;
        newAction = null;
        newGUITree = null;
        newGUITreeAction = null;
        currentStateTransition = null;
    }

    protected void moveForward() {
        doMoveForward();
    }

    public void onAddNode(State node) {
        if (takeScreenshot && takeScreenshotForNewState) {
            checkOutputDir();
            String id = node.getGraphId();
            File screenshotFile = new File(checkOutputDir(), String.format("%s.png", id));
            Logger.format("Saving screen shot for new state %s to %s", id, screenshotFile);
            ape.takeScreenshot(screenshotFile);
        }
    }

    @Override
    public void onVisitStateTransition(StateTransition edge) {
        currentStateTransition = edge;
        switch (edge.getType()) {
        case NEW_ACTION:
        case NEW_ACTION_TARGET:
            graphStableCounter = 0;
            break;
        case EXISTING:
            graphStableCounter++;
            break;
        }
        if (edge.isCircle() && edge.getTheta() == 0) {
            stateStableCounter++;
        } else {
            stateStableCounter = 0;
        }
        if (edge.isSameActivity()) {
            activityStableCounter++;
        } else {
            activityStableCounter = 0;
        }
    }

    protected ModelAction validateNewAction(ModelAction action) {
        if (action == null) {
            return null;
        }
        action = newState.resolveAction(this, action, getThrottleForNewAction(newState, action));
        if (ape.validateResolvedAction(action)) {
            action.setValid(true);
            return action;
        }
        Logger.wformat("Mark an action (%s) invalid", action);
        action.setValid(false);
        return null;
    }

    protected void validateAllNewActions() {
        Utils.assertNotNull(newState);
        for (ModelAction action : newState.getActions()) {
            validateNewAction(action);
        }
    }

    protected Action resolveNewAction() {
        Utils.assertNotNull(newState);
        adjustActionsByGUITree();
        Action action = selectNewActionNonnull();
        Utils.assertNotNull(action);
        if (action.isModelAction()) {
            newAction = (ModelAction) action;
            newGUITreeAction = newAction.getResolvedGUITreeAction();
            Utils.assertNotNull(newGUITreeAction);
            return newAction;
        } else {
            return action;
        }
    }


    /**
     * Empirical priority.
     * @param actionType
     * @return
     */
    protected int getActionBasePriority(ActionType actionType) {
        switch (actionType) {
        case MODEL_CLICK:
            return 4;
        case MODEL_LONG_CLICK:
            return 2;
        case MODEL_SCROLL_TOP_DOWN:
            return 2;
        case MODEL_SCROLL_BOTTOM_UP:
            return 3;
        case MODEL_SCROLL_LEFT_RIGHT:
            return 3;
        case MODEL_SCROLL_RIGHT_LEFT:
            return 2;
        default:
            return 1;
        }
    }

    protected void adjustActionsByGUITree() {
        // Rect displayBounds = ape.getDisplayBounds();
        for (ModelAction action : newState.getActions()) {
            int basePriority = getActionBasePriority(action.getType()) << 3;
            action.setPriority(basePriority);
            if (!action.requireTarget()) {
                if (action.isUnvisited()) {
                    int priority = action.getPriority();
                    priority += 5;
                    action.setPriority(priority);
                }
                continue;
            }
            if (!action.isValid()) {
                continue;
            }
            if (!action.isResolvedAt(timestamp)) {
                continue;
            }
            GUITreeNode node = action.getResolvedNode();
            action.setEnabled(node.isEnabled());
            Collection<StateTransition> edges = getGraph().getOutStateTransitions(action);
            int priority = action.getPriority();
            if (action.isUnvisited()) {
                priority += 20; // Select unvisited priority
            }
            if (!action.isSaturated()) {
                List<GUITreeNode> nodes = newGUITree.getNodes(action.getTarget());
                int size = nodes.size();
                if (size > 1) {
                    priority += Math.min(size, maxExtraPriorityAliasedActions) * getActionBasePriority(action.getType());
                }
            }
            for (StateTransition edge : edges) {
                if (edge.isStrong()) {
                    priority += 0;
                    if (edge.getTarget().isSaturated()) {
                        priority += -10; // no saturated states
                    } else {
                        if (edge.isSameActivity()) {
                            priority += 10;
                        } else {
                            priority += 0;
                        }
                    }
                } else {
                    if (edges.size() > 1) {
                        priority += 10; // make it weaker
                    }
                }
            }
            if (priority <= 0) {
                priority = 1;
            }
            action.setPriority(priority);
        }
    }

    boolean isTopLeftClick(ModelAction action, Rect displayBounds) {
        if (!action.requireTarget()) {
            return false;
        }
        if (!action.isClick()) {
            return false;
        }
        GUITreeNode node = action.getResolvedNode();
        if (node == null) {
            return false;
        }
        Rect nodeBounds = action.getResolvedNode().getBoundsInScreen();
        int top = displayBounds.top;
        int left = displayBounds.left;
        Rect topLeft = new Rect(left, top, left + 300, top + 300);
        return topLeft.contains(nodeBounds);
    }


    protected int getThrottleForNewAction(State state, ModelAction action) {
        if (state != action.getState()) {
            throw new IllegalStateException("Oops");
        }
        int throttle = baseThrottle;
        Collection<StateTransition> edges = getGraph().getOutStateTransitions(action);
        boolean hasActivityTransition = false;
        for (StateTransition edge : edges) {
            if (edge.action.isBack()) {
                continue;
            }
            if (!edge.isSameActivity()) {
                hasActivityTransition = true;
            }
        }
        if (action.isUnvisited()) {
            throttle += throttleForUnvisitedAction;
            Logger.dformat("Add throttle for unvisited activity state transition: %d", throttle);
        }
        if (hasActivityTransition) {
            throttle += throttleForActivityTransition;
            Logger.dformat("Add throttle for weak activity state transition: %d", throttle);
        }
        throttle = Math.min(throttle, maxThrottle);

        GUITreeNode node = action.getResolvedNode();
        if (node != null) {
            throttle += node.getExtraThrottle();
            Logger.dformat("Add user-defined throttle for state transition: %d", throttle);
        }

        if (throttle > 0) {
            Logger.dformat("Append a throttle %d for action %s", throttle, action);
        }
        return throttle;
    }

    /**
     * 
     * @return must return a non-null action
     */
    protected ModelAction handleNullAction() {
        ModelAction action = newState.randomlyPickAction(getRandom(), validatedActionFilter);
        if (action != null) {
            ModelAction resolved = validateNewAction(action);
            if (resolved != null) {
                return resolved;
            }
        }
        throw new BadStateException("No available action on the current state");
    }

    protected ModelAction selectNewActionRandomly() {
        ModelAction action = newState.randomlyPickAction(getRandom());
        return action;
    }

    protected ModelAction selectNewValidActionRandomly() {
        ModelAction action = newState.randomlyPickValidAction(getRandom());
        return action;
    }

    protected abstract Action selectNewActionNonnull();

    public void tearDown() {
        super.tearDown();
        saveGraph();
        saveActionHistory();
        actionCounters.print();
        getGraph().printActivityNodes();
        model.getNamingManager().dump();
        model.printCounters();
    }

    public List<ActionRecord> getActionHistory() {
        return this.model.getActionHistory();
    }

    public void updateActionHistory(int index, ActionRecord record) {
        this.model.updateActionHistory(index, record);
    }

    public void appendToActionHistory(long clockTimestamp, Action action) {
        int agentTimestamp = getTimestamp();
        this.model.appendToActionHistory(clockTimestamp, action, agentTimestamp);
        //actionCounters.logEvent(action.getType());
    }

    protected void saveActionHistory() {
        File actionHistoryFile = new File(checkOutputDir(), "action-history.log");
        Model.saveActionHistory(actionHistoryFile, getActionHistory());
    }

    protected void saveGraph() {
        if (!(saveDotGraph || saveObjModel || saveVisGraph)) {
            return;
        }
        Graph graph = getGraph();
        File graphOutputDir = checkOutputDir();
        Logger.println("Save graph data to " + graphOutputDir);
        File file = null;
        if (saveObjModel) {
            file = new File(graphOutputDir, "sataModel.obj");
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                oos.writeObject(model);
            } catch (IOException e) {
                e.printStackTrace();
                Logger.println("Fail to write model into " + file);
            }
        }
        if (saveDotGraph) {
            file = new File(graphOutputDir, "sataGraph.dot");
            try (PrintWriter pw = new PrintWriter(new FileOutputStream(file))) {
                graph.printDot(pw);
            } catch (IOException e) {
                e.printStackTrace();
                Logger.println("Fail to write dot graph into " + file);
            }
        }
        if (saveVisGraph) {
            file = new File(graphOutputDir, "sataGraph.vis.js");
            try (PrintWriter pw = new PrintWriter(new FileOutputStream(file))) {
                graph.printVis(pw);
            } catch (IOException e) {
                e.printStackTrace();
                Logger.println("Fail to write vis graph into " + file);
            }
        }
        if (saveStates) {
            for (State state : graph.getStates()) {
                file = new File(graphOutputDir,
                        String.format("step-%d-%s.txt", state.getFirstVisitedTimestamp(), state.getGraphId()));
                try (PrintWriter pw = new PrintWriter(new FileOutputStream(file))) {
                    state.saveState(pw);
                } catch (IOException e) {
                    e.printStackTrace();
                    Logger.println("Fail to write state into " + file);
                }
            }
        }
    }

}
