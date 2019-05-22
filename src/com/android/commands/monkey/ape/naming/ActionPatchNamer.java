package com.android.commands.monkey.ape.naming;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.android.commands.monkey.ape.model.ActionType;
import com.android.commands.monkey.ape.model.ScrollType;
import com.android.commands.monkey.ape.tree.GUITreeNode;

public class ActionPatchNamer extends AbstractNamer implements Serializable {

    class ActionPatchName extends AbstractName {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        Name baseName;
        int patch;
        ScrollType scrollType;
        List<ActionType> actions;

        public ActionPatchName(Name baseName, int patch, ScrollType scrollType) {
            this.baseName = baseName;
            this.patch = patch;
            this.scrollType = scrollType;
            this.actions = buildActions();
        }

        public boolean hasAction() {
            return !actions.isEmpty();
        }

        private List<ActionType> buildActions() {
            List<ActionType> actionTypes = new ArrayList<>(6);
            String patch = patches[this.patch];
            if (patch.contains("clickable=true") || patch.contains("checkable=true")) {
                actionTypes.add(ActionType.MODEL_CLICK);
            }
            if (patch.contains("long-clickable=true")) {
                actionTypes.add(ActionType.MODEL_LONG_CLICK);
            }
            switch (scrollType) {
            case all:
                actionTypes.add(ActionType.MODEL_SCROLL_BOTTOM_UP);
                actionTypes.add(ActionType.MODEL_SCROLL_TOP_DOWN);
                actionTypes.add(ActionType.MODEL_SCROLL_LEFT_RIGHT);
                actionTypes.add(ActionType.MODEL_SCROLL_RIGHT_LEFT);
                break;
            case horizontal:
                actionTypes.add(ActionType.MODEL_SCROLL_LEFT_RIGHT);
                actionTypes.add(ActionType.MODEL_SCROLL_RIGHT_LEFT);
                break;
            case vertical:
                actionTypes.add(ActionType.MODEL_SCROLL_BOTTOM_UP);
                actionTypes.add(ActionType.MODEL_SCROLL_TOP_DOWN);
                break;
            default:
            }
            return Collections.unmodifiableList(actionTypes);
        }

        public boolean refinesTo(Name name) {
            if (name instanceof ActionPatchName) {
                ActionPatchName that = (ActionPatchName) name;
                if (patch != that.patch) {
                    return false;
                }
                if (!this.scrollType.equals(that.scrollType)) {
                    return false;
                }
                return this.baseName.refinesTo(that.baseName);
            }
            return baseName.refinesTo(name);
        }

        @Override
        public Namer getNamer() {
            return ActionPatchNamer.this;
        }

        public String toString() {
            return baseName.toString() + ActionPatchNamer.patches[patch];
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((baseName == null) ? 0 : baseName.hashCode());
            result = prime * result + patch;
            result = prime * result + ((scrollType == null) ? 0 : scrollType.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ActionPatchName other = (ActionPatchName) obj;
            if (baseName == null) {
                if (other.baseName != null)
                    return false;
            } else if (!baseName.equals(other.baseName))
                return false;
            if (patch != other.patch)
                return false;
            if (scrollType != other.scrollType)
                return false;
            return true;
        }

        public List<ActionType> getActions() {
            return actions;
        }

        @Override
        public Name getLocalName() {
            ActionPatchNamer localNamer = (ActionPatchNamer) NamerFactory.getLocalNamer(ActionPatchNamer.this);
            return localNamer.createName(this.baseName.getLocalName(), this.patch, this.scrollType);
        }

        @Override
        public void toXPath(StringBuilder sb) {
            baseName.toXPath(sb);
            int k = patch;
            for (String prop : interactiveProperties) {
                sb.append("[@");
                sb.append(prop);
                sb.append("=");
                if (k % 2 == 1) {
                    sb.append("'true'");
                } else {
                    sb.append("'false'");
                }
                sb.append("]");
                k = k >> 1;
            }
        }

        @Override
        public void appendXPathLocalProperties(StringBuilder sb) {
            baseName.appendXPathLocalProperties(sb);
            int k = patch;
            for (String prop : interactiveProperties) {
                sb.append("[@");
                sb.append(prop);
                sb.append("=");
                if (k % 2 == 1) {
                    sb.append("'true'");
                } else {
                    sb.append("'false'");
                }
                sb.append("]");
                k = k >> 1;
            }
        }

    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static String[] interactiveProperties = new String[] { "enabled", "clickable", "checkable", "long-clickable",
    "scrollable" };


    private static final String[] patches;
    static {
        patches = new String[1 << interactiveProperties.length];
        for (int i = 0; i < patches.length; i++) {
            patches[i] = generatePatch(i);
        }
    }

    private Namer baseNamer;

    public ActionPatchNamer(Namer baseNamer) {
        super(baseNamer.getNamerTypes());
        this.baseNamer = baseNamer;
    }

    public Namer getBaseNamer() {
        return baseNamer;
    }

    private static String generatePatch(int k) {
        StringBuilder sb = new StringBuilder();
        for (String prop : interactiveProperties) {
            boolean val = k % 2 == 1;
            if (val) {
                NamerFactory.build(sb, prop, String.valueOf(val));
            }
            k = k >> 1;
        }
        return sb.toString();
    }

    private boolean checkProperty(String prop, GUITreeNode node) {
        if (prop.equals("enabled")) {
            return node.isEnabled();
        }
        if (prop.equals("clickable")) {
            return node.isClickable();
        }
        if (prop.equals("checkable")) {
            return node.isCheckable();
        }
        if (prop.equals("scrollable")) {
            return node.isScrollable();
        }
        if (prop.equals("long-clickable")) {
            return node.isLongClickable();
        }
        throw new RuntimeException("Unsupported property: " + prop);
    }
    
    private int patch(GUITreeNode node) {
        int flag = 0;
        for (int i = interactiveProperties.length - 1; i >= 0; i--) {
            flag = flag << 1;
            String prop = interactiveProperties[i];
            if (checkProperty(prop, node)) {
                flag |= 1;
            }
        }
        return flag;
    }

    public String toString() {
        return "ActionPatchNamer[" + getBaseNamer() + "]";
    }

    Name createName(Name baseName, int patch, ScrollType scrollType) {
        return NameManager.getCachedName(new ActionPatchName(baseName, patch, scrollType));
    }

    @Override
    public Name naming(GUITreeNode node) {
        Name baseName = baseNamer.naming(node);
        int patch = patch(node);
        ScrollType scrollType = ScrollType.valueOf(node.getScrollType());
        return createName(baseName, patch, scrollType);
    }

}
