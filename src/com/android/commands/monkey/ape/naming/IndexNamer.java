package com.android.commands.monkey.ape.naming;

import java.util.EnumSet;

import com.android.commands.monkey.ape.tree.GUITreeNode;

public class IndexNamer extends AbstractNamer implements SingletonNamer {

    class IndexName extends AbstractLocalName {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        private int index;

        public IndexName(int index) {
            this.index = index;
        }

        @Override
        public Namer getNamer() {
            return IndexNamer.this;
        }

        public String toString() {
            return "index=" + index + ";";
        }

        @Override
        public Name getLocalName() {
            return this;
        }

        @Override
        public void appendXPathLocalProperties(StringBuilder sb) {
            sb.append("[@index=");
            sb.append(index);
            sb.append("]");
        }
    }

    public String toString() {
        return "IndexNamer[index]";
    }
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public IndexNamer() {
        super(EnumSet.of(NamerType.INDEX));
    }

    @Override
    public NamerType getNamerType() {
        return NamerType.INDEX;
    }

    @Override
    public Name naming(GUITreeNode node) {
        return NameManager.getCachedName(new IndexName(node.getIndex()));
    }
}
