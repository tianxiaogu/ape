package com.android.commands.monkey.ape.model;

public class ActionCounters extends EnumCounters<ActionType> {

    /**
     * 
     */
    private static final long serialVersionUID = -3134953090850216534L;

    @Override
    public ActionType[] getEnums() {
        return ActionType.values();
    }

}
