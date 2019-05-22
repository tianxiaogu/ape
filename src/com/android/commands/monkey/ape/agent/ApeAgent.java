package com.android.commands.monkey.ape.agent;

import static com.android.commands.monkey.ape.utils.Config.checkRestart;
import static com.android.commands.monkey.ape.utils.Config.inputRate;
import static com.android.commands.monkey.ape.utils.Config.restartThresholdMax;
import static com.android.commands.monkey.ape.utils.Config.restartThresholdMin;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import com.android.commands.monkey.MonkeySourceApe;
import com.android.commands.monkey.MonkeyUtils;
import com.android.commands.monkey.ape.Agent;
import com.android.commands.monkey.ape.BadStateException;
import com.android.commands.monkey.ape.StopTestingException;
import com.android.commands.monkey.ape.events.ApeEvent;
import com.android.commands.monkey.ape.events.ApeFuzzer;
import com.android.commands.monkey.ape.model.Action;
import com.android.commands.monkey.ape.model.ActionType;
import com.android.commands.monkey.ape.model.Crash;
import com.android.commands.monkey.ape.model.CrashAction;
import com.android.commands.monkey.ape.model.FuzzAction;
import com.android.commands.monkey.ape.model.Graph;
import com.android.commands.monkey.ape.model.ModelAction;
import com.android.commands.monkey.ape.tree.GUITreeNode;
import com.android.commands.monkey.ape.utils.Config;
import com.android.commands.monkey.ape.utils.Logger;
import com.android.commands.monkey.ape.utils.RandomHelper;
import com.android.commands.monkey.ape.utils.Utils;

import android.content.ComponentName;
import android.content.Intent;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;

public abstract class ApeAgent implements Agent {

    public static ApeAgent createAgent(MonkeySourceApe ape) {
        String type = Config.get("ape.agentType");
        String modelFile = Config.get("ape.modelFile");
        Graph graph = null;
        if (modelFile == null) {
            graph = new Graph();
        } else {
            Logger.format("Loading graph model from %s", modelFile);
            graph = Graph.readGraph(modelFile);
        }
        if (type == null) {
            return new SataAgent(ape, graph);
        }
        if (type.equals("sata")) {
            return new SataAgent(ape, graph);
        }
        if (type.equals("random")) {
            return new RandomAgent(ape, graph);
        }
        if (type.equals("replay")) {
            String replayLog = Config.get("ape.replayLog");
            if (replayLog == null) {
                Logger.wformat("Replay agent requires a replay log.");
                System.exit(1);
            }
            return new ReplayAgent(ape, graph, replayLog);
        }
        return new SataAgent(ape, graph);
    }


    protected MonkeySourceApe ape;
    protected int timestamp;
    private int lastBadStateCount;
    private int badStateCounter;
    private int totalBadStates;

    private Set<String> activityNames = new HashSet<>();
    private Map<String, Set<String>> activityTransitions = new HashMap<>();

    protected boolean disableFuzzing;
    private boolean restart;
    private int nextRestartThreshold;
    private int lastRestartStep;
    boolean newActivityStarting;
    private boolean disableRestart;
    boolean start;

    public ApeAgent(MonkeySourceApe ape) {
        this.ape = ape;
        updateRestartThreshold();
    }

    public final boolean canFuzzing() {
        return !disableFuzzing;
    }

    @Override
    public boolean activityResuming(String pkg) {
        return false;
    }

    @Override
    public boolean activityStarting(Intent intent, String pkg) {
        boolean allow = MonkeyUtils.getPackageFilter().checkEnteringPackage(pkg);
        if (allow) {
            String newActivity = intent.getComponent().getClassName();
            if (activityNames.add(newActivity)) {
                newActivityStarting = true;
            }
            String currentActivity = ape.getTopActivityClassName();
            Utils.addToMapSet(activityTransitions, currentActivity, newActivity);
        }
        return allow;
    }

    static String processNameToPackageName(String processName) {
        int index = processName.indexOf(":");
        if (index == -1) {
            return processName;
        }
        return processName.substring(0, index);
    }

    public boolean appCrashed(String processName, int pid, String shortMsg, String longMsg, long timeMillis,
            String stackTrace) {
        Crash crash = new Crash(processName, pid, shortMsg, longMsg, timeMillis, stackTrace);
        CrashAction action = new CrashAction(crash);
        Logger.iformat("Appending crash [%s] to action history [%s]", crash, Thread.currentThread());
        appendToActionHistory(timeMillis, action);
        return false;
    }

    @Override
    public int appEarlyNotResponding(String arg0, int arg1, String arg2) {
        return 0;
    }

    @Override
    public int appNotResponding(String arg0, int arg1, String arg2) {
        return 0;
    }

    @Override
    public int systemNotResponding(String arg0) {
        return 0;
    }


    public Action generateFuzzingAction() {
        List<ApeEvent> events = ApeFuzzer.generateFuzzingEvents();
        Action fuzzAction = new FuzzAction(events);
        return fuzzAction;
    }

    protected Action checkInput(Action action) {
        if (action.requireTarget()) {
            GUITreeNode node = ((ModelAction) action).getResolvedNode();
            if (node.isEditText() && node.getInputText() == null) {
                if (RandomHelper.toss(inputRate)) {
                    String text = RandomHelper.nextString();
                    node.setInputText(text);
                }
            }
        }
        return action;
    }

    protected Action checkFuzzing(Action origin) {
        return origin; 
    }

    protected Action getStartAction(ActionType actionType) {
        return Action.getStartAction(actionType, ape.randomlyPickMainApp());
    }

    protected ActionType nextRestartAction() {
        if (RandomHelper.toss(0.2D)) {
            return ActionType.EVENT_CLEAN_RESTART;
        }
        return ActionType.EVENT_RESTART;
    }

    protected Action checkRestart(Action origin) {
        if (!checkRestart) {
            return origin;
        }
        if (restart) {
            restart = false;
            lastRestartStep = getTimestamp();
            this.startNewEpisode();
            return getStartAction(nextRestartAction());
        }
        if (disableRestart) {
            return origin;
        }
        int contSteps = getTimestamp() - lastRestartStep;
        if (contSteps > nextRestartThreshold) {
            lastRestartStep = getTimestamp();
            updateRestartThreshold();
            this.startNewEpisode();
            return getStartAction(nextRestartAction());
        }
        return origin;
    }

    protected void updateRestartThreshold() {
        if (restartThresholdMax <= restartThresholdMin) {
            nextRestartThreshold = restartThresholdMin;
        } else {
            nextRestartThreshold = RandomHelper.nextBetween(restartThresholdMin, restartThresholdMax);
        }
    }

    public void disableRestart() {
        Logger.iprintln("Requesting disabling restart.");
        this.disableRestart = true;
    }

    public void requestStart() {
        this.start = true;
    }

    public void requestRestart() {
        this.restart = true;
    }

    protected Random getRandom() {
        return ape.getRandom();
    }

    protected boolean toss(double probability) {
        double v = ape.getRandom().nextDouble();
        return v < probability;
    }

    public int getTimestamp() {
        return this.timestamp;
    }

    public boolean onLostFocused(int counter) {
        return true;
    }

    public final Action updateState(ComponentName topComp, AccessibilityNodeInfo info) {
        Action action = updateStateWrapper(topComp, info);
        return action;
    }

    private Action updateStateWrapper(ComponentName topComp, AccessibilityNodeInfo info) {
        try {
            Logger.format(">>>>>>>> %s begin step [%d][%d]", getLoggerName(), ++timestamp,
                    SystemClock.elapsedRealtimeNanos());
            printExploredActivity();
            printMemoryUsage();
            try {
                disableRestart = false;
                disableFuzzing = false;
                return checkInput(checkFuzzing(checkRestart(updateStateInternal(topComp, info))));
            } catch (BadStateException e) {
                Logger.wprintln("Bad state, retrieve the Window node.");
                info = ape.getRootInActiveWindowSlow();
                if (info == null) {
                    Logger.wprintln("Fail to retrieve the Window node.");
                    throw e;
                }
                return updateStateInternal(topComp, info);
            }
        } catch (BadStateException e) {
            Logger.wprintln("Handle bad state.");
            totalBadStates++;
            if (lastBadStateCount == (timestamp - 1)) {
                badStateCounter++;
            } else {
                badStateCounter = 0;
            }
            lastBadStateCount = timestamp;
            onBadState(lastBadStateCount, badStateCounter);
            if (badStateCounter > 10) {
                ape.stopTopActivity();
            }
            if (totalBadStates > 100) {
                throw new StopTestingException("Too many bad states.");
            }
            return handleBadState();
        } catch (Exception e) {
            throw e;
        } finally {
            Logger.format(">>>>>>>> %s end step [%d][%d]", getLoggerName(), timestamp,
                    SystemClock.elapsedRealtimeNanos());
        }

    }

    public abstract void onBadState(int lastBadStateCount, int badStateCounter);

    public abstract String getLoggerName();

    protected Action handleBadState() {
        return Action.ACTIVATE;
    }

    protected Action handleTrivialState() {
        return Action.NOP;
    }

    protected abstract Action updateStateInternal(ComponentName topComp, AccessibilityNodeInfo info);

    public int nextInt(int bound) {
        return ape.getRandom().nextInt(bound);
    }

    static Comparator<Entry<String, Object>> comparator = new Comparator<Entry<String, Object>>() {

        @Override
        public int compare(Entry<String, Object> o1, Entry<String, Object> o2) {
            return o1.getKey().compareTo(o2.getKey());
        }

    };

    public void tearDown() {
        printActivities();
        Config.printConfigurations();
    }


    protected File checkOutputDir() {
        File dir = ape.getOutputDirectory();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }



    private void printExploredActivity() {
        if (timestamp % 50 == 0) {
            printActivities();
        } else {
            Logger.iformat("Explored %d app activities.", activityNames.size());
        }
    }

    private void printMemoryUsage() {
        final Runtime runtime = Runtime.getRuntime();
        final long usedMemInMB=(runtime.totalMemory() - runtime.freeMemory()) / 1048576L;
        final long maxHeapSizeInMB=runtime.maxMemory() / 1048576L;
        final long availHeapSizeInMB = maxHeapSizeInMB - usedMemInMB;
        Logger.iformat("Used: %d MB, Max: %d MB, Available: %d MB", usedMemInMB, maxHeapSizeInMB, availHeapSizeInMB);
    }

    private void printActivities() {
        String[] names = this.activityNames.toArray(new String[0]);
        Arrays.sort(names);
        Logger.println("Explored app activities:");
        for (int i = 0; i < names.length; i++) {
            Logger.format("%4d %s", i + 1, names[i]);
        }
    }

}
