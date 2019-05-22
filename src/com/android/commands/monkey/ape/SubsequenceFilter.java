package com.android.commands.monkey.ape;

import com.android.commands.monkey.ape.model.StateTransition;

public interface SubsequenceFilter {

    boolean include(Subsequence path);

    boolean extend(Subsequence path, StateTransition edge);
}
