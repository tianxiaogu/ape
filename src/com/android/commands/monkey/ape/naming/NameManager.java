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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.android.commands.monkey.ape.utils.Utils;

public class NameManager {

    private static Map<Namer, Map<String, Name>> names = new HashMap<>();
    private static List<Name> nameList = new ArrayList<>(10000); // let gc happy?
    public static Name getCachedName(Name name) {
        Namer namer = name.getNamer();
        String key = name.toString();
        Name existing = Utils.getFromMapMap(names, namer, key);
        if (existing != null) {
            return existing;
        }
        Utils.addToMapMap(names, namer, key, name);
        ((AbstractName)name).setOrder(nameList.size());
        nameList.add(name);
        return name;
    }
}
