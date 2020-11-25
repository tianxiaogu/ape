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
package com.android.commands.monkey.ape.events;

import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.commands.monkey.MonkeyEvent;
import com.android.commands.monkey.MonkeyKeyEvent;
import com.android.commands.monkey.MonkeyThrottleEvent;

import android.view.KeyEvent;

public class ApeAppSwitchEvent extends AbstractApeEvent {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    boolean home;

    public ApeAppSwitchEvent(boolean home) {
        this.home = home;
    }

    @Override
    public List<MonkeyEvent> generateMonkeyEvents() {
        MonkeyKeyEvent appSwitchDown = new MonkeyKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_APP_SWITCH);
        MonkeyKeyEvent appSwitchUp = new MonkeyKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_APP_SWITCH);
        MonkeyKeyEvent postDown, postUp;
        MonkeyThrottleEvent throttle = new MonkeyThrottleEvent(500);
        if (home) {
            postDown = new MonkeyKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HOME);
            postUp = new MonkeyKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HOME);
        } else {
            postDown = new MonkeyKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK);
            postUp = new MonkeyKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK);
        }
        return Arrays.asList(appSwitchDown, appSwitchUp, throttle, postDown, postUp);
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject jEvent = super.toJSONObject();
        jEvent.put("home", home);
        return jEvent;
    }

    public static ApeEvent fromJSONObject(JSONObject jEvent) throws JSONException {
        boolean home = jEvent.getBoolean("home");
        return new ApeAppSwitchEvent(home);
    }
}
