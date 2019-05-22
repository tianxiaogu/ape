package com.android.commands.monkey.ape.naming;

import java.io.Serializable;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.NodeList;

import com.android.commands.monkey.ape.utils.XPathBuilder;

public class Namelet implements Serializable {

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
}
