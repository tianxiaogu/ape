package com.android.commands.monkey.ape.naming;

import com.android.commands.monkey.ape.tree.GUITreeNode;

public class EmptyNamer extends AbstractNamer {

    public static final String EMPTY_NAME_STRING = "";

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static EmptyNamer emptyNamer = new EmptyNamer();
    public static Name emptyName = NameManager.getCachedName(new AbstractLocalName() {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        @Override
        public Namer getNamer() {
            return emptyNamer;
        }

        public String toString() {
            return EMPTY_NAME_STRING;
        }

        @Override
        public Name getLocalName() {
            return this;
        }

        @Override
        public String toXPath() {
            return "//*";
        }

        @Override
        public void appendXPathLocalProperties(StringBuilder sb) { }

    });

    private EmptyNamer() {
        super(NamerType.noneOf());
    }

    public String toString() {
        return "EmptyNamer";
    }

    @Override
    public Name naming(GUITreeNode node) {
        return EmptyNamer.emptyName;
    }
}
