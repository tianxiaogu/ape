package com.android.commands.monkey.ape.model;

import static com.android.commands.monkey.ape.utils.Config.activityManagerType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.android.commands.monkey.ApeRRFormatter;
import com.android.commands.monkey.ape.naming.ActivityNamingManager;
import com.android.commands.monkey.ape.naming.Name;
import com.android.commands.monkey.ape.naming.Naming;
import com.android.commands.monkey.ape.naming.NamingFactory;
import com.android.commands.monkey.ape.naming.NamingManager;
import com.android.commands.monkey.ape.naming.StateNamingManager;
import com.android.commands.monkey.ape.tree.GUITree;
import com.android.commands.monkey.ape.tree.GUITreeAction;
import com.android.commands.monkey.ape.tree.GUITreeBuilder;
import com.android.commands.monkey.ape.tree.GUITreeNode;
import com.android.commands.monkey.ape.tree.GUITreeTransition;
import com.android.commands.monkey.ape.utils.Logger;

import android.content.ComponentName;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;

public class Model implements Serializable {

    static enum ModelEvent {
        NON_DETERMINISTIC_TRANSITION,
        ACTION_REFINEMENT,
        STATE_ABSTRACTION,
    }

    public static class ActionRecord implements Serializable {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        public final long clockTimestamp;
        public final int agentTimestamp;
        public final Action modelAction;
        public final GUITreeAction guiAction;
        public ActionRecord(long clockTimestamp, int agentTimestamp, Action action, GUITreeAction guiAction) {
            this.clockTimestamp = clockTimestamp;
            this.agentTimestamp = agentTimestamp;
            this.modelAction = action;
            this.guiAction = guiAction;
        }

        public void resolveModelAction() {
            if (this.modelAction.isModelAction()) {
                ModelAction modelAction = (ModelAction) this.modelAction;
                if (guiAction == null) {
                    throw new IllegalStateException("GUI action should not be null.");
                }
                GUITree tree = guiAction.getGUITree();
                int throttle = guiAction.getThrotlle();
                if (modelAction.requireTarget()) {
                    GUITreeNode node = guiAction.getGUITreeNode();
                    GUITreeNode[] nodes = tree.pickNodes(modelAction);
                    modelAction.resolveAt(agentTimestamp, throttle, tree, node, nodes);
                } else {
                    modelAction.resolveAt(agentTimestamp, throttle, tree, null, null);
                }
            }
        }
    }

    public static void saveActionHistory(File file, List<ActionRecord> actionHistory) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            for (ActionRecord record : actionHistory) {
                Action action = record.modelAction;
                int timestamp = record.agentTimestamp;
                long clockTime = record.clockTimestamp;
                if (action.isModelAction()) {
                    record.resolveModelAction();
                }
                ApeRRFormatter.startLogAction(pw, action, clockTime, timestamp);
                ApeRRFormatter.endLogAction(pw, action, timestamp);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Logger.wformat("Fail to save action history into %s.", actionHistory);
        }
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    // the abstraction function of the model
    // the model use namingManager to decide the specific abstraction function for a given GUI tree.
    protected NamingManager namingManager;
    // the state machine
    protected Graph graph;
    // A list of all actions, TODO: may be the cause of OOM
    protected List<ActionRecord> actionHistory = new ArrayList<ActionRecord>();

    protected int version;

    protected EnumCounters<ModelEvent> eventCounters = new EnumCounters<ModelEvent>() {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        @Override
        public ModelEvent[] getEnums() {
            return ModelEvent.values();
        }

    };

    public Model(NamingManager nm) {
        this(new Graph(), nm);
    }

    public List<ActionRecord> getActionHistory() {
        return this.actionHistory;
    }

    public void appendToActionHistory(long clockTimestamp, Action action, int agentTimestamp) {
        if (action.isModelAction()) {
            ModelAction modelAction = (ModelAction) action;
            this.actionHistory.add(new ActionRecord(clockTimestamp, agentTimestamp, action, modelAction.getResolvedGUITreeAction()));
        } else {
            this.actionHistory.add(new ActionRecord(clockTimestamp, agentTimestamp, action, null));
        }
    }

    public Model(Graph graph) {
        this.graph = graph;
        if (activityManagerType.equals("activity")) {
            this.namingManager = new ActivityNamingManager(new NamingFactory());
        } else {
            this.namingManager = new StateNamingManager(new NamingFactory());
        }
    }

    public Model(Graph graph, NamingManager namingManager) {
        this.graph = graph;
        this.namingManager = namingManager;
    }

    /**
     * Rebuild the whole model after abstraction functions of some GUI tree are updated.
     * @return
     */
    public Model rebuild() {
        long begin = SystemClock.elapsedRealtimeNanos();
        Logger.iprintln("Start rebuilding model... ");
        Set<State> statesToRemove = new HashSet<>();
        List<GUITreeTransition> treeTransitions = new ArrayList<>();
        Set<StateTransition> stateTransitions = new HashSet<>();
        List<GUITree> affectedTrees = new ArrayList<>();
        {
            // Remove model
            // long b = SystemClock.elapsedRealtimeNanos();
            for (State state : graph.getStates()) {
                for (GUITree tree : state.getGUITrees()) {
                    Naming naming = tree.getCurrentNaming();
                    Naming check = namingManager.getNaming(tree);
                    if (naming != check) {
                        statesToRemove.add(state);
                        break;
                    }
                }
            }
            for (State state : statesToRemove) {
                Logger.iformat("> Removing state %s", state);
                affectedTrees.addAll(state.getGUITrees());
                graph.remove(state, stateTransitions);
            }
            for (StateTransition st : stateTransitions) {
                treeTransitions.addAll(st.getGUITreeTransitions());
            }
            Collections.sort(treeTransitions, new Comparator<GUITreeTransition>() {

                @Override
                public int compare(GUITreeTransition o1, GUITreeTransition o2) {
                    return (int) (o1.getSource().getTimestamp() - o2.getSource().getTimestamp());
                }

            });
            long e = SystemClock.elapsedRealtimeNanos();
            Logger.iformat("> Removing (%d) old states and (%d) transitions finished in %d ms.", statesToRemove.size(),
                    stateTransitions.size(), TimeUnit.NANOSECONDS.toMillis(e - begin));
            for (State state : statesToRemove) {
                Logger.iformat(">> state: %s", state);
            }
            for (StateTransition st : stateTransitions) {
                Logger.iformat(">> transition: %s", st.toShortString());
            }
        }
        {
            long b = SystemClock.elapsedRealtimeNanos();
            graph.disableGraphEvents();
            graph.setVerbose(false);
            version++;
            graph.setVersion(version);
            Collections.sort(affectedTrees, new Comparator<GUITree>() {

                @Override
                public int compare(GUITree o1, GUITree o2) {
                    return o1.getTimestamp() - o2.getTimestamp();
                }

            });
            for (GUITree tree : affectedTrees) {
                getState(rebuild(tree));
            }
            for (GUITreeTransition tt : treeTransitions) {
                GUITree sourceTree = tt.getSource();
                GUITree targetTree = tt.getTarget();
                State source = sourceTree.getCurrentState();
                if (source == null) { // a GUI tree has been rebuilt by somebody
                    throw new NullPointerException("Source state should not be null for #" + sourceTree.getTimestamp());
                }
                State target = targetTree.getCurrentState();
                if (target == null) {
                    throw new NullPointerException("Target state should not be null for #" + targetTree.getTimestamp());
                }
                if (statesToRemove.contains(source)) {
                    throw new IllegalStateException("State " + source + " has been removed.");
                }
                if (statesToRemove.contains(target)) {
                    throw new IllegalStateException("State " + target + " has been removed.");
                }
                ModelAction action = rebuild(sourceTree, source, tt.getAction()).getModelAction();
                graph.addTransition(source, action, target, tt);
            }
            graph.rebuildHistory();
            graph.setVerbose(true);
            graph.enableGraphEvents();
            long e = SystemClock.elapsedRealtimeNanos();
            Logger.iformat("> Readding transitions finished in %d ms.", TimeUnit.NANOSECONDS.toMillis(e - b));
        }
        long end = SystemClock.elapsedRealtimeNanos();
        Logger.iformat(
                "Rebuilding model finished in %d ms, removed %d states and %d state transitions, and rebuild %d tree transitions.",
                TimeUnit.NANOSECONDS.toMillis(end - begin), statesToRemove.size(), stateTransitions.size(),
                treeTransitions.size());
        return this;
    }

    public List<GUITreeTransition> getGUITreeTransitions(StateTransition st) {
        return st.getGUITreeTransitions();
    }

    public GUITreeTransition getLatestGUITreeTransition(StateTransition st) {
        List<GUITreeTransition> ret = getGUITreeTransitions(st);
        if (ret == null || ret.isEmpty()) {
            return null;
        }
        return ret.get(ret.size() - 1);
    }

    /**
     * set the target for each GUI tree node
     * return a new GUI tree.
     * 
     * @param tree
     * @return
     */
    public GUITree rebuild(GUITree tree) {
        Logger.iprintln("> rebuilding tree #" + tree.getTimestamp());
        GUITreeBuilder treeBuilder = new GUITreeBuilder(namingManager, tree);
        return treeBuilder.getGUITree();
    }

    private GUITreeAction rebuild(GUITree tree, State state, GUITreeAction treeAction) {
        if (tree.getCurrentState() == null || state == null || tree.getCurrentState() != state) {
            throw new IllegalStateException();
        }
        ActionType type = treeAction.getActionType();
        ModelAction action;
        if (type.requireTarget()) {
            Name widget = treeAction.getGUITreeNode().getXPathName();
            if (!state.containsTarget(widget)) {
                Logger.wprintln("Given tree #" + tree.getTimestamp());
                Logger.wprintln("Action tree #" + treeAction.getGUITree().getTimestamp());
            }
            action = state.getAction(widget, type);
        } else {
            action = state.getAction(type);
        }
        treeAction.rebuild(action);
        return treeAction;
    }

    public StateTransition addTransition(State source, ModelAction action, State target, GUITree sourceTree,
            GUITreeAction treeAction, GUITree targetTree) {
        return graph.addTransition(source, action, target, sourceTree, treeAction, targetTree);
    }

    public Model resolveNonDeterministicTransitions(StateTransition edge) {
        if (edge.getType() == StateTransitionVisitType.NEW_ACTION_TARGET) {
            if (edge.getAction().isBack()) {
                return null; // back should be deterministic.
            }
            int version = this.version;
            long begin = SystemClock.elapsedRealtimeNanos();
            namingManager.resolveNonDeterminism(this, edge);
            long end = SystemClock.elapsedRealtimeNanos();
            if (version == this.version) {
                return null;
            }
            eventCounters.logEvent(ModelEvent.NON_DETERMINISTIC_TRANSITION);
            Logger.iformat("Eliminating non-deterministic transitions takes %s ms.",
                    TimeUnit.NANOSECONDS.toMillis(end - begin));
            return this;
        }
        return null;
    }

    /**
     * 
     * @param st
     * @return
     */
    public StateTransition update(StateTransition st) {
        return update(st, st.getLastGUITreeTransition());
    }

    public StateTransition update(StateTransition st, GUITreeTransition tt) {
        if (!isStale(st.getSource()) && !isStale(st.getTarget())) {
            return st;
        }
        return graph.getStateTransition(tt);
    }

    public ModelAction update(ModelAction action, GUITreeAction guiAction) {
        State state = action.getState();
        if (isStale(state)) {
            GUITree tree = guiAction.getGUITree();
            state = tree.getCurrentState();
            if (isStale(state)) {
                state = getState(tree);
            }
            if (isStale(state)) {
                throw new IllegalStateException("Sanity check failed!");
            }
            GUITreeNode node = guiAction.getGUITreeNode();
            ActionType type = action.getType();
            if (action.requireTarget()) {
                Name widget = node.getXPathName();
                return state.getAction(widget, type);
            } else {
                return state.getAction(action.getType());
            }
        }
        return action;
    }

    public ModelAction update(ModelAction action) {
        State state = action.getState();
        if (isStale(state)) {
            GUITree tree = state.getLatestGUITree();
            state = tree.getCurrentState();
            if (isStale(state)) {
                state = getState(tree);
            }
            GUITreeNode node = null;
            ActionType type = action.getType();
            if (action.requireTarget()) {
                node = action.getResolvedNode();
                if (node == null) {
                    return null;
                }
                if (!tree.contains(node)) {
                    return null;
                }
                Name widget = node.getXPathName();
                return state.getAction(widget, type);
            } else {
                return state.getAction(action.getType());
            }
        }
        return action;
    }

    public State update(State state) {
        if (isStale(state)) {
            GUITree tree = state.getLatestGUITree();
            state = tree.getCurrentState();
            if (isStale(state)) {
                return getState(tree);
            }
        }
        return state;
    }

    public State update(GUITree tree) {
        State state = tree.getCurrentState();
        if (isStale(state)) {
            return getState(tree);
        }
        return state;
    }

    /**
     * A state or action has been removed from the graph.
     * @param state
     * @return
     */
    public boolean isStale(State state) {
        if (state == null) {
            return true;
        }
        return !graph.contains(state);
    }

    public Graph getGraph() {
        return graph;
    }

    public State getState(ComponentName activity, AccessibilityNodeInfo rootInfo, Bitmap bitmap) {
        GUITreeBuilder treeBuilder = new GUITreeBuilder(namingManager, activity, rootInfo, bitmap);
        GUITree guiTree = treeBuilder.getGUITree();
        return checkAndAddStateData(guiTree);
    }

    public State getState(GUITree guiTree) {
        return checkAndAddStateData(guiTree);
    }

    private State checkAndAddStateData(GUITree tree) {
        Naming naming = tree.getCurrentNaming();
        StateKey stateKey = GUITreeBuilder.getStateKey(naming, tree);
        State state = graph.getOrCreateState(stateKey);
        state.append(tree);
        return state;
    }

    public NamingManager getNamingManager() {
        return namingManager;
    }

    public StateTransition getStateTransition(GUITreeTransition tt) {
        return graph.getStateTransition(tt);
    }

    public Iterator<GUITree> getGUITrees() {
        return graph.getGUITrees();
    }

    public Model actionRefinement(ModelAction action) {
        int version = this.version;
        long begin = SystemClock.elapsedRealtimeNanos();
        namingManager.actionRefinement(this, action);
        long end = SystemClock.elapsedRealtimeNanos();
        if (version == this.version) {
            return null;
        }
        eventCounters.logEvent(ModelEvent.ACTION_REFINEMENT);
        Logger.iformat("Action refinement takes %s ms.", TimeUnit.NANOSECONDS.toMillis(end - begin));
        return this;
    }

    public Model stateAbstraction(Naming naming, State target, Naming parentNaming, Set<State> states) {
        int version = this.version;
        long begin = SystemClock.elapsedRealtimeNanos();
        namingManager.stateAbstraction(this, naming, target, parentNaming, states);
        long end = SystemClock.elapsedRealtimeNanos();
        if (version == this.version) {
            return null;
        }
        Logger.iformat("State abstraction takes %s ms.", TimeUnit.NANOSECONDS.toMillis(end - begin));
        eventCounters.logEvent(ModelEvent.STATE_ABSTRACTION);
        return this;
    }

    public void printCounters() {
        this.eventCounters.print();
    }

    public void release(GUITree removed) {
        this.namingManager.release(removed);
    }

}
