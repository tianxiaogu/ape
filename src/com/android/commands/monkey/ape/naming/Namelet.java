package com.android.commands.monkey.ape.naming;

import java.io.Serializable;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.NodeList;

import com.android.commands.monkey.ape.utils.XPathBuilder;

public class Namelet implements Serializable, Comparable<Namelet> {

    enum Type {
        BASE, REFINE,
    }

    /**
     * 
     */
    private static final long serialVersionUID = -8141222611719008550L;
    private final Type type;
    private final String exprStr;
    private final Namer namer;

    private int depth;
    private Namelet parent;
    private Map<Namelet, Naming> children;

    public Namelet(Type type, String exprStr, Namer namer) {
        this.type = type;
        this.exprStr = exprStr;
        this.namer = namer;
    }

    public Namelet(String exprStr, Namer namer) {
        this(Type.REFINE, exprStr, namer);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((exprStr == null) ? 0 : exprStr.hashCode());
        result = prime * result + ((namer == null) ? 0 : namer.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    public String toString() {
        return String.format("[%s][%d][%s][%s][%s]", type, depth, exprStr, namer, parent);
    }

    public void addChildNaming(Namelet child, Naming childNaming) {
        if (children == null) {
            children = new HashMap<>();
        }
        children.put(child, childNaming);
    }

    public Naming getChildNaming(Namelet child) {
        if (children == null) {
            return null;
        }
        return children.get(child);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Namelet other = (Namelet) obj;
        if (exprStr == null) {
            if (other.exprStr != null)
                return false;
        } else if (!exprStr.equals(other.exprStr))
            return false;
        if (namer == null) {
            if (other.namer != null)
                return false;
        } else if (!namer.equals(other.namer))
            return false;
        if (type != other.type)
            return false;
        return true;
    }

    public void setParent(Namelet parent) {
        if (parent == null) {
            throw new IllegalArgumentException("Parent is null!");
        }
        if (this.parent != null) {
            throw new IllegalStateException("Namelet already in list.");
        }
        this.parent = parent;
        this.depth = parent.depth + 1;
    }

    public boolean isRoot() {
        return this.parent == null;
    }

    public Namelet getParent() {
        return this.parent;
    }

    public int getDepth() {
        return depth;
    }

    public Type getType() {
        return type;
    }

    public String getExprString() {
        return exprStr;
    }

    public boolean isBase() {
        return this.type.equals(Namelet.Type.BASE);
    }

    public boolean isRefine() {
        return this.type.equals(Namelet.Type.REFINE);
    }

    public XPathExpression getExpression() {
        return XPathBuilder.compileAbortOnError(exprStr);
    }

    public NodeList filter(Object tree) {
        try {
            return (NodeList) getExpression().evaluate(tree, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
        }
        return null;
    }

    public Namer getNamer() {
        return namer;
    }

    static Comparator<Namer> namerComparator = new Comparator<Namer>() {

        @Override
        public int compare(Namer o1, Namer o2) {
            EnumSet<NamerType> types1 = o1.getNamerTypes();
            EnumSet<NamerType> types2 = o2.getNamerTypes();
            int ret = types1.size() - types2.size();
            if (ret != 0) {
                return ret;
            }
            if (types1.isEmpty()) {
                return 0; // both are empty
            }
            NamerType[] typesArray1 = types1.toArray(new NamerType[0]);
            NamerType[] typesArray2 = types2.toArray(new NamerType[0]);
            for (int i = 0; i < typesArray1.length; i++) {
                ret = typesArray1[i].compareTo(typesArray2[i]);
                if (ret != 0) {
                    return ret;
                }
            }
            return 0;
        }
    };

    @Override
    public int compareTo(Namelet o) {
        int ret = this.exprStr.compareTo(o.exprStr);
        if (ret != 0) {
            return ret;
        }
        ret = this.type.compareTo(o.type);
        if (ret != 0) {
            return ret;
        }
        return namerComparator.compare(this.namer, o.namer);
    }

}
