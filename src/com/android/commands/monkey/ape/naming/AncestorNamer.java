package com.android.commands.monkey.ape.naming;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

import com.android.commands.monkey.ape.tree.GUITreeNode;

public class AncestorNamer extends AbstractNamer {

    class AncestorName extends AbstractName {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        int hashCode;
        private Name[] names;

        public AncestorName (Name... names) {
            this.names = names;
        }

        @Override
        public Namer getNamer() {
            return AncestorNamer.this;
        }

        @Override
        public int hashCode() {
            if (hashCode == 0) {
                final int prime = 31;
                int result = 1;
                result = prime * result + Arrays.hashCode(names);
                hashCode = result;
            }
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            AncestorName other = (AncestorName) obj;
            if (!Arrays.equals(names, other.names))
                return false;
            return true;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Name name : names) {
                sb.append(NamerFactory.NODE_SEP);
                sb.append(name);
            }
            return sb.toString();
        }

        @Override
        public Name getLocalName() {
            return names[names.length - 1].getLocalName();
        }

        @Override
        public void toXPath(StringBuilder sb) {
            for (Name localName : names) {
                sb.append("/*");
                localName.appendXPathLocalProperties(sb);
            }
        }

        @Override
        public void appendXPathLocalProperties(StringBuilder sb) {
            
        }

    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    Namer namer;

    public AncestorNamer(Namer namer) {
        super(NamerType.add(namer.getNamerTypes(), NamerType.ANCESTOR));
        this.namer = namer;
    }

    @Override
    public Name naming(GUITreeNode node) {
        GUITreeNode parentNode = node.getParent();
        Namer localNamer = NamerFactory.getLocalNamer(namer);
        if (parentNode != null) {
            LinkedList<Name> names = new LinkedList<>();
            boolean useParent = namer.getNamerTypes().contains(NamerType.PARENT);
            names.add(localNamer.naming(node));
            if (useParent) {
                while (parentNode != null) {
                    Namer parentNamer = NamerFactory.getLocalNamer(getNodeNamer(parentNode));
                    names.add(parentNamer.naming(parentNode));
                    parentNode = parentNode.getParent();
                }
            } else {
                while (parentNode != null) {
                    names.add(localNamer.naming(parentNode));
                    parentNode = parentNode.getParent();
                }
            }
            Iterator<Name> it = names.descendingIterator();
            Name [] array = new Name[names.size()];
            int i = 0;
            while (it.hasNext()) {
                array[i++] = it.next();
            }
            return NameManager.getCachedName(new AncestorName(array));
        }
        return NameManager.getCachedName(new AncestorName(localNamer.naming(node)));
    }

    private Namer getNodeNamer(GUITreeNode node) {
        Name tempName = node.getTempXPathName();
        if (tempName == null) {
            tempName = node.getXPathName();
        }
        if (tempName == null) {
            throw new NullPointerException("Temp name of a parent node should be set.");
        }
        return tempName.getNamer();
    }

    public String toString() {
        return "AncestorNamer[" + namer + "]";
    }
}
