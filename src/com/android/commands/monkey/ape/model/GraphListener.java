package com.android.commands.monkey.ape.model;

public interface GraphListener {
    void onAddNode(State node);

    void onVisitStateTransition(StateTransition edge);
}
