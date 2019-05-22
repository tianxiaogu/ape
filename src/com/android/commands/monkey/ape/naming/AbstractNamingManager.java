package com.android.commands.monkey.ape.naming;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.android.commands.monkey.ape.model.Model;
import com.android.commands.monkey.ape.model.ModelAction;
import com.android.commands.monkey.ape.model.State;
import com.android.commands.monkey.ape.model.StateKey;
import com.android.commands.monkey.ape.model.StateTransition;
import com.android.commands.monkey.ape.tree.GUITree;
import com.android.commands.monkey.ape.utils.Logger;

import android.os.SystemClock;

public abstract class AbstractNamingManager implements NamingManager, Cloneable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    protected final NamingFactory namingFactory;
    protected Map<GUITree, Naming> treeToNaming = new HashMap<>();
    protected int version;
    
    private static boolean debug = false;

    public AbstractNamingManager(NamingFactory nf) {
        this.namingFactory = nf;
    }

    public Naming getBaseNaming() {
        return namingFactory.getBaseNaming();
    }

    public final Model resolveNonDeterminism(Model model, StateTransition st) {
        return namingFactory.resolveNonDeterminism(model, st);
    }

    public final Model actionRefinement(Model model, ModelAction action) {
        return namingFactory.actionRefinement(model, action);
    }

    public void release(GUITree removed) {
        this.treeToNaming.remove(removed);
    }

    public final Model stateAbstraction(Model model, Naming naming, State target, Naming parentNaming, Set<State> states) {
        return namingFactory.batchAbstract(model, naming, target, parentNaming, states);
    }

    protected boolean checkReplace(Naming existing, Naming oldOne, Naming newOne) {
        if (oldOne == null || existing == null || newOne == null) {
            throw new IllegalArgumentException("Should not be null!");
        }
        if (oldOne == newOne) {
            throw new IllegalArgumentException("Should be different!");
        }
        if (existing == (newOne)) {
            return false; // has been updated
        }
        if (existing != oldOne) {
            throw new IllegalArgumentException("Should be the same!");
        }
        return true; // do replace
    }

    public Naming getTopNaming() {
        return namingFactory.getTopNaming();
    }

    public Naming getBottomNaming() {
        return namingFactory.getBottomNaming();
    }

    public AbstractNamingManager clone() {
        try {
            AbstractNamingManager that = (AbstractNamingManager) super.clone();
            that.treeToNaming = new HashMap<>(this.treeToNaming);
            return that;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Should not reach here.");
    }

    @Override
    public StateKey getStateKey(Naming naming, GUITree tree) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final Naming getNaming(GUITree tree) {
        Naming naming = treeToNaming.get(tree);
        if (naming == null) {
            naming = this.getNaming(tree, tree.getActivityName(), tree.getDocument());
            if (naming == null) {
                throw new NullPointerException("Cannot get naming for raw GUI tree.");
            }
            this.treeToNaming.put(tree, naming);
        }
        return naming;
    }

    @Override
    public final void updateNaming(GUITree tree, Naming newOne) {
        this.version++;
        Naming existing = this.treeToNaming.put(tree, newOne);
        if (debug) {
            if (existing != null && existing != tree.getCurrentNaming()) {
                Logger.wformat("Existing: %s, current: %s, new: %s", existing, tree.getCurrentNaming(), newOne);
                throw new IllegalStateException("Inconsistent naming update.");
            }
        }
        updateNaming(tree, tree.getActivityName(), tree.getDocument(), tree.getCurrentNaming(), newOne);
        if (debug) {
            if (newOne != this.getNaming(tree, tree.getActivityName(), tree.getDocument())) {
                throw new IllegalStateException("Inconsistent naming update.");
            }
        }
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public Map<GUITree, Naming> sync(Iterator<GUITree> trees) {
        long begin = SystemClock.elapsedRealtimeNanos();
        Logger.println("Start syncing naming manager...");
        Map<GUITree, Naming> updated = new HashMap<>();
        Set<GUITree> treeSet = new HashSet<GUITree>();
        int count = 0;
        while (trees.hasNext()) {
            GUITree tree = trees.next();
            if (treeSet.add(tree) == false) {
                throw new RuntimeException("Sanity check failed! Duplicated tree in tree iterator.");
            }
            count++;
            Naming cached = this.treeToNaming.get(tree);
            Naming current = tree.getCurrentNaming();
            Naming check = getNaming(tree, tree.getActivityName(), tree.getDocument());
            if (cached == null) {
                this.treeToNaming.put(tree, check);
            } else if (cached != check) {
                if (cached != current) {
                    Logger.println(" Cached: " + cached);
                    Logger.println("Current: " + current);
                    Logger.println("  Check: " + check);
                    throw new IllegalStateException("Inconsistent naming.");
                }
                updated.put(tree, check);
            }
        }
        long end = SystemClock.elapsedRealtimeNanos();
        Logger.format("Sync naming functions: checked %d trees in %d ms, %d updated.", count,
                TimeUnit.NANOSECONDS.toMillis(end - begin), updated.size());
        return updated;
    }

}
