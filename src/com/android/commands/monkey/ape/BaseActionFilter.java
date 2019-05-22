package com.android.commands.monkey.ape;

import com.android.commands.monkey.ape.model.ModelAction;

public abstract class BaseActionFilter implements ActionFilter {
    public int getPrority(ModelAction action) {
        return action.getPriority();
    }
}