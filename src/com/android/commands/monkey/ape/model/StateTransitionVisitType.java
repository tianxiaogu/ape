package com.android.commands.monkey.ape.model;

public enum StateTransitionVisitType {
    NEW_ACTION, // new edge for this action
    NEW_ACTION_TARGET, // new edge for this action and target
    EXISTING; // existing edge for this action and target
}
