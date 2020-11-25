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
package com.android.commands.monkey.ape;

import com.android.commands.monkey.ape.model.Action;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

public interface Agent {

    boolean activityResuming(String arg0);

    boolean activityStarting(Intent arg0, String arg1);

    boolean appCrashed(String processName, int pid, String shortMsg, String longMsg, long timeMillis,
            String stackTrace);

    int appEarlyNotResponding(String arg0, int arg1, String arg2);

    int appNotResponding(String arg0, int arg1, String arg2);

    int systemNotResponding(String arg0);

    void startNewEpisode();

    int getTimestamp();

    boolean canFuzzing();

    Action generateFuzzingAction();

    Rect getCurrentRootNodeBounds();

    void appendToActionHistory(long clockTimestamp, Action action);

    Action updateState(ComponentName topComp, AccessibilityNodeInfo info);

    int nextInt(int bound);

    void tearDown();

    void onAppActivityStarted(ComponentName app, boolean clean);

    void onActivityBlocked(ComponentName blockedActivity);

    void onActivityStopped();

    boolean onGraphStable(int counter);

    boolean onStateStable(int counter);

    boolean onVoidGUITree(int counter);

    boolean onLostFocused(int counter);

    void notifyActionConsumed();
}
