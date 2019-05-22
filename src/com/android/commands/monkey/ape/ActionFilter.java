package com.android.commands.monkey.ape;

import com.android.commands.monkey.ape.model.ModelAction;

public interface ActionFilter {

    ActionFilter ALL = new BaseActionFilter() {

        @Override
        public boolean include(ModelAction action) {
            return true;
        }

    };

    ActionFilter WITH_TARGET = new BaseActionFilter() {

        @Override
        public boolean include(ModelAction action) {
            return action.requireTarget();
        }

    };

    ActionFilter VALID = new BaseActionFilter() {

        @Override
        public boolean include(ModelAction action) {
            return action.isValid();
        }

    };

    ActionFilter ENABLED_VALID_UNVISITED = new BaseActionFilter() {

        @Override
        public boolean include(ModelAction action) {
            return action.isEnabled() && action.isValid() && action.isUnvisited();
        }

    };

    ActionFilter ENABLED_VALID_UNSATURATED = new BaseActionFilter() {

        @Override
        public boolean include(ModelAction action) {
            return action.isEnabled() && action.isValid() && !action.isSaturated();
        }

    };

    ActionFilter ENABLED_VALID = new BaseActionFilter() {
        @Override
        public boolean include(ModelAction action) {
            return action.isEnabled() && action.isValid();
        }
    };

    boolean include(ModelAction action);
}
