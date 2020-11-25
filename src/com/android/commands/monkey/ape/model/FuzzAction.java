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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.commands.monkey.ape.events.ApeEvent;
import com.android.commands.monkey.ape.events.ApeEvents;

public class FuzzAction extends Action {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final AtomicInteger fuzzIdGenerator = new AtomicInteger();
    private int id; // simply make every fuzz action unique.
    private List<ApeEvent> events;

    public FuzzAction(List<ApeEvent> events) {
        super(ActionType.FUZZ);
        this.events = events;
        this.id = fuzzIdGenerator.incrementAndGet();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        FuzzAction other = (FuzzAction) obj;
        if (id != other.id)
            return false;
        return true;
    }

    public List<ApeEvent> getFuzzingEvents() {
        return events;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject jAction = super.toJSONObject();
        JSONArray jEvents = new JSONArray();
        for (ApeEvent event : events) {
            jEvents.put(event.toJSONObject());
        }
        jAction.put("events", jEvents);
        return jAction;
    }

    public static Action fromJSON(JSONObject jAction) throws JSONException {
        JSONArray jEvents = jAction.getJSONArray("events");
        int length = jEvents.length();
        List<ApeEvent> events = new ArrayList<ApeEvent>(length);
        for (int i = 0; i < length; i++) {
            events.add(ApeEvents.toApeEvent(jEvents.getJSONObject(i)));
        }
        Action action = new FuzzAction(events);
        int throttle = jAction.getInt("throttle");
        action.setThrottle(throttle);
        return action;
    }
}
