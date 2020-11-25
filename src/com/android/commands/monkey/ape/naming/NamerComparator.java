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

import java.util.Comparator;
import java.util.EnumSet;

public class NamerComparator implements Comparator<Namer> {

    public static NamerComparator INSTANCE = new NamerComparator();

    private NamerComparator() {
    }

    public int compare(Namer n1, Namer n2) {
        EnumSet<NamerType> t1 = n1.getNamerTypes();
        EnumSet<NamerType> t2 = n2.getNamerTypes();
        int size = t1.size();
        int ret = size - t2.size();
        if (ret != 0) {
            return ret;
        }
        int c1 = 0;
        int c2 = 0;
        for (NamerType t : t1) {
            c1 += t.ordinal();
        }
        for (NamerType t : t2) {
            c2 += t.ordinal();
        }
        return c1 - c2;
    }
}
