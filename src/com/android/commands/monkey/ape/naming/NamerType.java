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
package com.android.commands.monkey.ape.naming;

import java.util.Arrays;
import java.util.EnumSet;

import com.android.commands.monkey.ape.utils.Config;

public enum NamerType {
    TYPE, INDEX, PARENT, TEXT, ANCESTOR;

    public static final NamerType[] used;
    static {
        if (Config.useAncestorNamer) {
            used = new NamerType[] { TYPE, INDEX, PARENT, TEXT, ANCESTOR };
        } else {
            used = new NamerType[] { TYPE, INDEX, PARENT, TEXT };
        }
    }

    public static final EnumSet<NamerType> allOf() {
        return EnumSet.copyOf(Arrays.asList(used));
    }

    public static final EnumSet<NamerType> noneOf() {
        return EnumSet.noneOf(NamerType.class);
    }

    public static final EnumSet<NamerType> complementOf(EnumSet<NamerType> set) {
        EnumSet<NamerType> empty = noneOf();
        for (NamerType n : used) {
            empty.add(n);
        }
        return empty;
    }

    public static final EnumSet<NamerType> add(EnumSet<NamerType> set, NamerType type) {
        EnumSet<NamerType> ret = EnumSet.copyOf(set);
        ret.add(type);
        return ret;
    }

    public boolean isLocal() {
        return this != PARENT && this != ANCESTOR;
    }
}
