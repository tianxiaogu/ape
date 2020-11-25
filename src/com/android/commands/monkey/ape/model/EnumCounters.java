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

import java.io.Serializable;

import com.android.commands.monkey.ape.utils.Logger;

public abstract class EnumCounters<E extends Enum<E>> implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -3224038897052376225L;
    private int[] counters;
    private int total;

    public EnumCounters() {
        counters = new int[getEnums().length];
    }

    public void logEvent(E e) {
        int i = e.ordinal();
        counters[i] = counters[i] + 1;
        total++;
    }

    public int getTotal() {
        return total;
    }

    public void print() {
        E[] es = getEnums();
        for (E e : es) {
            Logger.format("%6d  %s", counters[e.ordinal()], e);
        }
    }

    public abstract E[] getEnums();
}
