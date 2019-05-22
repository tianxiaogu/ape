package com.android.commands.monkey.ape.naming;

public abstract class AbstractLocalName extends AbstractName {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public void toXPath(StringBuilder sb) {
        sb.append("//*");
        appendXPathLocalProperties(sb);
    }

    @Override
    public boolean refinesTo(Name name) {
        return equals(name);
    }
}
