package com.android.commands.monkey.ape.naming;

import com.android.commands.monkey.ape.tree.GUITreeNode;

public class ParentNamer extends AbstractNamer {

    class ParentName extends AbstractName {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        Name parentName;
        Name localName;

        public ParentName(Name parentName, Name localName) {
            this.parentName = parentName;
            this.localName = localName;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((localName == null) ? 0 : localName.hashCode());
            result = prime * result + ((parentName == null) ? 0 : parentName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ParentName other = (ParentName) obj;
            if (localName == null) {
                if (other.localName != null)
                    return false;
            } else if (!localName.equals(other.localName))
                return false;
            if (parentName == null) {
                if (other.parentName != null)
                    return false;
            } else if (!parentName.equals(other.parentName))
                return false;
            return true;
        }

        @Override
        public Namer getNamer() {
            return ParentNamer.this;
        }

        @Override
        public Name getLocalName() {
            return localName;
        }

        public String toString() {
            return this.parentName + NamerFactory.NODE_SEP + this.localName;
        }

        public void toXPath(StringBuilder sb) {
            parentName.toXPath(sb);
            sb.append("/*");
            localName.appendXPathLocalProperties(sb);
        }

        @Override
        public void appendXPathLocalProperties(StringBuilder sb) {
            
        }
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private Namer namer;

    public ParentNamer(Namer namer) {
        super(NamerType.add(namer.getNamerTypes(), NamerType.PARENT));
        this.namer = namer;
    }

    @Override
    public Name naming(GUITreeNode node) {
        GUITreeNode parentNode = node.getParent();
        Name parentName;
        Name localName = namer.naming(node);
        if (parentNode != null) {
            parentName = parentNode.getTempXPathName();
            if (parentName == null) {
                parentName = parentNode.getXPathName();
            }
            if (parentName == null) {
                throw new IllegalStateException("Parent name should not be null.");
            }
            return NameManager.getCachedName(new ParentName(parentName, localName));
        }
        return NameManager.getCachedName(new ParentName(EmptyNamer.emptyName, localName));
    }

    public String toString() {
        return "ParentNamer[" + namer + "]";
    }

}
