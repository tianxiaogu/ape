package com.android.commands.monkey.ape.naming;

import static com.android.commands.monkey.ape.utils.Config.ignoreEmpty;
import static com.android.commands.monkey.ape.utils.Config.ignoreOutOfBounds;

import java.io.Serializable;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.android.commands.monkey.ape.tree.GUITree;
import com.android.commands.monkey.ape.tree.GUITreeBuilder;
import com.android.commands.monkey.ape.tree.GUITreeNode;
import com.android.commands.monkey.ape.utils.Logger;
import com.android.commands.monkey.ape.utils.Utils;

import android.os.SystemClock;

public class Naming implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 7543098620002637872L;


    public static class NamingResult {
        private int nodeSize;
        private Name[] names;
        private Object[] nodes;
        private Object[] namelets;

        public NamingResult(Map<Name, Map<GUITreeNode, Namelet>> nameToNodes) {
            names = new Name[nameToNodes.size()];
            nodes = new Object[names.length];
            namelets = new Object[names.length];
            names = nameToNodes.keySet().toArray(names);
            Arrays.sort(names);
            for (int i = 0; i < names.length; i++) {
                Name name = names[i];
                Map<GUITreeNode, Namelet> map = nameToNodes.get(name);
                if (map.size() == 1) {
                    nodeSize++;
                    nodes[i] = map.keySet().iterator().next();
                    namelets[i] = map.values().iterator().next();
                } else {
                    GUITreeNode[] ns = new GUITreeNode[map.size()];
                    Namelet[] ls = new Namelet[map.size()];
                    int index = 0;
                    for (Entry<GUITreeNode, Namelet> entry : map.entrySet()) {
                        ns[index] = entry.getKey();
                        ls[index] = entry.getValue();
                        index++;
                    }
                    nodes[i] = ns;
                    namelets[i] = ls;
                    nodeSize += ns.length;
                }
            }
        }

        public Name[] getNames() {
            return this.names;
        }

        public Object[] getNodes() {
            return this.nodes;
        }

        public int getNameSize() {
            return this.names.length;
        }

        public int getNodeSize() {
            return nodeSize;
        }

        public Name getName(GUITreeNode target) {
            for (int i = 0; i < names.length; i++) {
                Name name = names[i];
                Object nodeOrNodes = nodes[i];
                if (nodeOrNodes instanceof GUITreeNode) {
                    GUITreeNode node = (GUITreeNode) nodeOrNodes;
                    if (target == node) {
                        return name;
                    }
                } else {
                    GUITreeNode[] nodes = (GUITreeNode[]) nodeOrNodes;
                    for (int j = 0; j < nodes.length; j++) {
                        GUITreeNode node = nodes[j];
                        if (target == node) {
                            return name;
                        }
                    }
                }
            }
            throw new IllegalArgumentException("GUITreeNode does not exist.");
        }

        public void updateNames() {
            for (int i = 0; i < names.length; i++) {
                Name name = names[i];
                Object nodeOrNodes = nodes[i];
                if (nodeOrNodes instanceof GUITreeNode) {
                    GUITreeNode node = (GUITreeNode) nodeOrNodes;
                    node.setXPathName(name);
                    node.setCurrentNamelet((Namelet) namelets[i]);
                } else {
                    GUITreeNode[] nodes = (GUITreeNode[]) nodeOrNodes;
                    Namelet[] namelets = (Namelet[]) this.namelets[i];
                    for (int j = 0; j < nodes.length; j++) {
                        GUITreeNode node = nodes[j];
                        node.setXPathName(name);
                        node.setCurrentNamelet(namelets[j]);
                    }
                }
            }
        }

        public void dump() {
            Utils.dump(names);
        }
    }

    static boolean addNamedNode(Map<Name, Object> nameToNodes, Name name, GUITreeNode node) {
        if (ignoreEmpty) {
            if (node.isEmpty()) {
                return true;
            }
        }

        if (ignoreOutOfBounds) {
            if (node.isOutOfRoot()) {
                return true;
            }
        }

        Object existing = nameToNodes.get(name);
        if (existing == null) {
            nameToNodes.put(name, node);
        }
        if (existing instanceof GUITreeNode) {
            nameToNodes.put(name, new GUITreeNode[] { (GUITreeNode) existing, node });
        } else if (existing instanceof GUITreeNode[]) {
            GUITreeNode[] oldNodes = (GUITreeNode[]) existing;
            GUITreeNode[] newNodes = new GUITreeNode[oldNodes.length + 1];
            System.arraycopy(oldNodes, 0, newNodes, 0, oldNodes.length);
            newNodes[oldNodes.length] = node;
            nameToNodes.put(name, newNodes);
        }
        return true;
    }

    static Comparator<Namelet> comparator = new Comparator<Namelet>() {

        @Override
        public int compare(Namelet o1, Namelet o2) {
            int ret = o1.getDepth() - o2.getDepth();
            if (ret != 0) {
                return ret;
            }
            return o1.getExprString().compareTo(o2.getExprString());
        }

    };

    static AtomicInteger counter = new AtomicInteger();

    private String namingName; // for logging
    private Naming parent; // for sharing namelets
    private Namelet[] namelets;
    // Use //* namelet Namer defaultNamer;
    private int fineness;
    private Map<Namelet, Naming> children;

    public Naming(Namelet[] namelets) { // base
        this(null, namelets);
    }

    private Naming(Naming parent, Namelet[] namelets) {
        this.namingName = "Naming[" + counter.getAndIncrement() + "]";
        this.parent = parent;
        this.namelets = namelets;
        this.fineness = -1;
        for (int i = 0; i < namelets.length; i++) {
            Namelet namelet = namelets[i];
            int f = namelet.getNamer().getNamerTypes().size();
            if (fineness == -1) {
                fineness = f;
            } else if (fineness < f) {
                this.fineness = f;
            }
        }
    }

    public Naming getParent() {
        return parent;
    }

    public int size() {
        return this.namelets.length;
    }

    /*private*/ Namelet join(Namelet a1, Namelet a2) {
        if (!a1.getExprString().equals(a2.getExprString())) {
            throw new IllegalArgumentException();
        }
        if (!a1.getType().equals(a2.getType())) {
            throw new IllegalArgumentException();
        }

        return new Namelet(a1.getType(), a1.getExprString(), NamerFactory.join(a1.getNamer(), a2.getNamer()));
    }

    public boolean contains(Namelet namelet) {
        for (Namelet n : namelets) {
            if (n == namelet) {
                return true;
            }
        }
        return false;
    }

    private void ensureContain(Namelet namelet) {
        if (!contains(namelet)) {
            throw new IllegalStateException("namelet is not in");
        }
    }

    private void ensureReplaceable(Namelet namelet) {
        if (!isReplaceable(namelet)) {
            throw new IllegalStateException("namelet is not in");
        }
    }

    /**
     * Replace an existing namelet or add the new namelet.
     * 
     * @param namelet
     * @return
     */
    public Naming extend(Namelet parent, Namelet namelet) {
        ensureContain(parent);
        ensureRefine(parent, namelet);
        Naming child = getChild(namelet);
        if (child == null) {
            child = doExtend(namelet);
            namelet.setParent(parent);
            addChild(namelet, child);
        }
        return child;
    }

    private void ensureRefine(Namelet parent, Namelet namelet) {
        if (!namelet.getNamer().refinesTo(parent.getNamer())) {
            throw new IllegalArgumentException("Extended child namer should refine to parent namer.");
        }
    }

    /**
     * @return
     */
    public Namelet getLastNamelet() {
        return namelets[namelets.length -1];
    }

    public boolean isReplaceable(Namelet namelet) {
        if (!namelet.isRefine()) {
            return false;
        }
        return namelet == getLastNamelet();
    }

    /**
     * 
     * @param replaced
     * @param namelet
     * @return
     */
    public Naming replaceLast(Namelet replaced, Namelet namelet) {
        ensureReplaceable(replaced);
        Naming parentNaming = getParent();
        return parentNaming.extend(replaced.getParent(), namelet);
    }

    private void addChild(Namelet namelet, Naming child) {
        if (children == null) {
            children = new HashMap<>();
        }
        children.put(namelet, child);
    }

    public Collection<Naming> getChildren() {
        if (children == null) {
            return Collections.emptyList();
        }
        return this.children.values();
    }

    private Naming getChild(Namelet namelet) {
        if (children == null) {
            return null;
        }
        return children.get(namelet);
    }

    private Naming doExtend(Namelet namelet) {
        int length = this.namelets.length;
        Namelet[] newNamelets = new Namelet[length + 1];
        System.arraycopy(this.namelets, 0, newNamelets, 0, length);
        newNamelets[length] = namelet;
        return new Naming(this, newNamelets);
    }

    private void clearNames(Document xmlTree) {
        LinkedList<GUITreeNode> queue = new LinkedList<>();
        GUITreeNode root = GUITreeBuilder.getGUITreeNode(xmlTree.getDocumentElement());
        queue.add(root);
        while (!queue.isEmpty()) {
            GUITreeNode current = queue.removeFirst();
            current.setTempXPathName(null);
            Iterator<GUITreeNode> children = current.getChildren();
            while (children.hasNext()) {
                queue.addLast(children.next());
            }
        }
    }

    protected void saveXmlOnError(Document tree) {
        Logger.wprintln("Saving xml to /sdcard/badtree.xml");
        try {
            Utils.saveXml("/sdcard/badtree.xml", tree);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected void saveXmlOnError(Document tree, Element element) {
        saveXmlOnError(tree);
        Utils.dumpElement(element);
        Logger.wprintln("----------------------------------------");
        Utils.dumpTree(tree);
        Logger.wprintln("----------------------------------------");
        GUITree guiTree = GUITreeBuilder.getGUITree(tree);
        GUITreeNode guiNode = GUITreeBuilder.getGUITreeNode(element);
        Logger.iprintln("Checking GUI tree #" + guiTree);
        if (guiTree != null && !guiTree.containsHeavy(guiNode)) {
            Logger.iprintln("GUITreeNode does not belong to the tree.");
        }
    }

    public Name[] getNames(GUITree tree) {
        return naming(tree, false).getNames();
    }

    public Name getName(GUITree tree, GUITreeNode node) {
        return naming(tree, false).getName(node);
    }

    private Namelet select(List<Namelet> namelets) {
        Namelet namelet = null;
        if (namelets == null || namelets.size() == 0) {
            throw new IllegalArgumentException("Empty list");
        }
        if (namelets.size() == 1) {
            namelet = namelets.get(0);
            if (!namelet.getType().equals(Namelet.Type.BASE)) {
                throw new IllegalArgumentException("Missing base namelet.");
            }
            return namelet;
        }

        Collections.sort(namelets, comparator);
        for (int i = namelets.size() - 1; i >= 0; i--) {
            Namelet n = namelets.get(i).getParent();
            while (n != null) {
                if (Collections.binarySearch(namelets, n, comparator) == -1) {
                    break;
                }
                n = n.getParent();
            }
            if (n == null) { // all are included
                return namelets.get(i);
            }
        }
        for (Namelet n : namelets) {
            Logger.iprintln(n);
        }
        return null;
    }

    public Map<Element, List<Namelet>> select(Document tree) {
        Map<Element, List<Namelet>> elementToNamelets = new HashMap<>();
        for (Namelet namelet : namelets) {
            NodeList nodes = namelet.filter(tree);
            int length = nodes.getLength();
            for (int i = 0; i < length; i++) {
                Element item = (Element) nodes.item(i);
                Utils.addToMapList(elementToNamelets, item, namelet);
            }
        }
        return elementToNamelets;
    }

    private transient Map<GUITree, NamingResult> treeToNamingResult = new HashMap<>();

    public NamingResult naming(GUITree tree, boolean updateNodeName) {
        NamingResult results;

        results = treeToNamingResult.get(tree);
        if (results != null) {
            if (updateNodeName) {
                results.updateNames();
            }
            return results;
        }
        long begin = SystemClock.elapsedRealtimeNanos();
        try {
            Document document = tree.getDocument();
            results = namingInternal(document, updateNodeName);
            treeToNamingResult.put(tree, results);
            return results;
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        } finally {
            long end = SystemClock.elapsedRealtimeNanos();
            Logger.dformat("Create %d names for %d nodes in %d ms for tree %s by %s [Update node name: %s].", results.getNameSize(),
                    results.getNameSize(), TimeUnit.NANOSECONDS.toMillis(end - begin), tree, this, updateNodeName);
        }
    }

    protected NamingResult namingInternal(Document tree, boolean updateNodeName) {
        Map<Name, Map<GUITreeNode, Namelet>> nameToNodes = new HashMap<>();
        Map<Element, List<Namelet>> elementToNamelets = select(tree);
        LinkedList<Element> queue = new LinkedList<>();
        Element root = tree.getDocumentElement();
        queue.add(root);
        while (!queue.isEmpty()) {
            Element current = queue.removeFirst();
            List<Namelet> namelets = elementToNamelets.get(current);
            if (namelets == null || namelets.isEmpty()) {
                saveXmlOnError(tree, current);
                throw new IllegalStateException("A node has no namelets.");
            }
            Namelet namelet = select(namelets);
            if (namelet == null) {
                saveXmlOnError(tree, current);
                throw new IllegalStateException("A node has no namelet.");
            }
            Namer namer = namelet.getNamer();
            if (namer == null) {
                throw new IllegalStateException("A node has no namer.");
            }
            GUITreeNode treeNode = GUITreeBuilder.getGUITreeNode(current);
            if (treeNode == null) {
                throw new IllegalStateException("GUITree is not bound.");
            }
            Name name = namer.naming(treeNode);
            {
                Utils.addToMapMap(nameToNodes, name, treeNode, namelet);
                treeNode.setTempXPathName(name);
                if (updateNodeName) {
                    treeNode.setXPathName(name);
                    treeNode.setCurrentNamelet(namelet);
                }
            }
            NodeList children = current.getChildNodes();
            int length = children.getLength();
            for (int i = 0; i < length; i++) {
                Node n = children.item(i);
                if (n instanceof Element) {
                    queue.addLast((Element) n);
                }
            }
        }
        clearNames((Document) tree);
        return new NamingResult(nameToNodes);
    }

    public String toString() {
        return namingName;
    }

    public boolean equivalent(Naming that) {
        Set<Namelet> thisSet = new HashSet<>(Arrays.asList(this.namelets));
        Set<Namelet> thatSet = new HashSet<>(Arrays.asList(that.namelets));
        return thisSet.equals(thatSet);
    }

    public void dump() {
        Logger.format("this:%s, parent:%s.", this, parent);
        for (int i = 0; i < namelets.length; i++) {
            Namelet namelet = namelets[i];
            Logger.format("%3d. [%03d][%s] %s -> %s", i, namelet.getDepth(), namelet.getType(),
                    namelet.getExprString(), namelet.getNamer());
        }
    }

    public boolean isAncestor(Naming descendant) {
        descendant = descendant.getParent();
        while (descendant != null) {
            if (this == descendant) {
                return true;
            }
            descendant = descendant.getParent();
        }
        return false;
    }

    public int getFineness() {
        return fineness;
    }

    public void release(GUITree removed) {
        this.treeToNamingResult.remove(removed);
    }

}
