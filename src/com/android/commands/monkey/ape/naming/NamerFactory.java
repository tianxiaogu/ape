package com.android.commands.monkey.ape.naming;

import static com.android.commands.monkey.ape.utils.Config.useAncestorNamer;
import static com.android.commands.monkey.ape.utils.Config.usePatchNamer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

import com.android.commands.monkey.ape.model.ActionType;
import com.android.commands.monkey.ape.model.StateKey;
import com.android.commands.monkey.ape.naming.ActionPatchNamer.ActionPatchName;
import com.android.commands.monkey.ape.utils.XPathBuilder;

import android.content.ComponentName;

public class NamerFactory {


    /**
     * We will serialize an attribute path into .
     * Syntax: AP ::= (KEY `=' VALUE `;') * 
    */
    public static final Pattern KEY_VALUE_MATCHER = Pattern.compile("([^=]+)=([^;]+);");
    public static final String KEY_VALUE_SEP = "=";
    public static final String PROP_SEP = ";";
    public static final String NODE_SEP = "/";

    // Simply use URL encoding to esacpe KEY_VALUE_SEP, PROP_SEP, and NODE_SEP
    public static final String KEY_VALUE_SEP_URL = "%3D";
    public static final String PROP_SEP_URL = "%3B";
    public static final String NODE_SEP_URL = "%2F";

    public static final String INDEX_PROP_NAME = "index";

    /*
     * We do not want loose any interactive widgets.
     */
    private static final Namer emptyNamer = EmptyNamer.emptyNamer;
    // 1 kinds of info, total 4 namers
    private static final Namer typeNamer = new TypeNamer();
    private static final Namer textNamer = new TextNamer();
    private static final Namer indexNamer = new IndexNamer();
    private static final Namer parentNamer = new ParentNamer(emptyNamer);
    // 2 kinds of info, total 6 namers
    private static final Namer parentTypeNamer = new ParentNamer(typeNamer);
    private static final Namer parentTextNamer = new ParentNamer(textNamer);
    private static final Namer parentIndexNamer = new ParentNamer(indexNamer);
    private static final Namer typeTextNamer = new CompoundNamer(typeNamer, textNamer);
    private static final Namer typeIndexNamer = new CompoundNamer(typeNamer, indexNamer);
    private static final Namer textIndexNamer = new CompoundNamer(textNamer, indexNamer);
    // 3 kinds of info, total 4 namers
    private static final Namer parentTypeTextNamer = new ParentNamer(typeTextNamer);
    private static final Namer parentTypeIndexNamer = new ParentNamer(typeIndexNamer);
    private static final Namer parentTextIndexNamer = new ParentNamer(textIndexNamer);
    private static final Namer typeTextIndexNamer = new CompoundNamer(typeNamer, textNamer, indexNamer);

    // 4 kinds of info, total 1 namer
    private static final Namer parentTypeTextIndexNamer = new ParentNamer(typeTextIndexNamer);

    // ancestor namers
    private static final Namer ancestorNamer = new AncestorNamer(emptyNamer);
    private static final Namer ancestorTypeNamer = new AncestorNamer(typeNamer);
    private static final Namer ancestorIndexNamer = new AncestorNamer(indexNamer);
    private static final Namer ancestorTextNamer = new AncestorNamer(textNamer);
    private static final Namer ancestorParentNamer = new AncestorNamer(parentNamer);
    private static final Namer ancestorTypeTextNamer = new AncestorNamer(typeTextNamer);
    private static final Namer ancestorTypeIndexNamer = new AncestorNamer(typeIndexNamer);
    private static final Namer ancestorTextIndexNamer = new AncestorNamer(textIndexNamer);
    private static final Namer ancestorParentTypeNamer = new AncestorNamer(parentTypeNamer);
    private static final Namer ancestorParentTextNamer = new AncestorNamer(parentTextNamer);
    private static final Namer ancestorParentIndexNamer = new AncestorNamer(parentIndexNamer);
    private static final Namer ancestorParentTypeTextNamer = new AncestorNamer(parentTypeTextNamer);
    private static final Namer ancestorParentTypeIndexNamer = new AncestorNamer(parentTypeIndexNamer);
    private static final Namer ancestorParentTextIndexNamer = new AncestorNamer(parentTextIndexNamer);
    private static final Namer ancestorTypeTextIndexNamer = new AncestorNamer(typeTextIndexNamer);
    private static final Namer ancestorParentTypeTextIndexNamer = new AncestorNamer(parentTypeTextIndexNamer);

    private static final List<Namer> ALL;
    private static final List<Namer> PATCHED_ALL;
    public static final NamerLattice CURRENT;


    static {
        ALL = new ArrayList<Namer>(20);
        ALL.add(emptyNamer);
        ALL.add(typeNamer);
        ALL.add(textNamer);
        ALL.add(indexNamer);
        ALL.add(parentNamer);
        ALL.add(typeTextNamer);
        ALL.add(typeIndexNamer);
        ALL.add(textIndexNamer);
        ALL.add(typeTextIndexNamer);
        ALL.add(parentTypeNamer);
        ALL.add(parentTextNamer);
        ALL.add(parentIndexNamer);
        ALL.add(parentTypeTextNamer);
        ALL.add(parentTypeIndexNamer);
        ALL.add(parentTextIndexNamer);
        ALL.add(parentTypeTextIndexNamer);

        if (useAncestorNamer) {
            ALL.add(ancestorNamer);
            ALL.add(ancestorTypeNamer);
            ALL.add(ancestorTextNamer);
            ALL.add(ancestorIndexNamer);
            ALL.add(ancestorParentNamer);
            ALL.add(ancestorTypeTextNamer);
            ALL.add(ancestorTypeIndexNamer);
            ALL.add(ancestorTextIndexNamer);
            ALL.add(ancestorTypeTextIndexNamer);
            ALL.add(ancestorParentTypeNamer);
            ALL.add(ancestorParentTextNamer);
            ALL.add(ancestorParentIndexNamer);
            ALL.add(ancestorParentTypeTextNamer);
            ALL.add(ancestorParentTypeIndexNamer);
            ALL.add(ancestorParentTextIndexNamer);
            ALL.add(ancestorParentTypeTextIndexNamer);
        }

        PATCHED_ALL = new ArrayList<Namer>(ALL.size());
        for (Namer base : ALL) {
            PATCHED_ALL.add(new ActionPatchNamer(base));
        }

        if (usePatchNamer) {
            CURRENT = new NamerLattice(PATCHED_ALL);
        } else {
            CURRENT = new NamerLattice(ALL);
        }
    }

    public static StateKey buildStateKey(Naming naming, ComponentName activity, Name[] widgets) {
        return new StateKey(activity, naming, widgets);
    }

    public static String encode(String base, ActionType... actions) {
        throw new UnsupportedOperationException("TODO: unless performance problems");
    }

    public static Collection<Namer> notBelow(Namer namer) {
        return CURRENT.getNotBelow(namer);
    }

    public static Collection<Namer> all() {
        return CURRENT.all();
    }

    public static Collection<Namer> above(Namer namer) {
        return CURRENT.getUpper(namer);
    }

    public static Namer bottomNamer() {
        return CURRENT.getBottomNamer();
    }

    public static Namer topNamer() {
        return CURRENT.getTopNamer();
    }

    public static Namer fullNamer() {
        return CURRENT.getNamer(EnumSet.of(NamerType.ANCESTOR, NamerType.INDEX, NamerType.TYPE, NamerType.TEXT));
    }

    public static List<Namer> getSortedAbove(Namer namer) {
        return CURRENT.getSortedAbove(namer);
    }

    public static Namer getNamer(NamerType first, NamerType... others) {
        return CURRENT.getNamer(first, others);
    }

    public static Namer getNamer(EnumSet<NamerType> types) {
        return CURRENT.getNamer(types);
    }

    public static Namer join(Namer n1, Namer n2) {
        return CURRENT.join(n1, n2);
    }

    public static boolean hasAction(Name name) {
        if (name instanceof ActionPatchName) {
            return ((ActionPatchName) name).hasAction();
        }
        throw new IllegalArgumentException("Unsupported name: " + name);
    }

    public static List<ActionType> decodeActions(Name name) {
        if (name instanceof ActionPatchName) {
            return ((ActionPatchName)name).getActions();
        }
        throw new IllegalArgumentException("Unsupport name: " + name);
    }

    public static String nameToXPathString(Name name) {
        String xpathStr = name.toXPath();
        XPathBuilder.compileAbortOnError(xpathStr);
        return xpathStr;
    }

    public static String escapeToXPathString(String origin) {
        return origin.replaceAll("\"", "\\\""); // TODO: no escape right now
    }

    public static void build(StringBuilder sb, String prop, String value) {
        sb.append(prop);
        sb.append(KEY_VALUE_SEP);
        sb.append(value);
        sb.append(PROP_SEP);
    }

    public static String appendMetaName(String base, String patch) {
        return base + patch;
    }

    public static String concatinate(String parent, String self) {
        return parent + NODE_SEP + self;
    }

    public static Namer getLocalNamer(Namer namer) {
        EnumSet<NamerType> newTypes = NamerType.noneOf();
        for (NamerType type : namer.getNamerTypes()) {
            if (type.isLocal()) {
                newTypes.add(type);
            }
        }
        return CURRENT.getNamer(newTypes);
    }

    public static boolean isLocalNamer(Namer namer) {
        for (NamerType type : namer.getNamerTypes()) {
            if (!type.isLocal()) {
                return false;
            }
        }
        return true;
    }
}
