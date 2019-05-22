package com.android.commands.monkey.ape.naming;

import static com.android.commands.monkey.ape.utils.Config.actionRefinementFirst;
import static com.android.commands.monkey.ape.utils.Config.actionRefinmentThreshold;
import static com.android.commands.monkey.ape.utils.Config.maxInitialNamesPerStateThreshold;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.android.commands.monkey.ape.model.Model;
import com.android.commands.monkey.ape.model.ModelAction;
import com.android.commands.monkey.ape.model.State;
import com.android.commands.monkey.ape.model.StateKey;
import com.android.commands.monkey.ape.model.StateTransition;
import com.android.commands.monkey.ape.tree.GUITree;
import com.android.commands.monkey.ape.tree.GUITreeBuilder;
import com.android.commands.monkey.ape.tree.GUITreeNode;
import com.android.commands.monkey.ape.tree.GUITreeTransition;
import com.android.commands.monkey.ape.tree.GUITreeWidgetDiffer;
import com.android.commands.monkey.ape.utils.Config;
import com.android.commands.monkey.ape.utils.Logger;
import com.android.commands.monkey.ape.utils.Utils;

import android.os.SystemClock;

public class NamingFactory implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 5487802543922135315L;

    enum Type {
        STATE_REFINEMENT, ACTION_REFINEMENT, STATE_ABSTRACTION, ACTION_ABSTRACTION,
    }

    private static final boolean debug = false;

    private PriorityQueue<Predicate> predicates = new PriorityQueue<Predicate>();

    // Avoid repeatedly refinement/abstraction
    private Set<ModelAction> actionRefinementBlacklist = new HashSet<>();
    private Set<ModelAction> NDActionBlacklist = new HashSet<>();
    private Map<GUITree, Set<Naming>> guiTreeNamingBlaclist = new HashMap<>();

    private final Naming base;
    private final Naming top;
    private final Naming bottom;

    public NamingFactory() {
        base = createBaseNaming();
        top = createTopNaming();
        bottom = createBottomNaming();
    }

    public Naming getBaseNaming() {
        return base;
    }

    public Naming getTopNaming() {
        return top;
    }

    public Naming getBottomNaming() {
        return bottom;
    }

    static class RefinementResult {
        boolean actionRefinement;
        Naming originalNaming;
        Naming updatedNaming;
        Namelet originalNamelet;
        Namelet updatedNamelet;
        StateTransition stateTransition1;
        StateTransition stateTransition2;
        List<GUITreeTransition> treeTransitions1;
        List<GUITreeTransition> treeTransitions2;
        public Set<StateKey> states1 = Collections.emptySet();
        public Set<StateKey> states2 = Collections.emptySet();
        public Map<StateKey, List<GUITreeTransition>> stateToTrees1 = Collections.emptyMap();
        public Map<StateKey, List<GUITreeTransition>> stateToTrees2 = Collections.emptyMap();

        public RefinementResult(boolean actionRefinement, Naming origin, Naming update,
                Namelet originalNamelet, Namelet updatedNamelet, StateTransition stateTransition1,
                StateTransition stateTransition2, List<GUITreeTransition> treeTransitions1,
                List<GUITreeTransition> treeTransitions2) {
            this.actionRefinement = actionRefinement;
            this.originalNaming = origin;
            this.updatedNaming = update;
            this.originalNamelet = originalNamelet;
            this.updatedNamelet = updatedNamelet;
            this.stateTransition1 = stateTransition1;
            this.stateTransition2 = stateTransition2;
            this.treeTransitions1 = treeTransitions1;
            this.treeTransitions2 = treeTransitions2;
        }
    }

    public Model resolveNonDeterminism(Model model, StateTransition nst) {
        ModelAction action = nst.getAction();
        Logger.println("=== Try to resolve the non-determinism!!");
        if (action.isBack()) {
            Logger.format("No refinement on BACK actions: ", nst);
            return model;
        }
        List<GUITreeTransition> edgeGUITransitions = model.getGUITreeTransitions(nst);
        Collection<StateTransition> outStateTransitions = model.getGraph().getOutStateTransitions(action);
        {
            if (!NDActionBlacklist.contains(nst.getAction())) {
                for (StateTransition e : outStateTransitions) {
                    if (e == nst) {
                        continue;
                    }
                    Logger.iprintln("Find the non-deterministic edge");
                    Logger.iprintln("st1: " + e);
                    Logger.iprintln("st2: " + nst);

                    GUITreeWidgetDiffer differ = new GUITreeWidgetDiffer();
                    differ.diff(e.getTarget(), nst.getTarget());
                    differ.print();
                    List<RefinementResult> results = refine(model, e, nst, model.getGUITreeTransitions(e), edgeGUITransitions);
                    RefinementResult ret = filterRefinementResult(model, results);
                    if (ret != null) {
                        Logger.iprintln("Find a new naming that can resolve the non-deterministic transition.");
                        return rebuild(model, ret);
                    } else {
                        Logger.iprintln("Cannot find a new naming that can resolve the non-deterministic transition.");
                    }
                }
                if (outStateTransitions.size() >= 3) {
                    Logger.iformat("Add non-deterministic transtions caused by action %s to refinement blacklist.", nst.getAction());
                    NDActionBlacklist.add(nst.getAction());
                }
            } else {
                Logger.iformat("Reject refining non-deterministic transtions caused by action %s.", nst.getAction());
            }
        }
        return model;
    }

    private Predicate createAssertStatesFewerThan(Naming updatedNaming, Model model, Set<GUITree> affected, int threshold) {
        List<GUITree> trees = new ArrayList<>(affected.size());
        for (GUITree tree : affected) {
            trees.add(tree);
        }
        AssertStatesFewerThan p = new AssertStatesFewerThan(updatedNaming, Collections.unmodifiableList(trees), threshold);
        return p;
    }

    void refine(List<RefinementResult> results, NamingManager nm, Set<GUITree> affected, Naming currentNaming, StateTransition st1, StateTransition st2,
            List<GUITreeTransition> tts1, List<GUITreeTransition> tts2) {
        if (actionRefinementFirst && results.isEmpty()) {
            Logger.iprintln("Try action refinement, where source states are not necessarily to be the same.");
            actionRefinement(results, nm, affected, currentNaming, st1, st2, tts1, tts2);
            Logger.iprintln("Action refinement has " + results.size() + " results.");
        }
        if (results.isEmpty()) {
            Logger.iprintln("Try state refinement.");
            stateRefinement(results, nm, affected, currentNaming, st1, st2, tts1, tts2);
            Logger.iprintln("State refinement has " + results.size() + " results.");
        }
        if (!actionRefinementFirst && results.isEmpty()) {
            Logger.iprintln("Try action refinement, where source states are not necessarily to be the same.");
            actionRefinement(results, nm, affected, currentNaming, st1, st2, tts1, tts2);
            Logger.iprintln("Action refinement has " + results.size() + " results.");
        }
    }

    /**
     * 
     * @param model
     * @param rr
     * @return
     */
    Model rebuild(Model model, RefinementResult rr) {
        if (rr == null) {
            return model;
        }
        updateNamingManager(model, model.getNamingManager(), rr);
        return model.rebuild();
    }

    List<RefinementResult> refine(Model model, StateTransition st1, StateTransition st2, List<GUITreeTransition> tts1,
            List<GUITreeTransition> tts2) {
        long begin = SystemClock.elapsedRealtimeNanos();
        try {
            Naming currentNaming = st1.getSource().getCurrentNaming();
            if (currentNaming == null) {
                return Collections.emptyList(); // conflict naming
            }
            Set<GUITree> affected = new HashSet<>(st1.getSource().getGUITrees());
            return refine(model.getNamingManager(), affected, currentNaming, st1, st2, tts1, tts2);
        } catch (RuntimeException e) {
            throw e;
        } finally {
            long end = SystemClock.elapsedRealtimeNanos();
            Logger.iformat("Refinement takes %s ms.", TimeUnit.NANOSECONDS.toMillis(end - begin));
        }
    }

    List<RefinementResult> refine(NamingManager nm, Set<GUITree> affected, Naming currentNaming, StateTransition st1, StateTransition st2,
            List<GUITreeTransition> tts1, List<GUITreeTransition> tts2) {
        List<RefinementResult> results = new ArrayList<>();
        refine(results, nm, affected, currentNaming, st1, st2, tts1, tts2);
        return results;
    }

    private void stateRefinement(List<RefinementResult> results, NamingManager nm, Set<GUITree> affected, Naming currentNaming, StateTransition st1,
            StateTransition st2, List<GUITreeTransition> tts1, List<GUITreeTransition> tts2) {
        GUITreeTransition tt1 = tts1.get(tts1.size() - 1);
        GUITreeTransition tt2 = tts2.get(tts2.size() - 1);
        if (isTopNamingEquivalent(tt1.getSource(), tt2.getSource())) {
            Logger.iprintln("Two GUI trees are top naming equivalent..");
            if (isIsomorphic(tt1.getSource(), tt2.getSource())) {
                Logger.iprintln("Two GUI trees are top naming and isomorphic..");
            }
            return;
        }
        State sourceState1 = st1.getSource();
        if (sourceState1 != st2.getSource()) {
            throw new IllegalStateException("Source states should be the same!");
        }

        Set<Name> candidates = new HashSet<>();
        for (ModelAction action : sourceState1.getActions()) {
            if (action.requireTarget()) {
                candidates.add(action.getTarget());
            }
        }
        {
            Namelet last = currentNaming.getLastNamelet();
            Namer lastNamer = last.getNamer();
            if (currentNaming.isReplaceable(last)) {
                Namelet parent = last.getParent();
                Namer parentNamer = parent.getNamer();
                List<Namer> refinedNamers = NamerFactory.getSortedAbove(parentNamer);
                List<Namer> upperBounds = new ArrayList<>();
                outer: for (Namer refined : refinedNamers) {
                    if (!upperBounds.isEmpty()) {
                        for (Namer upper : upperBounds) {
                            if (refined.refinesTo(upper)) {
                                continue outer; // no retry 
                            }
                        }
                    }
                    if (lastNamer.refinesTo(refined)) {
                        continue; // avoid replace with the same namelet.
                    }
                    Namelet newNamelet = new Namelet(last.getExprString(), refined);
                    Naming newNaming = currentNaming.replaceLast(last, newNamelet);
                    if (!checkStateRefinement(newNaming, refined, tts1, tts2, upperBounds)) {
                        continue;
                    }
                    if (!checkPredicate(nm, affected, newNaming)) {
                        continue;
                    }
                    results.add(new RefinementResult(true, currentNaming, newNaming, last, newNamelet, st1, st2, tts1, tts2));
                    break;
                }
            }
        }

        for (Name name : candidates) {
            String xpathStr = NamerFactory.nameToXPathString(name);
            Namelet currentNamelet = checkNamelet(currentNaming, name, tts1, tts2);
            if (currentNamelet == null) {
                continue;
            }
            Namer currentNamer = name.getNamer();
            List<Namer> refinedNamers = NamerFactory.getSortedAbove(currentNamer);
            List<Namer> upperBounds = new ArrayList<>();
            Set<Namer> visited = new HashSet<Namer>();
            visited.add(currentNamer);
            LinkedList<Namer> queue = new LinkedList<Namer>();
            collectSortedAbove(currentNamer, queue, visited);
            outer: for (Namer refined : refinedNamers) {
                if (!upperBounds.isEmpty()) {
                    for (Namer upper : upperBounds) {
                        if (refined.refinesTo(upper)) {
                            continue outer; // no retry 
                        }
                    }
                }
                Namelet newNamelet = new Namelet(xpathStr, refined);
                Naming newNaming = currentNaming.extend(currentNamelet, newNamelet);
                if (!checkStateRefinement(newNaming, refined, tts1, tts2, upperBounds)) {
                    continue;
                }
                if (!checkPredicate(nm, affected, newNaming)) {
                    continue;
                }
                results.add(new RefinementResult(false, currentNaming, newNaming, currentNamelet, newNamelet, st1, st2, tts1, tts2));
                break;
            }
        }
    }

    /**
     * Smaller is better
     */
    static Comparator<RefinementResult> comparator = new Comparator<RefinementResult>() {

        @Override
        public int compare(RefinementResult o1, RefinementResult o2) {
            int ret = 0;
            if (o1.states1 != null && o2.states1 != null) {// compare the size
                // of states
                int sizeOfStates1OfO1 = o1.states1.size();
                int sizeOfStates2OfO1 = o1.states2.size();
                int sizeOfStates1OfO2 = o2.states1.size();
                int sizeOfStates2OfO2 = o2.states2.size();
                ret = sizeOfStates1OfO1 + sizeOfStates2OfO1 - sizeOfStates1OfO2 - sizeOfStates2OfO2;
                if (ret != 0) {
                    return ret; // prefer the one with fewer states
                }
            }
            {
                ret = NamerComparator.INSTANCE.compare(o1.originalNamelet.getNamer(), o2.originalNamelet.getNamer());
                if (ret != 0) {
                    return ret;
                }
            }
            {
                ret = NamerComparator.INSTANCE.compare(o1.updatedNamelet.getNamer(), o2.updatedNamelet.getNamer());
                if (ret != 0) {
                    return ret;
                }
            }
            return o1.updatedNamelet.getExprString().compareTo(o2.updatedNamelet.getExprString());
        }
    };

    private boolean checkPredicate(NamingManager nm, Set<GUITree> affected, Naming naming) {
        for (GUITree tree : affected) {
            if (Utils.containsMapSet(guiTreeNamingBlaclist, tree, naming)) {
                Logger.iformat("Naming %s has been blacklisted for GUI Tree #%d", naming, tree.getTimestamp());
                return false;
            }
        }
        for (Predicate p : predicates) {
            boolean ret = p.eval(nm, affected, naming);
            if (ret == false) {
                Logger.iformat("Naming %s violates constraints %s. ", naming, p);
                return false;
            }
        }
        return true;
    }

    private void removeConflictPredicates(NamingManager nm, Set<GUITree> affected, Naming naming) {
        List<Predicate> toBeRemoved = new ArrayList<Predicate>();
        for (Predicate p : predicates) {
            if (p.eval(nm, affected, naming) == false) {
                Logger.iprintln("Remove violated constraint: " + p);
                toBeRemoved.add(p);
            }
        }
        if (!toBeRemoved.isEmpty()) {
            predicates.removeAll(toBeRemoved);
        }
    }

    private void logRefinement(RefinementResult rr) {
        Map<StateKey, List<GUITreeTransition>> map1 = rr.stateToTrees1;
        Map<StateKey, List<GUITreeTransition>> map2 = rr.stateToTrees2;
        List<List<GUITreeTransition>> ts = new ArrayList<>(map1.size() + map2.size());
        for (List<GUITreeTransition> t : map1.values()) {
            ts.add(t);
        }
        for (List<GUITreeTransition> t : map2.values()) {
            ts.add(t);
        }
        Predicate p;
        if (rr.actionRefinement) {
            p = new AssertActionDivergent(rr.updatedNaming, ts);
        } else {
            p = new AssertSourceDivergent(rr.updatedNaming, ts);
        }
        predicates.add(p);
    }

    private void sortRefinementResults(List<RefinementResult> candidates) {
        Collections.sort(candidates, comparator);
        Logger.iformat("Find %d new naming", candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            RefinementResult rr = candidates.get(i);
            Naming oldOne = rr.originalNaming;
            Naming newOne = rr.updatedNaming;
            Logger.format("[%d] Updating naming: from %s to %s, [%d] <-> [%d].", i, oldOne, newOne, rr.states1.size(),
                    rr.states2.size());
            Logger.println("==================== Old =========================");
            oldOne.dump();
            Logger.println("--------------------------------------------------");
            newOne.dump();
            Logger.println("==================== New =========================");
        }
    }

    private RefinementResult filterRefinementResult(Model model, List<RefinementResult> candidates) {
        if (candidates.isEmpty()) {
            Logger.iprintln("No state refinement candidates to filter.");
            return null;
        }
        sortRefinementResults(candidates);
        return candidates.get(0);
    }

    private Model rebuild(Model model, Collection<GUITree> affected, Naming newNaming) {
        NamingManager nm = model.getNamingManager();
        for (GUITree tree : affected) {
            nm.updateNaming(tree, newNaming);
        }
        return model.rebuild();
    }

    private void updateNamingManager(Model model, NamingManager nm, RefinementResult rr) {
        Naming newOne = rr.updatedNaming;
        for (GUITree tree : rr.stateTransition1.getSource().getGUITrees()) {
            nm.updateNaming(tree, newOne);
        }
        if (debug) {
            Iterator<GUITree> trees = model.getGUITrees();
            Map<GUITree, Naming> updated = nm.sync(trees);
            for (Entry<GUITree, Naming> entry : updated.entrySet()) {
                GUITree tree = entry.getKey();
                Naming naming = entry.getValue();
                Logger.iformat("Update GUITree at %d, state is %s, current naming is %s, updated naming is %s.",
                        tree.getTimestamp(), tree.getCurrentState(), tree.getCurrentNaming(), naming);
            }
        }
        logRefinement(rr);
    }

    private boolean checkStateRefinement(Naming newNaming, Namer newNamer, List<GUITreeTransition> tts1,
            List<GUITreeTransition> tts2, List<Namer> upperBounds) {
        {
            GUITreeTransition tt1 = tts1.get(tts1.size() - 1);
            GUITreeTransition tt2 = tts2.get(tts2.size() - 1);
            GUITree sourceTree1 = tt1.getSource();
            GUITree sourceTree2 = tt2.getSource();
            StateKey newSourceState1 = GUITreeBuilder.getStateKey(newNaming, sourceTree1);
            StateKey newSourceState2 = GUITreeBuilder.getStateKey(newNaming, sourceTree2);
            if (newSourceState1.equals(newSourceState2)) {
                return false;
            }
        }
        {
            int threshold = getMaxStatesForRefinementThreshold(newNaming);
            Set<StateKey> states1 = new HashSet<>();
            Set<StateKey> states2 = new HashSet<>();
            Map<StateKey, List<GUITreeTransition>> stateToTrees1 = new HashMap<>();
            Map<StateKey, List<GUITreeTransition>> stateToTrees2 = new HashMap<>();

            for (GUITreeTransition tt1 : tts1) {
                StateKey stateKey = GUITreeBuilder.getStateKey(newNaming, tt1.getSource());
                states1.add(stateKey);
                Utils.addToMapList(stateToTrees1, stateKey, tt1);
                if (states1.size() > threshold) {
                    upperBounds.add(newNamer);
                    return false;
                }
            }
            for (GUITreeTransition tt2 : tts2) {
                // intersection
                StateKey stateKey = GUITreeBuilder.getStateKey(newNaming, tt2.getSource());
                if (states1.contains(stateKey)) {
                    return false;
                }
                states2.add(stateKey);
                Utils.addToMapList(stateToTrees2, stateKey, tt2);
                if (states1.size() + states2.size() > threshold) {
                    upperBounds.add(newNamer);
                    return false;
                }
            }
        }
        upperBounds.add(newNamer);
        return true;
    }

    private void collectSortedAbove(Namer cn, LinkedList<Namer> queue, Set<Namer> visited) {
        EnumSet<NamerType> newTypes = EnumSet.copyOf(cn.getNamerTypes());
        for (NamerType type : NamerType.used) {
            if (newTypes.add(type)) {
                Namer n = NamerFactory.getNamer(newTypes);
                if (!visited.contains(n)) {
                    visited.add(n);
                    queue.add(n);
                }
                newTypes.remove(type);
            }
        }
    }

    private void reportConflictNamelet(Namelet namelet1, Namelet namelet2) {
        Logger.wprintln("=== Conflict namelets..");
        Logger.wprintln("=== Namelet1: " + namelet1);
        Logger.wprintln("=== Namelet2: " + namelet2);
    }

    private void reportConflictNamer(Namer namer1, Namer namer2) {
        Logger.wprintln("=== Conflict namer..");
        Logger.wprintln("=== Namer1: " + namer1);
        Logger.wprintln("=== Namer2: " + namer2);
    }

    protected boolean checkActionRefinement(Naming newNaming, Namer newNamer,
            List<GUITreeTransition> tts1, List<GUITreeTransition> tts2, List<Namer> upperBounds) {
        Set<Name> names = new HashSet<>();
        for (GUITreeTransition tt1 : tts1) {
            GUITree sourceTree1 = tt1.getSource();
            GUITreeNode node1 = tt1.getAction().getGUITreeNode();
            Name name1 = GUITreeBuilder.getNodeName(newNaming, sourceTree1, node1);
            names.add(name1);
        }
        for (GUITreeTransition tt2 : tts2) {
            GUITree sourceTree2 = tt2.getSource();
            GUITreeNode node2 = tt2.getAction().getGUITreeNode();
            Name name2 = GUITreeBuilder.getNodeName(newNaming, sourceTree2, node2);
            if (names.add(name2) == false) {
                return false;
            }
        }
        {
            int threshold = getMaxStatesForRefinementThreshold(newNaming);
            Set<StateKey> states = new HashSet<>();

            for (GUITreeTransition tt1 : tts1) {
                StateKey stateKey = GUITreeBuilder.getStateKey(newNaming, tt1.getSource());
                states.add(stateKey);
                if (states.size() > threshold) {
                    upperBounds.add(newNamer);
                    return false;
                }
            }
            for (GUITreeTransition tt2 : tts2) {
                // intersection
                StateKey stateKey = GUITreeBuilder.getStateKey(newNaming, tt2.getSource());
                states.add(stateKey);
                if (states.size() > threshold) {
                    upperBounds.add(newNamer);
                    return false;
                }
            }

        }
        upperBounds.add(newNamer);
        return true;
    }

    private boolean isSharedAction(Name widget, List<GUITreeTransition> tts1, List<GUITreeTransition> tts2) {
        // Action should be on shared names
        Iterator<GUITree> sourceTreeIt1 = GUITreeTransition.sourceTreeIterator(tts1);
        boolean isShared = false;
        while (sourceTreeIt1.hasNext()) {
            GUITree tree1 = sourceTreeIt1.next();
            if (tree1.getNodes(widget).size() > 1) {
                isShared = true;
                break;
            }
        }
        if (!isShared) {
            Iterator<GUITree> sourceTreeIt2 = GUITreeTransition.sourceTreeIterator(tts2);
            while (sourceTreeIt2.hasNext()) {
                GUITree tree2 = sourceTreeIt2.next();
                if (tree2.getNodes(widget).size() > 1) {
                    isShared = true;
                    break;
                }
            }
        }
        return isShared;
    }

    private void actionRefinement(List<RefinementResult> results, NamingManager nm, Set<GUITree> affected, Naming currentNaming, StateTransition st1,
            StateTransition st2, List<GUITreeTransition> tts1, List<GUITreeTransition> tts2) {
        Name widget = st1.getAction().getTarget();
        if (widget == null) {
            return; // no action refinement on actions without a target
        }
        if (!isSharedAction(widget, tts1, tts2)) {
            Logger.iprintln("Action is not shared. No action refinement.");
            return;
        }
        Namelet currentNamelet = checkNamelet(currentNaming, widget, tts1, tts2);
        if (currentNamelet == null) {
            return;
        }
        Namer currentNamer = widget.getNamer();
        {
            if (currentNaming.isReplaceable(currentNamelet)) {
                Namelet parent = currentNamelet.getParent();
                Namer parentNamer = parent.getNamer();
                List<Namer> refinedNamers = NamerFactory.getSortedAbove(parentNamer);
                List<Namer> upperBounds = new ArrayList<>();
                outer: for (Namer refined : refinedNamers) {
                    if (!upperBounds.isEmpty()) {
                        for (Namer upper : upperBounds) {
                            if (refined.refinesTo(upper)) {
                                continue outer; // no retry 
                            }
                        }
                    }
                    if (currentNamer.refinesTo(refined)) {
                        continue; // avoid replace with the same namelet.
                    }
                    Namelet newNamelet = new Namelet(currentNamelet.getExprString(), refined);
                    Naming newNaming = currentNaming.replaceLast(currentNamelet, newNamelet);
                    if (checkActionRefinement(newNaming, refined, tts1, tts2, upperBounds) == false) {
                        continue outer;
                    }
                    if (!checkPredicate(nm, affected, newNaming)) {
                        continue;
                    }
                    results.add(new RefinementResult(true, currentNaming, newNaming, currentNamelet, newNamelet, st1, st2, tts1, tts2));
                    break;
                }
            }
        }
        List<Namer> refinedNamers = NamerFactory.getSortedAbove(currentNamer);
        List<Namer> upperBounds = new ArrayList<>();
        outer: for (Namer refined : refinedNamers) {
            if (!upperBounds.isEmpty()) {
                for (Namer upper : upperBounds) {
                    if (refined.refinesTo(upper)) {
                        continue outer; // no retry 
                    }
                }
            }
            String xpathStr = NamerFactory.nameToXPathString(widget);
            Namelet newNamelet = new Namelet(xpathStr, refined);
            Naming newNaming = currentNaming.extend(currentNamelet, newNamelet);
            if (checkActionRefinement(newNaming, refined, tts1, tts2, upperBounds) == false) {
                continue;
            }
            if (!checkPredicate(nm, affected, newNaming)) {
                continue;
            }
            results.add(new RefinementResult(true, currentNaming, newNaming, currentNamelet, newNamelet, st1, st2, tts1, tts2));
            break;
        }
    }

    private Namelet checkNamelet(Naming naming, Name widget, List<GUITreeTransition> tts1, List<GUITreeTransition> tts2) {
        Namelet namelet = resolveCurrentNamelet(naming, widget, GUITreeTransition.sourceTreeIterator(tts1));
        Namelet namelet2 = resolveCurrentNamelet(naming, widget, GUITreeTransition.sourceTreeIterator(tts2));
        if (namelet == null || namelet2 == null || namelet != namelet2) {
            reportConflictNamelet(namelet, namelet2);
            return null;
        }
        if (!namelet.getNamer().equals(widget.getNamer())) {
            reportConflictNamer(namelet.getNamer(), widget.getNamer());
            return null;
        }
        return namelet;
    }

    private Namelet resolveCurrentNamelet(Naming naming, Name name, Iterator<GUITree> treeIterator) {
        Namelet namelet = null;
        if (!treeIterator.hasNext()) {
            throw new IllegalStateException("Empty tree iterator..");
        }
        while (treeIterator.hasNext()) {
            GUITree tree = treeIterator.next();
            List<GUITreeNode> nodes = tree.getNodes(name);
            if (nodes.isEmpty()) {
                throw new IllegalStateException("No nodes for name " + name);
            }
            for (GUITreeNode node : nodes) {
                if (namelet == null) {
                    namelet = node.getCurrentNamelet();
                } else if (namelet != node.getCurrentNamelet()) {
                    Logger.println("Conflict namelet for %s:");
                    Logger.println("   get: " + node.getCurrentNamelet());
                    Logger.println("expect: " + namelet);
                    return null;
                }
            }
        }
        return namelet;
    }

    boolean isTopNamingEquivalent(GUITree t1, GUITree t2) {
        return isStateEquivalent(getTopNaming(), t1, t2);
    }

    private boolean isStateEquivalent(Naming naming, GUITree t1, GUITree t2) {
        StateKey key1 = GUITreeBuilder.getStateKey(naming, t1);
        StateKey key2 = GUITreeBuilder.getStateKey(naming, t2);
        return key1.equals(key2);
    }

    boolean isBaseNamingEquivalent(GUITree t1, GUITree t2) {
        return isStateEquivalent(getBaseNaming(), t1, t2);
    }

    static boolean isIsomorphic(GUITree t1, GUITree t2) {
        return isIsomorphic(t1.getRootNode(), t2.getRootNode());
    }

    static boolean isIsomorphic(GUITreeNode t1, GUITreeNode t2) {
        // Index
        if (t1.getIndex() != t2.getIndex()) {
            return false;
        }
        // Type
        if (!t1.getClassName().equals(t2.getClassName())) {
            return false;
        }
        if (!t1.getPackageName().equals(t2.getPackageName())) {
            return false;
        }
        if (!t1.getResourceID().equals(t2.getResourceID())) {
            return false;
        }
        if (t1.isEnabled() != t2.isEnabled()) {
            return false;
        }
        // Text
        if (!t1.getText().equals(t2.getText())) {
            return false;
        }
        if (!t1.getContentDesc().equals(t2.getContentDesc())) {
            return false;
        }
        // Action
        if (t1.isClickable() != t2.isClickable()) {
            return false;
        }
        if (t1.isCheckable() != t2.isCheckable()) {
            return false;
        }
        if (t1.isLongClickable() != t2.isLongClickable()) {
            return false;
        }
        if (t1.isScrollable() != t2.isScrollable()) {
            return false;
        }
        Iterator<GUITreeNode> c1 = t1.getChildren();
        Iterator<GUITreeNode> c2 = t2.getChildren();
        while (c1.hasNext() && c2.hasNext()) {
            if (!isIsomorphic(c1.next(), c2.next())) {
                return false;
            }
        }

        if (c1.hasNext() || c2.hasNext()) {
            return false;
        }

        return true;
    }

    private Naming createTopNaming() {
        String exprStr = "//*";
        Namelet namlet = new Namelet(Namelet.Type.BASE, exprStr, NamerFactory.topNamer());
        Naming naming = new Naming(new Namelet[] { namlet });
        return naming;
    }

    private Naming createBottomNaming() {
        String exprStr = "//*";
        Namelet namlet = new Namelet(Namelet.Type.BASE, exprStr, NamerFactory.bottomNamer());
        Naming naming = new Naming(new Namelet[] { namlet });
        return naming;
    }

    /**
     * 
     * @return
     */
    Naming createStoatBaseNaming() {
        List<Namelet> namelets = new ArrayList<Namelet>();
        Namelet root = null;
        { // default
            String exprStr = "//*";
            Namelet namelet = new Namelet(Namelet.Type.BASE, exprStr, NamerFactory.getNamer(NamerType.PARENT, NamerType.TYPE, NamerType.INDEX));
            root = namelet;
            namelets.add(namelet);
        }
        {
            String exprStr = "//*[@class='android.widget.ListView'"
                    + " or @class='android.widget.GridView'"
                    + " or @class='android.support.v7.widget.RecyclerView'"
                    + " or @class='android.support.v17.leanback.widget.VerticalGridView'"
                    + " or @class='android.support.v17.leanback.widget.HorizontalGridView'"
                    + " or @class='android.widget.ExpandableListView'" + "]/*";
            Namelet namelet = new Namelet(exprStr, NamerFactory.getNamer(NamerType.PARENT, NamerType.TYPE));
            namelet.setParent(root);
            namelets.add(namelet);
        }
        Naming naming = new Naming(namelets.toArray(new Namelet[namelets.size()]));
        return naming;

    }

    Naming createActionTypeBaseNaming() {
        List<Namelet> namelets = new ArrayList<Namelet>();
        {
            String exprStr = "//*[@clickable='true' or @long-clickable='true' or @checkable='true' or @scrollable='true']";
            Namelet namlet = new Namelet(Namelet.Type.BASE, exprStr, NamerFactory.getNamer(NamerType.TYPE));
            namelets.add(namlet);
        }
        { // default
            String exprStr = "//*[@clickable='false' and @long-clickable='false' and @checkable='false' and @scrollable='false']";
            Namelet namlet = new Namelet(Namelet.Type.BASE, exprStr, NamerFactory.bottomNamer());
            namelets.add(namlet);
        }
        Naming naming = new Naming(namelets.toArray(new Namelet[namelets.size()]));
        return naming;
    }

    Naming createResourceIDBaseNaming() {
        List<Namelet> namelets = new ArrayList<Namelet>();
        {
            String exprStr = "//*[@resource-id!='']";
            Namelet namlet = new Namelet(Namelet.Type.BASE, exprStr, NamerFactory.getNamer(NamerType.TYPE));
            namelets.add(namlet);
        }
        { // default
            String exprStr = "//*[@resource-id='']";
            Namelet namlet = new Namelet(Namelet.Type.BASE, exprStr, NamerFactory.getNamer(NamerType.TYPE));
            namelets.add(namlet);
        }
        Naming naming = new Naming(namelets.toArray(new Namelet[namelets.size()]));
        return naming;
    }

    Naming createParentIndexBaseNaming() {
        List<Namelet> namelets = new ArrayList<Namelet>();
        {
            String exprStr = "//*";
            Namelet namlet = new Namelet(Namelet.Type.BASE, exprStr,
                    NamerFactory.getNamer(NamerType.PARENT, NamerType.INDEX));
            namelets.add(namlet);
        }
        Naming naming = new Naming(namelets.toArray(new Namelet[namelets.size()]));
        return naming;
    }

    Naming createActionBaseNaming() {
        List<Namelet> namelets = new ArrayList<Namelet>();
        {
            String exprStr = "//*";
            Namelet namlet = new Namelet(Namelet.Type.BASE, exprStr, NamerFactory.bottomNamer());
            namelets.add(namlet);
        }
        Naming naming = new Naming(namelets.toArray(new Namelet[namelets.size()]));
        return naming;
    }

    private Naming createBaseNaming() {
        String type = "actiontype"; // default
        type = Config.get("ape.baseNaming", type);
        if ("stoat".equals(type)) {
            return createStoatBaseNaming();
        }
        if ("resourceid".equals(type)) {
            return createResourceIDBaseNaming();
        }
        if ("actiontype".equals(type)) {
            return createActionTypeBaseNaming();
        }
        if ("parentindex".equals(type)) {
            return createParentIndexBaseNaming();
        }
        return createBoostedBaseNaming();
    }

    Naming createBoostedBaseNaming() {
        List<Namelet> namelets = new ArrayList<Namelet>();

        // default for all nodes
        String exprStr;
        Namelet namelet;
        Naming naming;

        exprStr = "//*[@clickable='true' or @long-clickable='true' or @checkable='true' or @scrollable='true']";
        namelet = new Namelet(Namelet.Type.BASE, exprStr, NamerFactory.getNamer(NamerType.TYPE));
        namelets.add(namelet);
        exprStr = "//*[@clickable='false' and @long-clickable='false' and @checkable='false' and @scrollable='false']";
        namelet = new Namelet(Namelet.Type.BASE, exprStr, NamerFactory.bottomNamer());
        namelets.add(namelet);
        naming = new Naming(namelets.toArray(new Namelet[namelets.size()]));

        exprStr = "//*[@class!='android.widget.ListView'"
                + " and @class!='android.widget.GridView'"
                + " and @class!='android.support.v7.widget.RecyclerView'"
                + " and @class!='android.support.v17.leanback.widget.VerticalGridView'"
                + " and @class!='android.support.v17.leanback.widget.HorizontalGridView'"
                + " and @class!='android.widget.ExpandableListView'" + "]/*";
        Namelet extendedNamelet = new Namelet(exprStr,
                NamerFactory.getNamer(NamerType.PARENT, NamerType.TYPE, NamerType.INDEX));
        naming = naming.extend(namelet, extendedNamelet); // single path

        return naming;
    }

    /**
     * Check whether targetNaming is too fine-grained in comparison with its parent.
     * @param model
     * @param initialNaming
     * @param initialState
     * @param targetNaming, the naming that needs to be checked
     * @param targetStates, all current states created by the target naming and its children naming
     * @return
     */
    public Model batchAbstract(Model model, Naming initialNaming, State initialState,
            Naming targetNaming, Set<State> targetStates) {
        Naming targetParentNaming = targetNaming.getParent();
        if (targetParentNaming == null) {
            return model;
        }
        if (initialNaming.getParent() == null) {
            return model;
        }
        // after pre-condition checking.
        int affectedThreshold = 8;
        Set<State> affectedStates = filterTargets(initialState, targetNaming, targetStates);
        int threshold = getMaxStatesForRefinementThreshold(targetNaming);
        Set<StateKey> targets = new HashSet<>();
        Set<GUITree> affected = new HashSet<>();
        for (State state : affectedStates) {
            for (GUITree tree : state.getGUITrees()) {
                affected.add(tree);
                targets.add(GUITreeBuilder.getStateKey(targetNaming, tree));
            }
        }
        Logger.iformat("batchAbstract: refined targets: %d, affected states: %d, threshold: %d, affected threshold.",
                targets.size(), affectedStates.size(), threshold, affectedThreshold);
        if (affectedStates.size() <= affectedThreshold && targets.size() <= threshold) {
            Logger.iformat("batchAbstract: refined targets: %d <= threshold: %d ", targets.size(), threshold);
            Logger.iformat("batchAbstract: affected states: %d <= threshold: %d ", affectedStates.size(), affectedThreshold);
            return model;
        }
        Logger.iformat("Revert a naming from %s to %s, affect %d states and %d trees", targetNaming, targetParentNaming,
                affectedStates.size(), affected.size());
        for (StateKey key : targets) {
            Logger.iformat("- %s", key);
        }
        Predicate p = createAssertStatesFewerThan(targetParentNaming, model, affected, threshold);
        NamingManager nm = model.getNamingManager();
        blacklistRefinement(affected, targetParentNaming, targetNaming);
        model = rebuild(model, affected, targetParentNaming);
        removeConflictPredicates(nm, affected, targetParentNaming);
        this.predicates.add(p);
        return model;
    }

    private void blacklistRefinement(Set<GUITree> affected, Naming targetParentNaming, Naming targetNaming) {
        for (GUITree tree : affected) {
            Logger.iformat("Blacklist naming %s GUI Tree #%d", targetNaming, tree.getTimestamp());
            Utils.addToMapSet(guiTreeNamingBlaclist, tree, targetNaming);
        }
    }

    private Set<State> filterTargets(State initialState, Naming targetNaming, Set<State> targetStates) {
        if (initialState.getGUITrees().isEmpty()) {
            throw new IllegalArgumentException("Initial state has no GUI trees.");
        }
        GUITree tree = initialState.getLatestGUITree();
        Naming targetParentNaming = targetNaming.getParent();
        if (targetParentNaming == null) {
            throw new IllegalArgumentException("The parent naming of the target naming is null.");
        }
        StateKey originState = GUITreeBuilder.getStateKey(targetParentNaming, tree);
        Set<State> filteredTargets = new HashSet<>();
        // Logger.iformat("Original state %s", originState);
        for (State state : targetStates) {
            // Logger.iformat("Checking state %s", state);
            if (state.getGUITrees().isEmpty()) {
                continue;
            }
            if (targetNaming != state.getCurrentNaming() && !targetNaming.isAncestor(state.getCurrentNaming())) {
                targetNaming.dump();
                state.getCurrentNaming().dump();
                throw new IllegalStateException("Target naming is invalid.");
            }
            StateKey oldState = GUITreeBuilder.getStateKey(targetParentNaming, state.getLatestGUITree());
            if (originState.equals(oldState)) {
                filteredTargets.add(state);
            }
        }
        return filteredTargets;
    }

    private int getMaxStatesForRefinementThreshold(Naming targetNaming) {
        int finness = targetNaming.getFineness();
        int total = NamerType.used.length;
        int shift = total - finness;
        int threshold = Math.min(8, Math.max(1, 2 << shift));
        return threshold;
    }

    protected Model checkActionRefinement(Model model, Naming newNaming, Namer newNamer,
            GUITreeNode[] nodes, GUITree tree, List<GUITree> trees, List<Namer> upperBounds) {
        Set<Name> newNames = new HashSet<>();
        // Condition 1: actions should be refined
        newNaming.dump();
        Map<Name, List<GUITreeNode>> partitions = new HashMap<>();
        for (GUITreeNode n : nodes) {
            Name newName = GUITreeBuilder.getNodeName(newNaming, tree, n);
            newNames.add(newName);
            Utils.addToMapList(partitions, newName, n);
        }

        if (newNames.size() == 1) {
            // Still singleton
            Logger.iformat("New names: %d, try another namer.", newNames.size());
            for (Name n: newNames) {
                Logger.iformat("- %s", n);
            }
            return null;
        }

        // Condition 2: states should not be too refined
        Set<StateKey> newStates = new HashSet<>();
        int threshold = getMaxStatesForRefinementThreshold(newNaming);
        for (GUITree t : trees) {
            StateKey newState = GUITreeBuilder.getStateKey(newNaming, t);
            if (newState.getWidgets().length > maxInitialNamesPerStateThreshold) {
                Logger.iformat("New state has too many names: states (%d) > threshold (%d).",
                        newState.getWidgets().length, maxInitialNamesPerStateThreshold);
                // stop searching finer namers;
                upperBounds.add(newNamer);
                return null;
            }
            newStates.add(newState);
            if (newStates.size() > threshold) {
                Logger.iformat("New states are too fine: states(%d) > threshold (%d), trees(%d).",
                        newStates.size(), threshold, trees.size());
                // stop searching finer namers;
                upperBounds.add(newNamer);
                return null;
            }
        }

        // Condition 3: no violation of refinement constraints.
        Set<GUITree> affected = new HashSet<GUITree>(trees);
        if (!checkPredicate(model.getNamingManager(), affected, newNaming)) {
            Logger.wprintln("New naming violates the refinement constaints");
            return null;
        }

        Logger.iformat("Refine actions: nodes(%d), new names (%d), new states (%d), trees(%d), state threshold (%d)",
                nodes.length, newNames.size(), newStates.size(), trees.size(), threshold);
        for (Name n : newNames) {
            Logger.iformat("- %s", n);
        }

        // stop searching finer namers;
        upperBounds.add(newNamer);
        Predicate p = createAssertActionDivergent(newNaming, model, partitions, tree, trees, threshold);
        this.predicates.add(p);
        // Finally: rebuild the model
        return rebuild(model, trees, newNaming);
    }

    public Model actionRefinement(Model model, ModelAction action) {
        if (!action.requireTarget()) {
            return model;
        }
        if (actionRefinementBlacklist.contains(action)) {
            Logger.iformat("actionRefinement: Reject an action [%s] in blacklist.", action);
            return model;
        }
        GUITreeNode[] nodes = action.getResolvedNodes();
        if (nodes.length <= actionRefinmentThreshold) {
            return model;
        }
        final State state = action.getState();
        if (state.getWidgets().length >= maxInitialNamesPerStateThreshold) {
            Logger.iformat("Already too many names %d.", state.getWidgets().length);
            return model;
        }
        final GUITree tree = state.getLatestGUITree();
        List<GUITree> trees = state.getGUITrees();
        Naming naming = state.getCurrentNaming();
        Name name = action.getTarget();
        GUITreeNode node = action.getResolvedNode();
        if (node == null) {
            throw new IllegalStateException("Action on new/target states must be resolved.");
        }
        Logger.iformat("actionRefinement: Refine name %s that has been resolved to %d nodes", name, nodes.length);
        //
        Namelet namelet = node.getCurrentNamelet();
        Namer namer = namelet.getNamer();
        if (naming.isReplaceable(namelet)) {
            Namelet parent = namelet.getParent();
            Namer parentNamer = parent.getNamer();
            List<Namer> refinedNamers = NamerFactory.getSortedAbove(parentNamer);
            List<Namer> upperBounds = new ArrayList<>();
            outer: for (Namer refined : refinedNamers) {
                if (!upperBounds.isEmpty()) {
                    for (Namer upper : upperBounds) {
                        if (refined.refinesTo(upper)) {
                            continue outer; // no retry 
                        }
                    }
                }
                if (namer.refinesTo(refined)) {
                    continue; // avoid replace with the same namelet.
                }
                Naming newNaming = naming.replaceLast(namelet, new Namelet(namelet.getExprString(), refined));
                Model newModel = checkActionRefinement(model, newNaming, refined, nodes, tree, trees, upperBounds);
                if (newModel != null) {
                    return newModel;
                }
            }
        }
        {
            String nameExpr = NamerFactory.nameToXPathString(name);
            List<Namer> refinedNamers = NamerFactory.getSortedAbove(namer);
            List<Namer> upperBounds = new ArrayList<>();
            outer: for (Namer refined : refinedNamers) {
                if (!upperBounds.isEmpty()) {
                    for (Namer upper : upperBounds) {
                        if (refined.refinesTo(upper)) {
                            continue outer; // no retry 
                        }
                    }
                }
                Naming newNaming = naming.extend(namelet, new Namelet(nameExpr, refined));
                Model newModel = checkActionRefinement(model, newNaming, refined, nodes, tree, trees, upperBounds);
                if (newModel != null) {
                    return newModel;
                }
            }
        }
        Logger.iformat("actionRefinement: Add an action [%s] into blacklist.", action);
        actionRefinementBlacklist.add(action);
        return model;
    }

    private Predicate createAssertActionDivergent(Naming updatedNaming, Model model,
            Map<Name, List<GUITreeNode>> partitions, GUITree tree,
            List<GUITree> trees, int threshold) {
        List<List<GUITreeNode>> nodes = new ArrayList<>();
        for (List<GUITreeNode> p : partitions.values()) {
            nodes.add(Collections.unmodifiableList(p));
        }
        return new AssertActionDivergent2(updatedNaming, tree, nodes);
    }

}
