/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.commands.monkey;

import android.hardware.display.DisplayManagerGlobal;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.InputDevice;

/**
 * monkey touch event
 */
public class MonkeyTouchEvent extends MonkeyMotionEvent {

    static final int statusBarHeight;
    static {
        Display display = DisplayManagerGlobal.getInstance().getRealDisplay(Display.DEFAULT_DISPLAY);
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        statusBarHeight = (int) (24 * dm.density);
    }

    public MonkeyTouchEvent(int action) {
        super(MonkeyEvent.EVENT_TYPE_TOUCH, InputDevice.SOURCE_TOUCHSCREEN, action);
    }

    @Override
    protected String getTypeLabel() {
        return "Touch";
    }

    public MonkeyMotionEvent addPointer(int id, float x, float y, float pressure, float size) {
        if (y < statusBarHeight) { // avoid touch status bar
            y = statusBarHeight + 1;
        }
        return super.addPointer(id, x, y, pressure, size);
    }
}
