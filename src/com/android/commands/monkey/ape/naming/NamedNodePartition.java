package com.android.commands.monkey.ape.naming;

public class NamedNodePartition {

    private final String[] names;
    private final Object[] nodes;

    public NamedNodePartition(String[] names, Object[] nodes) {
        this.names = names;
        this.nodes = nodes;
    }

    public String[] getNames() {
        return names;
    }

    public Object[] getNodes() {
        return nodes;
    }

}
