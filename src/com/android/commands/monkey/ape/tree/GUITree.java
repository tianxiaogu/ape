package com.android.commands.monkey.ape.tree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.android.commands.monkey.ape.Agent;
import com.android.commands.monkey.ape.model.ModelAction;
import com.android.commands.monkey.ape.model.State;
import com.android.commands.monkey.ape.naming.Name;
import com.android.commands.monkey.ape.naming.Naming;
import com.android.commands.monkey.ape.utils.Logger;
import com.android.commands.monkey.ape.utils.Utils;

import android.content.ComponentName;

public class GUITree implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 7853671636037436555L;

    private static final boolean debug = false;

    private static List<GUITree> loadedGUITrees = new ArrayList<GUITree>();

    public static void releaseDocuments() {
        for (GUITree tree : loadedGUITrees) {
            tree.releaseDocument();
        }
        loadedGUITrees.clear();
    }

    private static void registerLoadedDocument(GUITree tree) {
        loadedGUITrees.add(tree);
    }

    private int timestamp;

    private final GUITreeNode rootNode;
    private final String activityClassName;
    private final String activityPackageName;

    private Naming currentNaming;
    private State currentState;

    private Name[] currentNames; // names for the nodes at the same index in currentNodes
    private Object[] currentNodes; // An element of this array may be a node or an array of nodes

    private transient Document document;

    public GUITree(GUITreeNode guiTree, ComponentName activityName) {
        this.rootNode = guiTree;
        this.activityPackageName = activityName.getPackageName();
        this.activityClassName = activityName.getClassName();
    }

    public Naming getCurrentNaming() {
        return this.currentNaming;
    }

    public ComponentName getActivityName() {
        return new ComponentName(activityPackageName, activityClassName);
    }

    public String getActivityClassName() {
        return activityClassName;
    }

    public GUITreeNode getRootNode() {
        return rootNode;
    }

    public Name[] getCurrentNames() {
        return currentNames;
    }

    public boolean isIsomorphicTo(GUITree tree) {
        throw new RuntimeException("Not implemented");
    }

    public void printGUITree() {
        for (int i = 0; i < currentNames.length; i++) {
            Logger.format("%5d %s", i + 1, currentNames[i]);
        }
    }

    public boolean hasFocusedNode() {
        boolean focused = false;
        outer: for (Object nodeOrNodes : currentNodes) {
            if (nodeOrNodes instanceof GUITreeNode) {
                GUITreeNode node = (GUITreeNode) nodeOrNodes;
                if (node.isFocused()) {
                    focused = true;
                    break;
                }
            } else {
                GUITreeNode[] node = (GUITreeNode[]) nodeOrNodes;
                for (GUITreeNode n : node) {
                    if (n.isFocused()) {
                        break outer;
                    }
                }
            }
        }
        return focused;
    }

    public GUITreeNode getFirstNode(Name widget) {
        int index = Arrays.binarySearch(currentNames, widget);
        if (index < 0) {
            printGUITree();
            throw new IllegalStateException("Cannot find widget " + widget);
        }
        Object nodeOrNodes = currentNodes[index];
        if (nodeOrNodes instanceof GUITreeNode) {
            return (GUITreeNode) nodeOrNodes;
        }
        GUITreeNode[] nodes = (GUITreeNode[]) nodeOrNodes;
        return nodes[0];
    }

    public List<GUITreeNode> getNodes(Name widget) {
        int index = Arrays.binarySearch(currentNames, widget);
        if (index < 0) {
            printGUITree();
            throw new IllegalStateException("Cannot find widget " + widget);
        }
        Object nodeOrNodes = currentNodes[index];
        if (nodeOrNodes instanceof GUITreeNode) {
            return Collections.singletonList((GUITreeNode) nodeOrNodes);
        }
        GUITreeNode[] nodes = (GUITreeNode[]) nodeOrNodes;
        return Arrays.asList(nodes);
    }

    public GUITreeNode[] pickNodes(ModelAction action) {
        int index = Arrays.binarySearch(currentNames, action.getTarget());
        if (index < 0) {
            printGUITree();
            throw new IllegalStateException("Cannot find widget " + action.getTarget());
        }
        Object nodeOrNodes = currentNodes[index];
        if (nodeOrNodes instanceof GUITreeNode) {
            return new GUITreeNode[] { (GUITreeNode) nodeOrNodes };
        }
        return (GUITreeNode[]) nodeOrNodes;
    }

    public GUITreeNode pickNode(Agent agent, ModelAction action) {
        GUITreeNode[] nodes = pickNodes(action);
        return nodes[agent.nextInt(nodes.length)];
    }

    public int getCountOfTargetNodes(String target) {
        int index = Arrays.binarySearch(currentNames, target);
        if (index < 0) {
            printGUITree();
            throw new IllegalStateException("Cannot find widget " + target);
        }
        Object nodeOrNodes = currentNodes[index];
        if (nodeOrNodes instanceof GUITreeNode) {
            return 1;
        }
        GUITreeNode[] nodes = (GUITreeNode[]) nodeOrNodes;
        return nodes.length;
    }

    public Document getDocument() {
        if (document == null) {
            Logger.iformat("Rebuild document for tree #%d", this.getTimestamp());
            document = GUITreeBuilder.buildDocumentFromGUITree(this);
            setDocument(document);
        }
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
        if (document != null) {
            registerLoadedDocument(this);
            document.setUserData(GUITreeBuilder.GUI_TREE_PROP_NAME, this, null);
        }
    }

    public void validate() {
        for (int i = 0; i < currentNames.length; i++) {
            Name w = currentNames[i];
            Object nodeOrNodes = currentNodes[i];
            if (nodeOrNodes instanceof GUITreeNode) {
                if (!w.equals(((GUITreeNode) nodeOrNodes).getXPathName())) {
                    throw new IllegalStateException("Mismatched node and name " + w);
                }
            } else {
                GUITreeNode[] ns = (GUITreeNode[]) nodeOrNodes;
                for (GUITreeNode n : ns) {
                    if (!w.equals(n.getXPathName())) {
                        throw new IllegalStateException("Mismatched node and name " + w);
                    }
                }
            }
        }
    }

    void rebuild(Name[] widgets, Object[] nodes) {
        this.currentNames = widgets;
        this.currentNodes = nodes;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getTimestamp() {
        return this.timestamp;
    }

    public void setCurrentNaming(Naming current, Name[] currentWidgets, Object[] currentNodes) {
        this.currentNaming = current;
        this.currentNames = currentWidgets;
        this.currentNodes = currentNodes;
    }

    public void setCurrentState(State state) {
        if (state == null) {
            this.currentState = null;
            return;
        }
        this.currentState = state;
        Name[] oldWidgets = this.currentNames;
        this.currentNames = state.getWidgets();
        if (debug) {
            if (oldWidgets != currentNames) {
                if (currentNames.length != oldWidgets.length) {
                    Utils.dump(currentNames);
                    Utils.dump(oldWidgets);
                    throw new RuntimeException("mismatch GUI tree and state");
                }
                for (int i = 0; i < currentNames.length; i++) {
                    if (!currentNames[i].equals(oldWidgets[i])) {
                        Utils.dump(currentNames);
                        Utils.dump(oldWidgets);
                        throw new RuntimeException("mismatch GUI tree and state");
                    }
                }
            }
        }
    }

    public State getCurrentState() {
        return this.currentState;
    }

    public boolean contains(GUITreeNode node) {
        Name widget = node.getXPathName();
        int index = Arrays.binarySearch(currentNames, widget);
        if (index == -1) {
            return false;
        }
        Object nodeOrNodes = currentNodes[index];
        if (nodeOrNodes instanceof GUITreeNode) {
            return nodeOrNodes == node;
        }
        GUITreeNode[] nodes = (GUITreeNode[]) nodeOrNodes;
        for (GUITreeNode n : nodes) {
            if (n == node) {
                return true;
            }
        }
        return false;
    }

    /**
     * For debugging only, need to traverse the whole GUI tree.
     * @param node
     * @return
     */
    public boolean containsHeavy(GUITreeNode node) {
        return search(rootNode, node);
    }

    private boolean search(final GUITreeNode root, final GUITreeNode node) {
        if (root == node) {
            return true;
        }
        if (root.getChildCount() == 0) {
            return false;
        }
        Iterator<GUITreeNode> it = root.getChildren();
        while (it.hasNext()) {
            if (search(it.next(), node)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Release the XML document to save memory since we can recreate it when necessary.
     */
    public void releaseDocument() {
        Logger.iprintln("Release document for tree #" + getTimestamp());
        releaseDocumentNode(this.rootNode);
        this.document = null;
    }

    private static void releaseDocumentNode(GUITreeNode node) {
        Element e = node.getDomNode();
        e.setUserData(GUITreeBuilder.GUI_TREE_NODE_PROP_NAME, null, null);
        node.setDomNode(null);
        Iterator<GUITreeNode> iterator = node.getChildren();
        while (iterator.hasNext()) {
            releaseDocumentNode(iterator.next());
        }
    }

    public String toString() {
        return "GUITree[" + this.timestamp + "]@" + currentState;
    }

    public Object[] getCurrentNodes() {
        return currentNodes;
    }
}
