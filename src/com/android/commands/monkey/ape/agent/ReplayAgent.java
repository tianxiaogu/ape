package com.android.commands.monkey.ape.agent;

import java.util.List;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.android.commands.monkey.MonkeySourceApe;
import com.android.commands.monkey.ApeRRFormatter;
import com.android.commands.monkey.ape.StopTestingException;
import com.android.commands.monkey.ape.Subsequence;
import com.android.commands.monkey.ape.model.ModelAction;
import com.android.commands.monkey.ape.model.StartAction;
import com.android.commands.monkey.ape.model.Action;
import com.android.commands.monkey.ape.model.ActionType;
import com.android.commands.monkey.ape.model.FuzzAction;
import com.android.commands.monkey.ape.model.Graph;
import com.android.commands.monkey.ape.model.State;
import com.android.commands.monkey.ape.naming.Name;
import com.android.commands.monkey.ape.tree.GUITree;
import com.android.commands.monkey.ape.tree.GUITreeBuilder;
import com.android.commands.monkey.ape.tree.GUITreeNode;
import com.android.commands.monkey.ape.utils.Logger;
import com.android.commands.monkey.ape.utils.RandomHelper;
import com.android.commands.monkey.ape.utils.XPathBuilder;

import android.content.ComponentName;

/**
 * Trivial replay agent, under development now.
 * @author txgu
 *
 */
public class ReplayAgent extends StatefulAgent {

    List<JSONObject> actions;
    int cursor;
    public ReplayAgent(MonkeySourceApe ape, Graph graph, String logFile) {
        super(ape, graph);
        cursor = 0;
        actions = ApeRRFormatter.readActions(logFile);
    }

    @Override
    public void onActivityBlocked(ComponentName blockedActivity) {
        
    }

    @Override
    public boolean onVoidGUITree(int counter) {
        return false;
    }

    @Override
    public void onBufferLoss(State actual, State expected) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void onRefillBuffer(Subsequence path) {
        throw new RuntimeException("Not implemented");
    }

    protected Name resolveName(NodeList nodeList) {
        int index = RandomHelper.nextInt(nodeList.getLength());
        Element e = (Element) nodeList.item(index);
        GUITreeNode node = GUITreeBuilder.getGUITreeNode(e);
        if (node == null) {
            return null;
        }
        return node.getXPathName();
    }
    
    protected Name resolveName(String target, String full) throws XPathExpressionException {
        int retry = 3;
        while (retry--> 0) {
            GUITree guiTree = newState.getLatestGUITree();
            Document guiXml = guiTree.getDocument();
            XPathExpression targetXPath = XPathBuilder.compileAbortOnError(target);
            XPathExpression fullXPath = XPathBuilder.compileAbortOnError(full);
            NodeList nodesByTarget = (NodeList) targetXPath.evaluate(guiXml, XPathConstants.NODESET);
            NodeList nodesByFull = (NodeList) fullXPath.evaluate(guiXml, XPathConstants.NODESET);
            Name name;
            if (nodesByFull.getLength() != 0) {
                name = resolveName(nodesByFull);
            } else if (nodesByTarget.getLength() != 0) {
                name = resolveName(nodesByTarget);
            } else {
                refreshNewState();
                continue;
            }
            if (name != null) {
                return name;
            }
        }
        Logger.wprintln("Cannot resolve node.");
        return null;
    }
    
    @Override
    protected Action selectNewActionNonnull() {
        if (cursor >= actions.size()) {
            Logger.println("Run out of actions.");
            throw new StopTestingException();
        }
        this.disableFuzzing = true;
        Logger.iformat("Current actions: %d, cursor: %d", actions.size(), cursor);
        JSONObject jAction = actions.get(cursor);
        cursor++;
        Logger.iformat("Current action: %s", jAction);
        try {
            ActionType actionType = ActionType.valueOf(jAction.getString("actionType"));
            if (actionType.isModelAction()) {
                if (actionType.requireTarget()) {
                    String target = jAction.getString("target");
                    String full = jAction.getString("full");
                    Name name = resolveName(target, full);
                    if (name == null) {
                        // return Action.NOP;
                        Logger.wformat("target: %s", target);
                        Logger.wformat("full: %s", full);
                        Logger.printXml(newGUITree.getDocument());
                        throw new StopTestingException("Cannot resolve node.");
                    }
                    ModelAction action = newState.getAction(name, actionType);
                    if (action == null) {
                        Logger.println("Cannot get action on current state.");
                        throw new StopTestingException("Cannot resolve action: " + jAction);
                    }
                    int throttle = jAction.getInt("throttle");
                    action.setThrottle(throttle);
                    return action;
                } else {
                    return newState.getBackAction();
                }
            } else {
                switch (actionType) {
                case FUZZ:
                    return FuzzAction.fromJSON(jAction);
                case EVENT_START:
                case EVENT_RESTART:
                case EVENT_CLEAN_RESTART:
                    return StartAction.fromJSON(jAction);
                case EVENT_NOP:
                    return Action.NOP;
                case EVENT_ACTIVATE:
                    return Action.ACTIVATE;
                default:
                    throw new RuntimeException();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Logger.wformat("Malformed action: %s", jAction);
            throw new StopTestingException("Malformed action: " + jAction, e);
        } catch (XPathExpressionException e) {
            Logger.wprintln("Error during evaluate xpath exception.");
            throw new StopTestingException("Malformed action: " + jAction, e);
        }
    }

    @Override
    public void onBadState(int lastBadStateCount, int badStateCounter) {
        
    }

    @Override
    public String getLoggerName() {
        return "Replay";
    }

}
