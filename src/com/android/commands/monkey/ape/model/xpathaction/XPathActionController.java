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
package com.android.commands.monkey.ape.model.xpathaction;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.android.commands.monkey.ape.model.ModelAction;
import com.android.commands.monkey.ape.model.State;
import com.android.commands.monkey.ape.naming.Name;
import com.android.commands.monkey.ape.tree.GUITree;
import com.android.commands.monkey.ape.tree.GUITreeBuilder;
import com.android.commands.monkey.ape.tree.GUITreeNode;
import com.android.commands.monkey.ape.utils.Logger;

/**
 * This is an experimental feature.
 * @author txgu
 *
 */
public class XPathActionController {

    private static final XPathActionController singleton;

    static {
        XPathActionReader reader = new XPathActionReader();
        List<XPathActionSequence> actions;
        File jsonFile = new File("/sdcard/ape.xpath.actions");
        if (jsonFile.exists()) {
            actions = reader.read(jsonFile);
        } else {
            actions = Collections.emptyList();
        }
        singleton = new XPathActionController(actions);
    }

    public static ModelAction selectAction(State state, GUITree tree) {
        return singleton.checkAction(state, tree);
    }

    private LinkedList<XPathAction> actionBuffer = new LinkedList<>();
    private List<XPathActionSequence> actions = new ArrayList<>();

    public XPathActionController(List<XPathActionSequence> actions) {
        this.actions = actions;
    }

    public ModelAction checkAction(State state, GUITree tree) {
        if (actions.isEmpty()) {
            return null;
        }

        if (actionBuffer.isEmpty()) {
            return fillActionBuffer(state, tree);
        }

        XPathAction current = actionBuffer.removeFirst();
        return resolveAction(state, tree, current);
    }

    private ModelAction resolveAction(State state, GUITree tree, XPathAction current) {
        Document document = tree.getDocument();
        if (document == null) {
            Logger.println("Document is null");
            return null;
        }
        try {
            Logger.wprintln(current.getExprStr());
            NodeList nodes = (NodeList) current.getExpr().evaluate(document, XPathConstants.NODESET);
            if (nodes.getLength() == 0) {
                Logger.println("No matching nodes");
                return null;
            }
            Element n0 = (Element) nodes.item(0);
            {
                GUITreeNode treeNode = GUITreeBuilder.getGUITreeNode(n0);
                if (treeNode == null) {
                    Logger.wprintln("Node should not be null!");
                    return null;
                }
                Name xpathName = treeNode.getXPathName();
                treeNode.setInputText(current.getText());
                treeNode.setExtraThrottle(current.getThrottle());
                return state.getAction(xpathName, current.getAction());
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            Logger.println("Fail to evaluate expr" + current.getExprStr());
        }
        return null;

    }

    private ModelAction fillActionBuffer(State state, GUITree tree) {
        Random rand = new Random();
        for (XPathActionSequence actionSeq : actions) {
            if (actionSeq.isEmpty()) {
                continue;
            }
            if (rand.nextDouble() >= actionSeq.getProbability()) {
                continue;
            }
            XPathAction xa = actionSeq.get(0);

            ModelAction a = resolveAction(state, tree, xa);
            if (a == null) {
                continue;
            }
            actionBuffer.clear();
            for (int j = 1; j < actionSeq.size(); j++) {
                actionBuffer.add(actionSeq.get(j));
            }
            return a;
        }

        return null;
    }
}
