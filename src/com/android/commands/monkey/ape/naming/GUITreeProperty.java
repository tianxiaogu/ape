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
