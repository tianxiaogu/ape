package com.android.commands.monkey.ape.model;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.android.commands.monkey.ape.ActionFilter;
import com.android.commands.monkey.ape.agent.StatefulAgent;
import com.android.commands.monkey.ape.naming.Name;
import com.android.commands.monkey.ape.naming.NamerComparator;
import com.android.commands.monkey.ape.naming.NamerFactory;
import com.android.commands.monkey.ape.naming.Naming;
import com.android.commands.monkey.ape.tree.GUITree;
import com.android.commands.monkey.ape.tree.GUITreeNode;
import com.android.commands.monkey.ape.utils.Logger;
import com.android.commands.monkey.ape.utils.RandomHelper;
import com.android.commands.monkey.ape.utils.Utils;

import android.content.ComponentName;

public class State extends GraphElement {



    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private StateKey stateKey;
    private ModelAction[] actions;
    private ModelAction backAction;

    private List<GUITree> treeHistory;

    public State(StateKey stateKey) {
        this.stateKey = stateKey;
        List<ModelAction> c = new ArrayList<ModelAction>();
        for (Name widget : stateKey.getWidgets()) {
            buildActions(c, widget);
        }
        backAction = new ModelAction(this, ActionType.MODEL_BACK);
        c.add(backAction);
        actions = c.toArray(new ModelAction[c.size()]);
    }

    /**
     * GUI actions, i.e., action with a target
     * @return
     */
    public List<ModelAction> targetedActions() {
        List<ModelAction> results = new ArrayList<ModelAction> (actions.length);
        collectActions(results, ActionFilter.WITH_TARGET);
        return results;
    }

    public List<ModelAction> collectActions(ActionFilter filter) {
        List<ModelAction> results = new ArrayList<ModelAction> (actions.length);
        collectActions(results, filter);
        return results;
    }

    public void collectActions(Collection<ModelAction> results, ActionFilter filter) {
        for (ModelAction action : actions) {
            if (filter.include(action)) {
                results.add(action);
            }
        }
    }

    public int countActions(ActionFilter filter, boolean includeBack) {
        int count = 0;
        for (ModelAction action : actions) {
            if (!includeBack && action.isBack()) {
                continue;
            }
            if (filter.include(action)) {
                count++;
            }
        }
        return count;
    }

    private int countActionPriority(ActionFilter filter, boolean includeBack) {
        int totalPriority = 0;
        for (ModelAction action : actions) {
            if (!includeBack && action.getType().equals(ActionType.MODEL_BACK)) {
                continue;
            }
            if (filter.include(action)) {
                if (action.getPriority() <= 0) {
                    throw new IllegalStateException(
                            "Action should has a positive priority, but we get " + action.getPriority());
                }
                totalPriority += action.getPriority();
            }
        }
        return totalPriority;
    }

    public ModelAction greedyPickLeastVisited(ActionFilter filter) {
        ModelAction minAction = null;
        int minValue = Integer.MAX_VALUE;
        for (ModelAction action : actions) {
            if (!filter.include(action)) {
                continue;
            }
            if (action.getVisitedCount() < minValue) {
                minValue = action.getVisitedCount();
                minAction = action;
            }
        }
        return minAction;
    }

    public ModelAction randomlyPickAction(Random random, ActionFilter filter) {
        return randomlyPickAction(random, filter, true);
    }

    public ModelAction randomlyPickAction(Random random, ActionFilter filter, boolean includeBack) {
        int total = countActionPriority(filter, includeBack);
        if (total == 0) {
            return null;
        }

        int index = random.nextInt(total);
        return pickAction(index, filter, includeBack);
    }

    public boolean containsTarget(Name target) {
        return stateKey.containsTarget(target);
    }

    private ModelAction pickAction(int index, ActionFilter filter, boolean includeBack) {
        for (ModelAction action : actions) {
            if (!includeBack && action.getType().equals(ActionType.MODEL_BACK)) {
                continue;
            }
            if (filter.include(action)) {
                int priority = action.getPriority();
                if (priority > index) {
                    return action;
                } else {
                    index = index - priority;
                }
            }
        }
        Logger.println("*** WARNING: You have a non-stable action filter...");
        return null; // this may happen if you filter is not stable.
    }

    public static StateKey buildStateKey(Naming naming, ComponentName activity, Name[] widgets) {
        return NamerFactory.buildStateKey(naming, activity, widgets);
    }

    public static State buildState(StateKey stateKey) {
        State state = new State(stateKey);
        return state;
    }

    public int getCountOfActions() {
        return this.actions.length;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((stateKey == null) ? 0 : stateKey.hashCode());
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
        State other = (State) obj;
        if (stateKey == null) {
            if (other.stateKey != null)
                return false;
        } else if (!stateKey.equals(other.stateKey))
            return false;
        return true;
    }

    private void buildActions(List<ModelAction> actions, Name widget) {
        List<ActionType> actionTypes = NamerFactory.decodeActions(widget);
        for (ActionType actionType : actionTypes) {
            ModelAction action = new ModelAction(this, widget, actionType);
            actions.add(action);
        }
    }

    public List<ModelAction> getActions() {
        return Arrays.asList(actions);
    }

    public String getActivity() {
        return stateKey.getActivity();
    }

    public ModelAction getBackAction() {
        return this.backAction;
    }

    public String toString() {
        return super.toString() + this.stateKey.toString() + "[A=" + this.actions.length + "]";
    }

    public StateKey getStateKey() {
        return this.stateKey;
    }

    public void dumpState() {
        Logger.format("Dumpping state %s", this);
        stateKey.dumpState();
    }

    public void saveState(PrintWriter pw) {
        stateKey.saveState(pw);
        pw.println();
        for (int i = 0; i < actions.length; i++) {
            pw.format("%3d %s\n", i, actions[i]);
        }
    }

    public ModelAction firstEnabledUnvisitedValidAction() {
        return firstAction(ActionFilter.ENABLED_VALID_UNVISITED);
    }

    public ModelAction firstAction(ActionFilter filter) {
        for (ModelAction action : actions) {
            if (filter.include(action)) {
                return action;
            }
        }
        return null;
    }

    public int getCountOfWidgets() {
        return getWidgets().length;
    }

    public Name[] getWidgets() {
        return stateKey.getWidgets();
    }

    public void printActions() {
        int count = 1;
        for (ModelAction e : actions) {
            Logger.format("%5d %s", count++, e.toFullString());
        }
    }

    public ModelAction randomlyPickUnvisitedAction(Random random) {
        ModelAction action = randomlyPickAction(random, ActionFilter.ENABLED_VALID_UNVISITED, false);
        if (action == null && ActionFilter.ENABLED_VALID_UNVISITED.include(getBackAction())) {
            action = getBackAction();
        }
        return action;
    }

    public ModelAction randomlyPickUnsaturatedAction(Random random) {
        ModelAction action = randomlyPickAction(random, ActionFilter.ENABLED_VALID_UNSATURATED, false);
        if (action == null && ActionFilter.ENABLED_VALID_UNSATURATED.include(getBackAction())) {
            action = getBackAction();
        }
        return action;
    }

    public ModelAction randomlyPickAction(Random random) {
        return randomlyPickAction(random, ActionFilter.ALL);
    }

    public ModelAction randomlyPickValidAction(Random random) {
        return randomlyPickAction(random, ActionFilter.VALID);
    }

    public void append(GUITree tree) {
        Naming naming = stateKey.getNaming();
        if (!tree.getCurrentNaming().equals(naming)) {
            Logger.wprintln("Different naming results in the same state.");
            Logger.wprintln("Expected " + naming);
            if (naming != null)
                naming.dump();
            Naming get = tree.getCurrentNaming();
            Logger.wprintln("Get: " + get);
            if (get != null) {
                get.dump();
            }
            if (naming != null && get != null) {
                Logger.wprintln("Equivalent? " + naming.equivalent(get));
            }
            // throw new RuntimeException("Different naming results in the same
            // state.");
        }
        if (tree.getCurrentState() == this) {
            Logger.println(this);
            throw new RuntimeException("Cannot be appended twice.");
        }
        tree.setCurrentState(this);
        if (treeHistory == null) {
            treeHistory = new ArrayList<GUITree>();
        }
        treeHistory.add(tree);
    }

    public List<GUITree> getGUITrees() {
        if (this.treeHistory == null) {
            return Collections.emptyList();
        }
        return this.treeHistory;
    }

    public GUITree getLatestGUITree() {
        if (treeHistory == null) {
            return null;
        }
        return treeHistory.get(treeHistory.size() - 1);
    }

    public ModelAction resolveAction(StatefulAgent agent, ModelAction action, int throttle) {
        if (treeHistory == null) {
            throw new IllegalStateException("Empty GUI tree history");
        }
        GUITree latest = getLatestGUITree();
        if (!action.getType().requireTarget()) {
            action.resolveAt(agent.getTimestamp(), throttle, latest, null, null);
            return action;
        }
        GUITreeNode[] nodes = latest.pickNodes(action);
        GUITreeNode node = RandomHelper.randomPick(Arrays.asList(nodes));
        action.resolveAt(agent.getTimestamp(), throttle, latest, node, nodes);
        return action;
    }

    /**
     * 
     * @param action
     * @return
     */
    public ModelAction relocate(ModelAction action) {
        if (action.getState().equals(this)) {
            return action;
        }
        Name target = action.getTarget();
        ActionType type = action.getType();
        if (target == null) {
            for (ModelAction a : getActions()) {
                if (type.equals(a.getType()) && a.getTarget() == null) {
                    return a;
                }
            }
            return null;
        }
        List<ModelAction> candidates = new ArrayList<>();
        for (ModelAction a : getActions()) {
            if (!a.requireTarget()) {
                continue;
            }
            if (type.equals(a.getType())) {
                if (target.refinesTo(a.getTarget())
                        || a.getTarget().refinesTo(target)) {
                    candidates.add(a);
                }
            }
        }
        if (!candidates.isEmpty()) {
            Logger.iformat("Relocating finds %d candidates", candidates.size());
            if (candidates.size() == 1) {
                return candidates.get(0);
            }
            Collections.sort(candidates, new Comparator<ModelAction>() {
                @Override
                public int compare(ModelAction o1, ModelAction o2) {
                    return - NamerComparator.INSTANCE.compare(o1.getTarget().getNamer(), o2.getTarget().getNamer());
                }
            });
            for (ModelAction c : candidates) {
                Logger.iformat("- %s", c);
            }
            return candidates.get(0);
        }
        return null;
    }

    public int getCountOfTargetNodes(String target) {
        if (treeHistory == null) {
            throw new IllegalStateException("Empty GUI tree history");
        }
        GUITree latest = getLatestGUITree();
        return latest.getCountOfTargetNodes(target);
    }

    public boolean isTrivialState() {
        return this.stateKey.isTrivialState();
    }

    public void printWidgets() {
        stateKey.printWidgets();
    }

    public ModelAction getAction(Name widget, ActionType type) {
        if (!containsTarget(widget)) {
            this.dumpState();
            throw new IllegalStateException("No such widget [" + widget + "]");
        }
        for (ModelAction action : actions) {
            if (widget.equals(action.getTarget()) && type.equals(action.getType())) {
                return action;
            }
        }
        this.dumpState();
        throw new IllegalStateException("No such action [" + type + "@" + widget + "]");
    }

    public ModelAction getAction(ActionType type) {
        for (ModelAction action : actions) {
            if (action.requireTarget() == false && type.equals(action.getType())) {
                return action;
            }
        }
        throw new IllegalStateException("No such action [" + type + "]");
    }

    public Naming getCurrentNaming() {
        return stateKey.getNaming();
    }

    public float getSaturation() {
        float saturation = 0.0f;
        if (actions.length == 0) {
            return 1.0F;
        }
        for (ModelAction action : actions) {
            saturation += action.getResolvedSaturation();
        }
        return saturation/actions.length;
    }

    public boolean isSaturated() {
        for (ModelAction action : actions) {
            if (!ActionFilter.ENABLED_VALID.include(action)) {
                continue;
            }
            if (!action.isSaturated()) {
                return false;
            }
        }
        return true;
    }

    public GUITree removeLastLastGUITree() {
        if (treeHistory == null || treeHistory.size() <=1) {
            return null;
        }
        return treeHistory.remove(treeHistory.size() - 2);
    }

    public void dumpActions() {
        Utils.dump(actions);
    }

    public List<ModelAction> getUnsaturatedActions() {
        List<ModelAction> actions = new ArrayList<ModelAction>(this.actions.length);
        collectActions(actions, ActionFilter.ENABLED_VALID_UNSATURATED);
        return actions;
    }

    public boolean isBackEnabled() {
        return ActionFilter.ENABLED_VALID.include(this.backAction);
    }
}
