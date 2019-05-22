package com.android.commands.monkey.ape.naming;

import java.util.Arrays;
import java.util.EnumSet;

import com.android.commands.monkey.ape.tree.GUITreeNode;

public class CompoundNamer extends AbstractNamer {

    class CompoundName extends AbstractLocalName {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        int hashCode = 0;
        Name[] names;

        public CompoundName(Name... names) {
            this.names = names;
        }

        @Override
        public Namer getNamer() {
            return CompoundNamer.this;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Name name : names) {
                sb.append(name);
            }
            return sb.toString();
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
            CompoundName other = (CompoundName) obj;
            if (!Arrays.equals(names, other.names))
                return false;
            return true;
        }

        public Name getLocalName() {
            return this;
        }

        @Override
        public void appendXPathLocalProperties(StringBuilder sb) {
            for (Name name : names) {
                name.appendXPathLocalProperties(sb);
            }
        }
        
        public boolean contains(Name name) {
            for (Name n : names) {
                if (n.equals(name)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean refinesTo(Name name) {
            name = name.getLocalName();
            if (name instanceof CompoundName) {
                CompoundName that = (CompoundName) name;
                if (that.names.length > this.names.length) {
                    return false;
                }
                for (Name n : that.names) {
                    if (!contains(n)) {
                        return false;
                    }
                }
                return true;
            }
            return contains(name);
        }

    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    Namer[] namers;

    CompoundNamer(Namer... namer) {
        super(collectType(namer));
        this.namers = namer;
    }

    private static EnumSet<NamerType> collectType(Namer[] namers) {
        EnumSet<NamerType> types = NamerType.noneOf();
        for (Namer namer : namers) {
            if (!(namer instanceof SingletonNamer)) {
                throw new IllegalArgumentException("Do not support nested compound namer.");
            }
            types.addAll(namer.getNamerTypes());
        }
        return types;
    }

    @Override
    public Name naming(GUITreeNode node) {
        Name[] names = new Name[namers.length];
        int i = 0;
        for (Namer namer : namers) {
            Name name = (Name) namer.naming(node);
            if (name == null) {
                throw new IllegalStateException("Name should not be null");
            }
            names[i++] = name;
        }
        return NameManager.getCachedName(new CompoundName(names));
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CompoundNamer[");
        if (namers.length > 0) {
            sb.append(namers[0]);
            for (int i = 1; i < namers.length; i++) {
                sb.append(",");
                sb.append(namers[i]);
            }
        }
        sb.append("]");
        return sb.toString();
    }

}
