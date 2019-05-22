package ape;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.android.commands.monkey.ApeRRFormatter;
import com.android.commands.monkey.ape.Subsequence;
import com.android.commands.monkey.ape.model.Action;
import com.android.commands.monkey.ape.model.CrashAction;
import com.android.commands.monkey.ape.model.Model;
import com.android.commands.monkey.ape.model.Model.ActionRecord;
import com.android.commands.monkey.ape.model.ModelAction;
import com.android.commands.monkey.ape.model.State;
import com.android.commands.monkey.ape.model.StateTransition;
import com.android.commands.monkey.ape.utils.Logger;
import com.android.commands.monkey.ape.utils.Utils;

/**
 * A trivial action sequence reducer, which works really bad.
 * This tiny reducer should be compatible with J2SE and can be invoked in your development environment.
 * @author txgu
 *
 */
public class Reducer {

    static void reduce(Model model, List<ActionRecord> crashLog) {
        if (crashLog.size() <= 2) {
            Logger.wprintln("Trivial crash log: fewer than two states.");
            return;
        }
        Action firstAction = crashLog.get(0).modelAction;
        if (!firstAction.canStartApp()) {
            Logger.wformat("The first action is expected to be START, but we get %s.", firstAction);
            return;
        }
        Action lastAction = crashLog.get(crashLog.size() - 1).modelAction;
        if (!lastAction.isCrash()) {
            Logger.wformat("The first action is expected to be CRASH, but we get %s.", lastAction);
            return;
        }
        Set<State> states = new HashSet<State>();
        Set<Action> actions = new HashSet<Action>();
        Map<State, List<ActionRecord>> stateToRecords = new HashMap<>();
        Map<Action, List<ActionRecord>> actionToRecords = new HashMap<>();
        State firstState = null;
        State lastState = null;
        Action lastStart = null;
        Action lastNonCrash = null;
        for (ActionRecord ar : crashLog) {
            actions.add(ar.modelAction);
            if (ar.modelAction.isModelAction()) {
                ar.resolveModelAction();
                ModelAction ma = (ModelAction) ar.modelAction;
                lastState = ma.getState();
                states.add(lastState);
                Utils.addToMapList(stateToRecords, lastState, ar);
                Utils.addToMapList(actionToRecords, ar.modelAction, ar);
                if (firstState == null) {
                    firstState = lastState;
                }
            }
            if (ar.modelAction.canStartApp()) {
                lastStart = ar.modelAction;
                firstState = null;
            }
        }
        if (firstState == null || lastState == null) {
            throw new IllegalArgumentException("Invalid ");
        }
        if (firstState == lastState) {
            Logger.iformat("It seems to be a trivial crash on startup.", lastState, firstState);
            return;
        }
        for (int i = crashLog.size() - 2; i >= 0; i--) {
            ActionRecord ar = crashLog.get(i);
            if (!ar.modelAction.isCrash()) {
                lastNonCrash = ar.modelAction;
                if (lastNonCrash.isModelAction()) {
                    ar.resolveModelAction();
                }
                break;
            }
        }
        if (lastNonCrash == null) {
            Logger.iprintln("No last non-crash action.");
            return;
        }
        List<Subsequence> subsequences = model.getGraph().moveToState(firstState, lastState, false);
        if (subsequences.isEmpty()) {
            // when trace is unconnected due to fuzzing, clean restart. etc.
            Logger.iformat("%s is not reachable from %s.", lastState, firstState);
        } else {
            Subsequence sub = subsequences.get(0);
            for (StateTransition edge : sub.getEdges()) {
                Logger.println("Source: " + edge.getSource());
                Logger.println("Action: " + edge.getAction());
                Logger.println("Target: " + edge.getTarget());
            }
            PrintWriter pw = new PrintWriter(System.out);
            ApeRRFormatter.startLogAction(pw, lastStart, 0, 0); // clock time and timestamp is not necessary during replaying.
            for (StateTransition edge : sub.getEdges()) {
                ApeRRFormatter.startLogAction(pw, edge.action, 0, 0);
            }
            ApeRRFormatter.startLogAction(pw, lastNonCrash, 0, 0);
        }
    }

    public static void main(String[] args) {
        String outputDir = args[0];
        File modelFile = new File(outputDir, "sataModel.obj");
        if (!modelFile.isFile()) {
            throw new IllegalArgumentException(String.format("File %s does not exist.", modelFile));
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(modelFile))) {
            Model model = (Model) ois.readObject();
            List<ActionRecord> actionRecords = model.getActionHistory();
            int begin = 0;
            for (int i = 0; i < actionRecords.size(); i++) {
                ActionRecord record = actionRecords.get(i);
                Action action = record.modelAction;
                if (action.isCrash()) {
                    CrashAction crashAction = (CrashAction) action;
                    crashAction.crash.print();
                    List<ActionRecord> crashLog = actionRecords.subList(begin, i + 1);
                    int index = 0;
                    for (ActionRecord ar : crashLog) {
                        String stateId = "";
                        if (ar.modelAction.isModelAction()) {
                            ar.resolveModelAction();
                            ModelAction ma = (ModelAction) ar.modelAction;
                            stateId = ma.getState().getGraphId() + " ";
                        }
                        Logger.iformat("%4d %s%s", index++, stateId, ar.modelAction);
                    }
                    reduce(model, crashLog);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
