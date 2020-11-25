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

import org.json.JSONException;
import org.json.JSONObject;

public class ApeEvents {

    public static ApeEvent toApeEvent(JSONObject jEvent) throws JSONException {
        String type = jEvent.getString("type");
        if (type.equals(ApeClickEvent.class.getSimpleName())) {
            return ApeClickEvent.fromJSONObject(jEvent);
        }
        if (type.equals(ApeAppSwitchEvent.class.getSimpleName())) {
            return ApeAppSwitchEvent.fromJSONObject(jEvent);
        }
        if (type.equals(ApeDragEvent.class.getSimpleName())) {
            return ApeDragEvent.fromJSONObject(jEvent);
        }
        if (type.equals(ApePinchOrZoomEvent.class.getSimpleName())) {
            return ApePinchOrZoomEvent.fromJSONObject(jEvent);
        }
        if (type.equals(ApeKeyEvent.class.getSimpleName())) {
            return ApeKeyEvent.fromJSONObject(jEvent);
        }
        if (type.equals(ApeRotationEvent.class.getSimpleName())) {
            return ApeRotationEvent.fromJSONObject(jEvent);
        }
        if (type.equals(ApeTrackballEvent.class.getSimpleName())) {
            return ApeTrackballEvent.fromJSONObject(jEvent);
        }
        throw new IllegalArgumentException("Unknown event type: " + type);
    }
}
