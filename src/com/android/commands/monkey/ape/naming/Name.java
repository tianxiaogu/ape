package com.android.commands.monkey.ape.naming;

public interface Name extends Comparable<Name> {
    Namer getNamer();
    Name getLocalName();
    boolean refinesTo(Name other);
    String toXPath();
    void appendXPathLocalProperties(StringBuilder sb);
    void toXPath(StringBuilder sb);
}
