package com.android.commands.monkey.ape.model.xpathaction;

import java.util.List;

public class XPathActionSequence {

    final double probability;
    final List<XPathAction> actions;

    public XPathActionSequence(double probability, List<XPathAction> actions) {
        this.probability = probability;
        this.actions = actions;
    }

    public double getProbability() {
        return probability;
    }

    public List<XPathAction> getActions() {
        return actions;
    }

    public boolean isEmpty() {
        return actions.isEmpty();
    }

    public XPathAction get(int i) {
        return actions.get(i);
    }

    public int size() {
        return actions.size();
    }
}
