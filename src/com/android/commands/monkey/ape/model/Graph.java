package com.android.commands.monkey.ape.model;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.android.commands.monkey.ape.ActionFilter;
import com.android.commands.monkey.ape.Subsequence;
import com.android.commands.monkey.ape.SubsequenceFilter;
import com.android.commands.monkey.ape.naming.Name;
import com.android.commands.monkey.ape.naming.Naming;
import com.android.commands.monkey.ape.tree.GUITree;
import com.android.commands.monkey.ape.tree.GUITreeAction;
import com.android.commands.monkey.ape.tree.GUITreeTransition;
import com.android.commands.monkey.ape.utils.Logger;
import com.android.commands.monkey.ape.utils.Utils;

public class Graph implements Serializable {

    static abstract class GraphIterator<T> implements Iterator<T> {

        Iterator<StateTransition> iterator;

        GraphIterator(Iterator<StateTransition> iterator) {
            this.iterator = iterator;
        }

        protected StateTransition advance() {
            return iterator.next();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }
    }

    public static Iterator<State> targets(Collection<StateTransition> edges) {
        return new GraphIterator<State>(edges.iterator()) {

            @Override
            public State next() {
                return advance().getTarget();
            }

        };
    }

    public static Set<State> targetsSet(Collection<StateTransition> edges) {
        Set<State> states = new HashSet<>();
        Iterator<State> it = targets(edges);
        while (it.hasNext()) {
            states.add(it.next());
        }
        return states;
    }

    /**
     * 
     */
    private static final long serialVersionUID = -4227920009231281174L;

    protected Map<StateKey, State> keyToState = new HashMap<>();

    private Map<String, ActivityNode> activities = new HashMap<String, ActivityNode>();
    private Map<StateTransition, StateTransition> edges = new HashMap<StateTransition, StateTransition>();
    private Map<State, Map<StateTransition, StateTransition>> stateToOutStateTransitions = new HashMap<>();
    private Map<State, Map<StateTransition, StateTransition>> stateToInStateTransitions = new HashMap<>();
    private Map<ModelAction, Map<StateTransition, StateTransition>> actionToOutStateTransitions = new HashMap<>();

    private Set<State> entryStates = new HashSet<State>();
    private Set<State> cleanEntryStates = new HashSet<State>();
    private Set<GUITree> entryGUITrees = new HashSet<GUITree>();
    private Set<GUITree> cleanEntryGUITrees = new HashSet<GUITree>();

    private Map<Naming, Set<State>> namingToStates = new HashMap<>();

    private /* transient */ Set<ModelAction> unvisitedActions = new HashSet<>();
    private /* transient */ Set<ModelAction> visitedActions = new HashSet<>();

    private transient List<StateTransition> stateTransitionHistory = new ArrayList<>();
    private List<GUITreeTransition> treeTransitionHistory = new ArrayList<>(100);

    private boolean fireEvents;
    private transient List<GraphListener> listeners;
    private int timestamp;

    private ActionCounters actionCounters = new ActionCounters();

    private String graphId = "g0";

    private Map<String, Map<Name, Set<ModelAction>>> nameToActions = new HashMap<>();

    private boolean verbose = true;

    public int size() {
        return keyToState.size();
    }

    public Set<State> getEntryStates() {
        return this.entryStates;
    }

    public Set<State> getCleanEntryStates() {
        return this.cleanEntryStates;
    }

    public State getNameGlobalTarget(ModelAction action) {
        State state = action.getState();
        Name name = action.getTarget();
        if (name == null) {
            return null;
        }
        String activity = state.getActivity();
        Set<ModelAction> actions = Utils.getFromMapMap(nameToActions, activity, name);
        if (actions == null) {
            return null;
        }
        if (actions.size() <= 1) {
            return null;
        }
        Set<State> targets = new HashSet<>();
        for (ModelAction shared : actions) {
            if (!shared.getType().equals(action.getType())) {
                continue;
            }
            for (StateTransition st : getOutStateTransitions(shared)) {
                targets.add(st.getTarget());
            }
        }
        if (targets.size() == 1) {
            return targets.iterator().next();
        }
        return null;
    }

    public boolean isNameGlobalAction(ModelAction action) {
        return getNameGlobalTarget(action) != null;
    }

    public boolean isActionUnvisitedByName(ModelAction action) {
        if (!action.requireTarget()) {
            return action.isUnvisited();
        }
        State state = action.getState();
        String activity = state.getActivity();
        Set<ModelAction> actions = Utils.getFromMapMap(nameToActions, activity, action.getTarget());
        if (actions == null) {
            return action.isUnvisited();
        }
        for (ModelAction shared : actions) {
            if (shared.isVisited() && shared.getType().equals(action.getType())) {
                return false;
            }
        }
        return true;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setVersion(int version) {
        graphId = "g" + version;
    }

    public ActivityNode getActivityNode(String activity) {
        ActivityNode node = activities.get(activity);
        return node;
    }

    public List<GraphListener> getListeners() {
        if (listeners == null) {
            return Collections.emptyList();
        }
        return listeners;
    }

    private boolean addActivity(State state) {
        String activity = state.getActivity();
        ActivityNode node = activities.get(activity);
        if (node == null) {
            node = new ActivityNode(activity);
            node.addState(state);
            activities.put(activity, node);
            return true;
        }
        node.addState(state);
        return false;
    }

    public void printStatistics() {
        Logger.format(
                "GSTG(%s): activities (%d), states (%d), edges (%d), unvisited actions (%d), visited actions (%d)",
                graphId, activities.size(), size(), edges.size(), unvisitedActions.size(), visitedActions.size());
        for (ActivityNode an : this.activities.values()) {
            Logger.iformat("- %5.2f %3d %s", an.getVisitedRate(), an.getStates().size(), an);
        }
    }


    static State[] EMPTY_TARGETS = new State[0];

    public void addListener(GraphListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        this.listeners.add(listener);
    }

    public State getOrCreateState(StateKey stateKey) {
        State state = keyToState.get(stateKey);
        if (state == null) {
            state = State.buildState(stateKey);
            state.setGraphId(graphId + "s" + keyToState.size());
            fireNewStateEvents(state);
            keyToState.put(stateKey, state);
            addActivity(state);
            addActions(state);
            Utils.addToMapSet(namingToStates, state.getCurrentNaming(), state);
        }
        return state;
    }

    private void addActions(State state) {
        Map<Name, Set<ModelAction>> actions = Utils.getMapFromMap(nameToActions, state.getActivity());
        for (ModelAction action : state.getActions()) {
            action.setGraphId(graphId + "a" + actionCounters.getTotal());
            actionCounters.logEvent(action.getType());
            unvisitedActions.add(action);
            if (action.requireTarget()) {
                Utils.addToMapSet(actions, action.getTarget(), action);
            }
        }
    }

    public static Comparator<GraphElement> VISIT_COUNT_COMPARATOR = new Comparator<GraphElement>() {

        @Override
        public int compare(GraphElement o1, GraphElement o2) {
            return o1.getVisitedCount() - o2.getVisitedCount();
        }
    };

    static Comparator<GraphElement> LAST_VISIT_COUNT_COMPARATOR = new Comparator<GraphElement>() {

        @Override
        public int compare(GraphElement o1, GraphElement o2) {
            return o1.getLastVisitedTimestamp() - o2.getLastVisitedTimestamp();
        }
    };

    public void disableGraphEvents() {
        this.fireEvents = false;
    }

    public void enableGraphEvents() {
        this.fireEvents = true;
    }

    private void fireNewStateEvents(State node) {
        if (fireEvents == false) {
            return;
        }
        for (GraphListener l : listeners) {
            l.onAddNode(node);
        }
    }

    private void fireStateTransitionEvents(StateTransition edge) {
        if (fireEvents == false) {
            return;
        }
        for (GraphListener l : listeners) {
            l.onVisitStateTransition(edge);
        }
    }

    public int getTimestamp() {
        return timestamp;
    }

    public Iterator<GUITree> getGUITrees() {
        return new Iterator<GUITree>() {
            Iterator<GUITree> treeIterator;
            Iterator<State> stateIterator;
            {
                stateIterator = keyToState.values().iterator();
                if (stateIterator.hasNext()) {
                    treeIterator = stateIterator.next().getGUITrees().iterator();
                } else {
                    treeIterator = null;
                }
            }

            @Override
            public boolean hasNext() {
                if (treeIterator == null) {
                    return false;
                }
                if (!treeIterator.hasNext()) {
                    if (stateIterator.hasNext()) {
                        treeIterator = stateIterator.next().getGUITrees().iterator();
                        return treeIterator.hasNext();
                    } else {
                        treeIterator = null;
                        return false;
                    }
                } else {
                    return true;
                }
            }

            @Override
            public GUITree next() {
                return treeIterator.next();
            }

        };
    }

    public int getAverageVisitedCount() {
        int size = this.size();
        if (size == 0) {
            return 0;
        }
        return this.timestamp / size;
    }

    public StateTransition addTransition(State source, ModelAction action, State target, GUITree sourceTree,
            GUITreeAction treeAction, GUITree targetTree) {
        StateTransition edge = addStateTransition(source, action, target);
        if (edge == null) {
            return null;
        }
        GUITreeTransition treeTransition = new GUITreeTransition(sourceTree, treeAction, targetTree);
        treeTransition.setThrottle(action.getThrottle());
        edge.updateThrottle(treeTransition.getThrottle());
        stateTransitionHistory.add(edge);
        treeTransitionHistory.add(treeTransition);
        edge.append(treeTransition);

        edge.strengthen();
        markVisited(edge, timestamp);
        fireStateTransitionEvents(edge);
        if (this.entryGUITrees.contains(sourceTree)) {
            this.entryStates.add(source);
        }
        if (this.cleanEntryGUITrees.contains(sourceTree)) {
            this.cleanEntryStates.add(source);
        }
        return edge;
    }

    public StateTransition addTransition(State source, ModelAction action, State target, GUITreeTransition treeTransition) {
        StateTransition edge = addStateTransition(source, action, target);
        if (edge == null) {
            throw new IllegalStateException("Should not be null.");
        }
        if (this.entryGUITrees.contains(treeTransition.getSource())) {
            this.entryStates.add(source);
        }
        if (this.cleanEntryGUITrees.contains(treeTransition.getSource())) {
            this.cleanEntryStates.add(source);
        }
        edge.strengthen();
        stateTransitionHistory.add(edge);
        edge.updateThrottle(treeTransition.getThrottle());
        edge.append(treeTransition);
        int timestamp = treeTransition.getTimestamp();
        if (timestamp == -1) {
            throw new IllegalStateException("Untracked GUI tree transition..");
        }
        markVisited(source, timestamp);
        markVisited(action, timestamp);
        markVisited(edge, timestamp);
        return edge;
    }

    private StateTransition addStateTransition(State source, ModelAction action, State target) {
        boolean added = false;
        if (target == null) {
            throw new RuntimeException("Target state should never be null");
        }
        StateTransition edge = null;
        if (source != null) {
            edge = new StateTransition(source, action, target);
            added = Utils.addToMapMapIfAbsent(actionToOutStateTransitions, action, edge, edge);
            if (verbose) {
                Logger.println("=== Adding edge...");
                Logger.println("    Source: " + source);
                Logger.println("    Action: " + action);
                Logger.println("    Target: " + target);
                Logger.println("    Add target result: " + added);
            }
            if (added != Utils.addToMapMapIfAbsent(stateToOutStateTransitions, source, edge, edge)) {
                throw new IllegalStateException("Sanity check failed");
            }
            if (added != Utils.addToMapMapIfAbsent(stateToInStateTransitions, target, edge, edge)) {
                throw new IllegalStateException("Sanity check failed");
            }
            if (added) {
                edge.setGraphId(graphId + "e" + edges.size());
                Utils.putIfAbsent(edges, edge, edge);
                if (actionToOutStateTransitions.get(action).size() == 1) {
                    edge.setType(StateTransitionVisitType.NEW_ACTION);
                } else {
                    edge.setType(StateTransitionVisitType.NEW_ACTION_TARGET);
                }
            } else {
                StateTransition existing = edges.get(edge);
                if (existing == null || existing == edge) {
                    throw new IllegalStateException();
                }
                edge = existing;
                edge.setType(StateTransitionVisitType.EXISTING);
            }
        }
        timestamp++;
        if (verbose) {
            printStatistics();
            Logger.format("GSTG is %supdated.", (added ? "" : "NOT "));
            Logger.format("GSTG state is %schanged.", (target.equals(source) ? "NOT " : ""));
        }
        return edge;
    }

    public void weakenStateTransition(State source, ModelAction action, State target) {
        StateTransition edge = this.edges.get(new StateTransition(source, action, target));
        if (edge == null) {
            throw new IllegalArgumentException("Cannot update a non-existing edge");
        }
        edge.weaken();
    }

    public Set<StateTransition> getOutStateTransitions(State state) {
        Map<StateTransition, StateTransition> ret = stateToOutStateTransitions.get(state);
        if (ret == null) {
            return Collections.emptySet();
        }
        return ret.keySet();
    }

    public Collection<StateTransition> getOutStateTransitions(ModelAction action) {
        Map<StateTransition, StateTransition> ret = actionToOutStateTransitions.get(action);
        if (ret == null) {
            return Collections.emptySet();
        }
        return ret.keySet();
    }

    public Set<StateTransition> getInStateTransitions(State state) {
        Map<StateTransition, StateTransition> ret = stateToInStateTransitions.get(state);
        if (ret == null) {
            return Collections.emptySet();
        }
        return ret.keySet();
    }

    public Subsequence findShortestPath(SubsequenceFilter filter, State current) {
        Set<StateTransition> edges = getOutStateTransitions(current);
        if (edges.isEmpty()) {
            return null;
        }
        Set<State> visited = new HashSet<State>();
        visited.add(current);
        LinkedList<Subsequence> pathQueue = new LinkedList<>();
        // n-step search
        // Do a BFS to find the shortest path
        // avoid unnecessary leading recursion
        {
            Subsequence path = new Subsequence(current);
            for (StateTransition edge : edges) {
                if (visited.contains(edge.target)) {
                    continue;
                }
                if (filter.extend(path, edge)) {
                    Subsequence newPath = new Subsequence(path, edge);
                    visited.add(edge.target);
                    if (filter.include(newPath)) {
                        return newPath;
                    }
                    pathQueue.addLast(newPath);
                }
            }
        }
        while (!pathQueue.isEmpty()) {
            Subsequence path = pathQueue.removeFirst();
            Set<StateTransition> outStateTransitions = getOutStateTransitions(path.getLastState());
            if (outStateTransitions.isEmpty()) {
                continue;
            }
            for (StateTransition edge : outStateTransitions) {
                if (visited.contains(edge.target)) {
                    continue;
                }
                if (filter.extend(path, edge)) {
                    Subsequence newPath = new Subsequence(path, edge);
                    visited.add(edge.target);
                    if (filter.include(newPath)) {
                        return newPath;
                    }
                    pathQueue.addLast(newPath);
                }
            }
        }
        return null;

    }

    public void markVisited(ModelAction action, int timestamp) {
        if (!action.getType().isModelAction()) {
            return;
        }
        if (action.isUnvisited()) {
            if (!this.unvisitedActions.remove(action)) {
                throw new RuntimeException("sanity check failed, action should be added " + action);
            }
            action.visitedAt(timestamp);
            this.visitedActions.add(action);
        } else if (visitedActions.contains(action)) {
            action.visitedAt(timestamp);
        } else {
            Logger.println("Untracked action: " + action);
            Logger.println("State is " + action.getState());
            if (this.unvisitedActions.contains(action)) {
                throw new RuntimeException("Unvisited actions should not be marked visited.");
            }
            throw new RuntimeException("sanity check failed");
        }
    }

    public void markVisited(State node, int timestamp) {
        node.visitedAt(timestamp);
        markVisited(node.getActivity(), timestamp);
    }

    private void markVisited(StateTransition edge, int timestamp) {
        if (!edges.containsKey(edge)) {
            throw new RuntimeException("Sanity check failed!");
        }
        edge.visitedAt(timestamp);
    }

    private void markVisited(String activity, int timestamp) {
        ActivityNode node = getActivityNode(activity);
        node.visitedAt(timestamp);
    }

    public void printDot(PrintWriter pw) {
        pw.format("digraph GSTG {\n", 1);
        for (State sd : keyToState.values()) {
            String stateID = sd.getGraphId();
            pw.format("\t%s [label=\"%s\"];\n", stateID, stateID);
        }
        for (StateTransition edge : edges.keySet()) {
            String sourceID = edge.source.getGraphId();
            String targetID = edge.target.getGraphId();
            ModelAction action = edge.action;
            String actionID = action.getGraphId();
            String edgeID = edge.getGraphId();
            String style = "solid";
            if (edge.getStrength() < 1) {
                style = "dashed";
            } else if (edge.getMissingCount() > 0) {
                style = "bold";
            }

            pw.format("\t%s -> %s [style=%s, label=\"%s[%s]\"];\n", sourceID, targetID, style, edgeID, actionID);
        }
        pw.format("}\n", 1);
    }

    String getColor(int value, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException();
        }

        if (max < min) {
            throw new IllegalArgumentException();
        }

        int range = max - min;
        int delta = value - min;

        if (range == 0) {
            range = 1;
        }

        int r, g, b;
        r = 255 - (int) (255 * (1.0 * delta / range));
        b = g = r;
        return String.format("#%02X%02X%02X", r, g, b);
    }

    public void printVis(PrintWriter pw) {
        int maxVisited = Integer.MIN_VALUE;
        for (State state : keyToState.values()) {
            if (state.getVisitedCount() > maxVisited) {
                maxVisited = state.getVisitedCount();
            }
        }

        // System.out.format("%d %d %d", maxVisited, maxStrength, minStrength);
        pw.println("var nodes = new vis.DataSet([");
        for (State sd : keyToState.values()) {
            String stateID = sd.getGraphId();
            pw.format(
                    "\t{ id: \"%s\", label: \"%s\", title: \"%s\", screenURL: \"%s\", color: {background: \"%s\", border: \"#000000\"}},\n",
                    stateID, stateID, getNodeTitle(sd), String.format("step-%d.png", sd.getFirstVisitedTimestamp()),
                    getColor(sd.getVisitedCount(), 0, maxVisited));
        }
        pw.println("]);");

        int minStrength = Integer.MAX_VALUE;
        int maxStrength = Integer.MIN_VALUE;
        maxVisited = Integer.MIN_VALUE;
        for (StateTransition e : edges.keySet()) {
            if (e.getStrength() > maxStrength) {
                maxStrength = e.getStrength();
            }
            if (e.getStrength() < minStrength) {
                minStrength = e.getStrength();
            }
            if (e.getVisitedCount() > maxVisited) {
                maxVisited = e.getVisitedCount();
            }
        }

        pw.println("var edges = new vis.DataSet([");
        for (StateTransition edge : edges.keySet()) {
            String sourceID = edge.source.getGraphId();
            String targetID = edge.target.getGraphId();
            ModelAction action = edge.action;
            String actionID = action.getGraphId();
            pw.format("\t{from: \"%s\", to: \"%s\", label: \"%s\", title: \"%s\", color: {border: \"%s\"}%s},\n",
                    sourceID, targetID, actionID, getStateTransitionTitle(edge),
                    getColor(edge.getVisitedCount(), 0, maxVisited), getStateTransitionType(edge));
        }
        pw.println("]);");
    }

    private String getStateTransitionType(StateTransition edge) {
        if (edge.isStrong()) {
            return "";
        }
        return ", dashes: true";
    }

    static String escape(String javaString) {
        return javaString.replace("\\", "\\\\").replace("\n", "\\n").replace("'", "\\'").replace("\"", "\\\"");
    }

    private Object getStateTransitionTitle(StateTransition edge) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table><tbody><tr>");
        sb.append("<td>Visited</td>" + "<td>First Visited</td>" + "<td>Last Visited</td>" + "<td>Strength</td>"
                + "<td>Hitting</td>" + "<td>Missing</td>" + "<td>Action</td>");
        sb.append("</tr><tr><td>");
        sb.append(edge.action.getVisitedCount());
        sb.append("</td><td>");
        sb.append(edge.action.getFirstVisitedTimestamp());
        sb.append("</td><td>");
        sb.append(edge.action.getLastVisitedTimestamp());
        sb.append("</td><td>");
        sb.append(edge.getStrength());
        sb.append("</td><td>");
        sb.append(edge.getHittingCount());
        sb.append("</td><td>");
        sb.append(edge.getMissingCount());
        sb.append("</td><td>");
        sb.append(escape(edge.action.toFullString()));
        sb.append("</td></tr></tbody></table>");
        return escape(sb.toString());
    }

    private Object getNodeTitle(State sd) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table border='1'><tbody><tr><td colspan='6'>");
        sb.append(sd.getActivity());
        sb.append("</td></tr><tr>");
        sb.append(
                "<td>Visited</td><td>First Visited</td><td>Last Visited</td><td>Theta</td><td>One step Theta</td><td>Actions</td><td>Widgets</td>");
        sb.append("</tr><tr><td>");
        sb.append(sd.getVisitedCount());
        sb.append("</td><td>");
        sb.append(sd.getFirstVisitedTimestamp());
        sb.append("</td><td>");
        sb.append(sd.getLastVisitedTimestamp());
        sb.append("</td><td>");
        sb.append(sd.getCountOfActions());
        sb.append("</td><td>");
        sb.append(sd.getCountOfWidgets());
        sb.append("</td></tr></tbody></table>");

        sb.append("<ol>");
        for (Name w : sd.getWidgets()) {
            sb.append("<li>");
            sb.append(w);
        }
        sb.append("</ol>");
        sb.append("<ol>");
        for (ModelAction a : sd.getActions()) {
            sb.append("<li>");
            sb.append(escape(a.toFullString()));
        }
        sb.append("</ol>");
        return escape(sb.toString());
    }

    public int getWeakOutStateTransitions(State state, int threshold) {
        Map<StateTransition, StateTransition> ret = stateToOutStateTransitions.get(state);
        if (ret == null) {
            return 0;
        }
        int count = 0;
        for (StateTransition edge : ret.keySet()) {
            if (edge.getStrength() < threshold) {
                count++;
            }
        }
        return count;
    }

    public int getWeakOutStateTransitions(State state) {
        Map<StateTransition, StateTransition> ret = stateToOutStateTransitions.get(state);
        if (ret == null) {
            return 0;
        }
        int count = 0;
        for (StateTransition edge : ret.keySet()) {
            if (!edge.isStrong()) {
                count++;
            }
        }
        return count;
    }

    public Collection<State> getStates() {
        return this.keyToState.values();
    }

    // TODO: Use TreeSet
    public Collection<StateTransition> sortStateTransitionsByLastVisitedCount(Set<StateTransition> edges) {
        StateTransition[] results = new StateTransition[edges.size()];
        results = edges.toArray(results);
        Arrays.sort(results, LAST_VISIT_COUNT_COMPARATOR);
        return Arrays.asList(results);
    }

    public List<Subsequence> findShortestPaths(State current, SubsequenceFilter filter, int pathLength) {
        List<Subsequence> selectedPaths = new ArrayList<Subsequence>();
        findShortestPaths(selectedPaths, current, filter, pathLength);
        return selectedPaths;
    }

    public void findShortestPaths(List<Subsequence> selectedPaths, State current, SubsequenceFilter filter,
            int pathLength) {
        Set<StateTransition> edges = getOutStateTransitions(current);
        if (edges.isEmpty()) {
            return;
        }

        Set<State> visited = new HashSet<State>();
        visited.add(current);
        LinkedList<Subsequence> pathQueue = new LinkedList<>();
        // n-step search
        // Do a BFS to find the shortest path
        // avoid unnecessary leading recursion
        {
            Subsequence path = new Subsequence(current);
            for (StateTransition edge : sortStateTransitionsByLastVisitedCount(edges)) {
                if (visited.contains(edge.target)) {
                    continue;
                }
                if (filter.extend(path, edge)) {
                    Subsequence newPath = new Subsequence(path, edge);
                    visited.add(edge.target);
                    if (filter.include(newPath)) {
                        selectedPaths.add(newPath);
                        newPath.close();
                        continue;
                    }
                    pathQueue.addLast(newPath);
                }
            }
        }
        while (!pathQueue.isEmpty()) {
            Subsequence path = pathQueue.removeFirst();
            if (path.size() == pathLength) {
                path.close();
                continue;
            }
            Set<StateTransition> outStateTransitions = getOutStateTransitions(path.getLastState());
            if (outStateTransitions.isEmpty()) {
                continue;
            }
            for (StateTransition edge : sortStateTransitionsByLastVisitedCount(outStateTransitions)) {
                if (visited.contains(edge.target)) {
                    continue;
                }
                if (filter.extend(path, edge)) {
                    Subsequence newPath = new Subsequence(path, edge);
                    visited.add(edge.target);
                    if (filter.include(newPath)) {
                        selectedPaths.add(newPath);
                        newPath.close();
                        continue;
                    }
                    pathQueue.addLast(newPath);
                }
            }
        }
    }


    public boolean isReachable(Collection<State> from, State to) {
        if (from.contains(to)) {
            return true;
        }
        Set<State> visited = new HashSet<State>();
        visited.addAll(from);
        LinkedList<State> queue = new LinkedList<>();
        queue.addAll(from);
        while (!queue.isEmpty()) {
            State current = queue.removeFirst();
            Set<StateTransition> outStateTransitions = getOutStateTransitions(current);
            if (outStateTransitions.isEmpty()) {
                continue;
            }
            for (StateTransition edge : outStateTransitions) {
                if (visited.contains(edge.target)) {
                    continue;
                }
                if (edge.isStrong()) {
                    if (edge.target == to) {
                        return true;
                    }
                    queue.add(edge.target);
                    visited.add(edge.target);
                }
            }
        }
        return false;
    }

    public void moveToState(List<Subsequence> selectedPaths, State current, final State end, final boolean includeBack, int pathLength) {
        SubsequenceFilter strongFilter = new SubsequenceFilter() {
            @Override
            public boolean include(Subsequence path) {
                if (path.isEmpty()) {
                    return false;
                }
                return end.equals(path.getLastState());
            }

            @Override
            public boolean extend(Subsequence path, StateTransition edge) {
                if (!ActionFilter.ENABLED_VALID.include(edge.action)) {
                    return false;
                }
                if (!includeBack && edge.action.isBack()) {
                    return false;
                }
                return edge.isStrong();
            }
        };

        findShortestPaths(selectedPaths, current, strongFilter, pathLength);
    }

    /**
     * 
     * @param states,
     *            A*B
     * @return
     */
    public Subsequence fillTransitionsByHistory(final State[] states) {
        int H = stateTransitionHistory.size() - 1;
        int S = states.length - 1;
        LinkedList<StateTransition> edges = new LinkedList<StateTransition>();
        final int maxWindowSize = stateTransitionHistory.size(); // states.length
        // >> 1 + 2;
        final int maxCircleSize = 1;
        int edgeCount = 0;
        while (H >= 0 && S >= 0 && edgeCount < maxWindowSize) {
            State current = states[S--];
            int circleCount = 0;
            while (H >= 0) {
                StateTransition edge = stateTransitionHistory.get(H);
                H--; // move to next;
                edgeCount++;
                if (edge.target.equals(current)) {
                    if (edge.isCircle()) { // non-trivial action
                        if (edge.getTheta() != 0 && H >= 0) {
                            StateTransition previous = stateTransitionHistory.get(H); // next
                            // in
                            // reverse
                            // order
                            if (previous.target.equals(edge.target)) {
                                if (circleCount > maxCircleSize) {
                                    Logger.dformat("Too many circle edges, skip a valid non-trival circle edge %s",
                                            edge);
                                    continue;
                                }
                                edges.addFirst(edge); // valid transitions
                                circleCount++;
                                Logger.dformat("Include a valid non-trival circle edge %s", edge);
                                continue;
                            } else { // skip invalid transitions
                                Logger.dformat("Skip an invalid non-trival circle edge %s", edge);
                                continue;
                            }
                        } else { // skip trivial update
                            Logger.dformat("Skip a trival circle edge %s", edge);
                            continue;
                        }
                    } else if (S < 0) {
                        break;
                    } else if (edge.source.equals(states[S])) {
                        edges.addFirst(edge);
                        Logger.dformat("Include a valid non-trival edge %s", edge);
                        break;
                    } else {
                        Logger.dformat("Skip an invalid edge %s", edge);
                    }
                }
            }
        }
        if (edges.isEmpty()) {
            return null;
        }
        if (S >= 0) { // S must be -1
            return null;
        }
        return new Subsequence(edges);
    }

    public void fillTransitions(List<Subsequence> selectedPaths, final State[] states) {
        Logger.println("Fill transitions: ");
        for (int i = 0; i < states.length; i++) {
            Logger.format("\t%3d %s", i, states[i]);
        }
        SubsequenceFilter filter = new SubsequenceFilter() {
            @Override
            public boolean include(Subsequence path) {
                return path.size() == states.length - 1;
            }

            @Override
            public boolean extend(Subsequence path, StateTransition edge) {
                int size = path.size();
                if (!states[size].equals(edge.source)) {
                    Utils.reportConflict(states[size], edge.source);
                    throw new IllegalStateException();
                }
                if (!edge.isStrong()) {
                    return false;
                }
                if (!states[size + 1].equals(edge.target)) {
                    return false;
                }
                if (!edge.action.isBack() && !edge.action.isValid()) {
                    return false;
                }
                return true;
            }
        };

        findShortestPaths(selectedPaths, states[0], filter, states.length - 1); // skip
        // the
        // first
    }

    public boolean hasInStateTransitionFrom(State thisState, State other) {
        Set<StateTransition> edges = getInStateTransitions(thisState);
        if (edges.isEmpty()) {
            return false;
        }
        for (StateTransition e : edges) {
            if (e.source.equals(other)) {
                return true;
            }
        }
        return false;
    }

    public void printActivityNodes() {
        ActivityNode[] nodes = new ActivityNode[this.activities.size()];
        nodes = this.activities.values().toArray(nodes);
        Arrays.sort(nodes);
        Logger.iprintln("Print states in each activity.");
        for (int i = nodes.length - 1, k = 1; i >= 0; i--, k++) {
            Logger.format("%3d. [%d] %s", k, nameToActions.get(nodes[i].activity).size(), nodes[i]);
            State[] states = nodes[i].getStates().toArray(new State[0]);
            Arrays.sort(states, new Comparator<State>() {

                @Override
                public int compare(State o1, State o2) {
                    int ret = o1.getVisitedCount() - o2.getVisitedCount();
                    if (ret != 0) {
                        return ret;
                    }
                    ret = o1.getCountOfWidgets() - o2.getCountOfWidgets();
                    if (ret != 0) {
                        return ret;
                    }
                    ret = o1.getCountOfActions() - o2.getCountOfActions();
                    if (ret != 0) {
                        return ret;
                    }
                    return o1.getFirstVisitedTimestamp() - o2.getFirstVisitedTimestamp();
                }

            });
            for (int j = states.length - 1, m = 1; j >= 0; j--, m++) {
                Logger.format("    %3d %s", m, states[j]);
            }
        }
        Logger.iprintln("Print entry states");
        State[] entries = entryStates.toArray(new State[0]);
        for (int i = 0; i < entries.length; i++) {
            Logger.iformat("%3d. %s", i, entries[i]);
        }
        Logger.iprintln("Print naming states");
        for(Entry<Naming, Set<State>> entry : namingToStates.entrySet()) {
            Naming naming = entry.getKey();
            Set<State> states = entry.getValue();
            Logger.iformat("- %3d %s", states.size(), naming);
            for (State s : states) {
                Logger.iformat("    - %s", s);
            }
        }
        // Logger.iprintln("Print action counters.");
        // actionCounters.print();
    }

    /**
     * How many edges of a state
     * 
     * @param state
     * @return
     */
    public int getCountOfOutStateTransitions(State state) {
        Map<StateTransition, StateTransition> edges = this.stateToOutStateTransitions.get(state);
        if (edges == null) {
            return 0;
        }
        return edges.size();
    }

    /**
     * How many actions have been visited on a state
     * 
     * @param state
     * @return
     */
    public int getCountOfOutActions(State state) {
        int count = 0;
        for (ModelAction action : state.getActions()) {
            if (this.actionToOutStateTransitions.containsKey(action)) {
                count++;
            }
        }
        return count;
    }

    public StateTransition findStateTransition(State source, ModelAction action, State target) {
        return this.edges.get(new StateTransition(source, action, target));
    }

    public StateTransition getStateTransition(State source, ModelAction action, State target) {
        StateTransition edge = findStateTransition(source, action, target);
        if (edge == null) {
            throw new IllegalArgumentException("Cannot update a non-existing edge");
        }
        return edge;
    }

    public static Graph readGraph(String modelFile) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(modelFile))) {
            return (Graph) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.println("Fail to load graph from " + modelFile);
        }
        return new Graph();
    }

    public int getCountOfUnvisitedActions() {
        return this.unvisitedActions.size();
    }

    public void printStateTransitions() {
        StateTransition[] edges = new StateTransition[this.edges.size()];
        edges = this.edges.keySet().toArray(edges);
        Arrays.sort(edges, VISIT_COUNT_COMPARATOR);
        for (int i = edges.length - 1, ii = 1; i >= 0; i--, ii++) {
            StateTransition edge = edges[i];
            Logger.format("%3d %s", ii, formatStateTransition(edge));
        }
    }

    public String formatStateTransition(StateTransition edge) {
        return String.format("%s H(%d), M(%d) %s -%s-> %s", edge.getGraphId(), edge.getHittingCount(),
                edge.getMissingCount(), edge.source.getGraphId(), edge.action.getGraphId(), edge.target.getGraphId());
    }

    public ActivityNode[] getActivityNodes() {
        return activities.values().toArray(new ActivityNode[0]);
    }

    public int getCountOfActivities() {
        return this.activities.size();
    }

    public StateTransition getStateTransition(GUITreeTransition tt) {
        StateTransition st = tt.getCurrentStateTransition();
        if (st == null) {
            throw new IllegalStateException("Invalid tree transition");
        }
        if (!edges.containsKey(st)) {
            throw new IllegalStateException("Dangling tree transition");
        }
        return st;
    }

    public boolean isLikeBack(ModelAction action) {
        if (action.isBack()) {
            return true;
        }
        if (!action.isClick()) {
            return false;
        }
        State state = action.getState();
        ModelAction back = state.getBackAction();
        if (!back.isVisited()) {
            return false;
        }
        throw new RuntimeException("Not implemented");
    }

    void remove(State state, Collection<StateTransition> removed) {
        {
            Map<StateTransition, StateTransition> inStateTransitions = this.stateToInStateTransitions.remove(state);
            if (inStateTransitions != null && inStateTransitions.size() > 0) {
                Collection<StateTransition> temp = inStateTransitions.values();
                removed.addAll(temp);
                for (StateTransition edge : temp) {
                    this.edges.remove(edge);
                    State source = edge.getSource();
                    ModelAction action = edge.getAction();
                    Utils.removeFromMapMap(this.actionToOutStateTransitions, action, edge);
                    Utils.removeFromMapMap(this.stateToOutStateTransitions, source, edge);
                    Utils.removeFromMapMap(this.stateToInStateTransitions, edge.getTarget(), edge);
                }
            }
        }
        {
            Map<StateTransition, StateTransition> outStateTransitions = this.stateToOutStateTransitions.remove(state);
            if (outStateTransitions != null && outStateTransitions.size() > 0) {
                Collection<StateTransition> temp = outStateTransitions.values();
                removed.addAll(temp);
                for (StateTransition edge : temp) {
                    this.edges.remove(edge);
                    State target = edge.getTarget();
                    ModelAction action = edge.getAction();
                    Utils.removeFromMapMap(this.actionToOutStateTransitions, action, edge);
                    Utils.removeFromMapMap(this.stateToInStateTransitions, target, edge);
                    Utils.removeFromMapMap(this.stateToOutStateTransitions, edge.getSource(), edge);
                }
            }
        }
        {
            ActivityNode an = this.getActivityNode(state.getActivity());
            an.removeState(state);
        }
        {
            Map<Name, Set<ModelAction>> actions = Utils.getMapFromMap(nameToActions, state.getActivity());
            for (ModelAction action : state.getActions()) {
                this.unvisitedActions.remove(action);
                this.visitedActions.remove(action);
                if (action.requireTarget()) {
                    Utils.removeFromMapSet(actions, action.getTarget(), action);
                    if (actions.get(action.getTarget()).isEmpty()) {
                        actions.remove(action.getTarget());
                    }
                }
            }
        }
        {
            this.entryStates.remove(state);
            this.cleanEntryStates.remove(state);
            this.keyToState.remove(state.getStateKey());
        }
        {
            Utils.removeFromMapSet(namingToStates, state.getCurrentNaming(), state);
        }
    }

    public Set<State> getStates(Naming naming) {
        Set<State> states = this.namingToStates.get(naming);
        if (states == null) {
            return Collections.emptySet();
        }
        return states;
    }

    public Set<State> getAllStates(Naming naming) {
        Set<State> states = new HashSet<State>();
        getAllStates(states, naming);
        return states;
    }

    private void getAllStates(Set<State> states, Naming naming) {
        states.addAll(getStates(naming));
        for (Naming child : naming.getChildren()) {
            getAllStates(states, child);
        }
    }

    void rebuildHistory() {
        if (stateTransitionHistory == null) {
            stateTransitionHistory = new ArrayList<>(this.treeTransitionHistory.size() << 1);
        } else {
            stateTransitionHistory.clear();
        }
        for (GUITreeTransition tt : this.treeTransitionHistory) {
            StateTransition edge = (StateTransition) tt.getCurrentStateTransition();
            int fv = edge.getFirstVisitedTimestamp();
            int lv = edge.getLastVisitedTimestamp();
            if (fv == -1 || fv > tt.getTimestamp()) {
                edge.firstVisitTimestamp = fv;
            }
            if (lv == -1 || lv < tt.getTimestamp()) {
                edge.lastVisitTimestamp = lv;
            }
            edge.visitedCount++;
            stateTransitionHistory.add(edge);
        }
    }

    public List<GUITreeTransition> getTreeHistory() {
        return treeTransitionHistory;
    }

    public boolean contains(State state) {
        State check = this.keyToState.get(state.getStateKey());
        if (check == null) {
            return false;
        }
        if (check != state) {
            Logger.wprintln("Duplicated states with the same key");
            Logger.wprintln("   get: " + state);
            Logger.wprintln("expect: " + check);
            throw new RuntimeException("Sanity check failed!");
        }
        return check == state;
    }

    public void addEntryGUITree(GUITree tree) {
        this.entryGUITrees.add(tree);
    }

    public void addCleanEntryGUITree(GUITree tree) {
        this.cleanEntryGUITrees.add(tree);
    }

    public boolean isEntryState(State state) {
        return this.entryStates.contains(state);
    }

    public boolean isCleanEntryState(State state) {
        return this.cleanEntryStates.contains(state);
    }

    public List<Subsequence> moveToState(State from, State to, boolean includeBack, int maxLength) {
        List<Subsequence> selectedPaths = new ArrayList<>();
        moveToState(selectedPaths, from, to, includeBack, maxLength);
        return selectedPaths;
    }
    public List<Subsequence> moveToState(State from, State to, boolean includeBack) {
        List<Subsequence> selectedPaths = new ArrayList<>();
        moveToState(selectedPaths, from, to, includeBack, Integer.MAX_VALUE);
        return selectedPaths;
    }

    public List<Subsequence> moveToState(State from, State to, int pathLength) {
        List<Subsequence> selectedPaths = new ArrayList<>();
        moveToState(selectedPaths, from, to, true, pathLength);
        return selectedPaths;
    }

    public Set<StateTransition> remove(State state) {
        Set<StateTransition> removed = new HashSet<>();
        remove(state, removed);
        return removed;
    }

    public List<Collection<StateTransition>> getTransitionsByName(ModelAction action) {
        List<Collection<StateTransition>> results = new ArrayList<>();
        if (!action.requireTarget()) {
            return Collections.emptyList();
        }
        Set<ModelAction> actions = Utils.getFromMapMap(nameToActions, action.getState().getActivity(), action.getTarget());
        if (actions == null || actions.isEmpty()) {
            return Collections.emptyList();
        }
        for (ModelAction a : actions) {
            if (a.getType().equals(action.getType())) {
                results.add(getOutStateTransitions(a));
            }
        }
        return results;
    }


}
