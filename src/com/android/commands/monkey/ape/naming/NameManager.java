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
