package com.android.commands.monkey.ape.naming;

public enum GUITreeProperty {

    INDEX("index"), RESOURCE_ID("resource-id"), CLASS("class"), PACKAGE("package"), TEXT("text"), CLICKABLE(
            "clickable"), LONG_CLICKABLE("long-clickable"), CHECKABLE("checkable"), SCROLLABLE("scrollable"), ENABLED(
                    "enabled"), CHECKED("checked"), FOCUSABLE("focusable"), FOCUSED("focused"), BOUNDS("bounds");

    private final String attributeName;

    GUITreeProperty(String attrName) {
        this.attributeName = attrName;
    }

    public String getAttributeName() {
        return this.attributeName;
    }
}
