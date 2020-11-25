/*
 * Copyright 2020 Advanced Software Technologies Lab at ETH Zurich, Switzerland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.commands.monkey.ape.model;

/**
 * @author txgu
 *
 */
public enum ActionType {
    
    // a phantom action for logging.
    PHANTOM_CRASH,

    // Fuzz action
    FUZZ,
    
    EVENT_START, // start the activity
    EVENT_RESTART, // kill the process and start the activity
    EVENT_CLEAN_RESTART, // kill the process and clean the app cache, and start the activity
    EVENT_NOP, // throttle
    EVENT_ACTIVATE,

    // a model action can be used as a label of an edge in the model.
    MODEL_BACK,
    MODEL_CLICK,
    MODEL_LONG_CLICK,
    MODEL_SCROLL_TOP_DOWN,
    MODEL_SCROLL_BOTTOM_UP,
    MODEL_SCROLL_LEFT_RIGHT,
    MODEL_SCROLL_RIGHT_LEFT;

    public boolean requireTarget() {
        int ord = ordinal();
        return ord >= MODEL_CLICK.ordinal() && ord <= MODEL_SCROLL_RIGHT_LEFT.ordinal();
    }

    public boolean canStartApp() {
        return this == EVENT_START || this == EVENT_RESTART || this == EVENT_CLEAN_RESTART;
    }
    
    public boolean isScroll() {
        int ord = ordinal();
        return ord >= MODEL_SCROLL_TOP_DOWN.ordinal() && ord <= MODEL_SCROLL_RIGHT_LEFT.ordinal();
    }

    public boolean isModelAction() {
        int ord = ordinal();
        return ord >= MODEL_BACK.ordinal() && ord <= MODEL_SCROLL_RIGHT_LEFT.ordinal();
    }
}
