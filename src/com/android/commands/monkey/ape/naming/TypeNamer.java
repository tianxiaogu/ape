package com.android.commands.monkey.ape.naming;

import java.util.EnumSet;

import com.android.commands.monkey.ape.tree.GUITreeNode;

public class TypeNamer extends AbstractNamer implements SingletonNamer {

    class TypeName extends AbstractLocalName {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        String klass;
        String resourceId;

        public TypeName(String klass, String resourceId) {
            super();
            this.klass = klass;
            this.resourceId = resourceId;
        }

        @Override
        public Namer getNamer() {
            return TypeNamer.this;
        }

        public String toString() {
            if (resourceId == null || resourceId.length() == 0) {
                return "class=" + klass + ";";
            }
            return "class=" + klass + ";resource-id=" + resourceId + ";";
        }

        @Override
        public void appendXPathLocalProperties(StringBuilder sb) {
            sb.append("[@class=\"");
            sb.append(NamerFactory.escapeToXPathString(klass));
            sb.append("\"][@resource-id=\"");
            sb.append(NamerFactory.escapeToXPathString(resourceId));
            sb.append("\"]");
        }


        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((klass == null) ? 0 : klass.hashCode());
            result = prime * result + ((resourceId == null) ? 0 : resourceId.hashCode());
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
            TypeName other = (TypeName) obj;
            if (klass == null) {
                if (other.klass != null)
                    return false;
            } else if (!klass.equals(other.klass))
                return false;
            if (resourceId == null) {
                if (other.resourceId != null)
                    return false;
            } else if (!resourceId.equals(other.resourceId))
                return false;
            return true;
        }

        @Override
        public Name getLocalName() {
            return this;
        }

    }

    public String toString() {
        return "TypeNamer[type,resource-id]";
    }
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public TypeNamer() {
        super(EnumSet.of(NamerType.TYPE));
    }

    @Override
    public NamerType getNamerType() {
        return NamerType.TYPE;
    }

    @Override
    public Name naming(GUITreeNode node) {
        return NameManager.getCachedName(new TypeName(node.getClassName(), node.getResourceID()));
    }

}
