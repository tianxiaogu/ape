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

import com.android.commands.monkey.ape.tree.GUITreeNode;

public class EmptyNamer extends AbstractNamer {

    public static final String EMPTY_NAME_STRING = "";

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static EmptyNamer emptyNamer = new EmptyNamer();
    public static Name emptyName = NameManager.getCachedName(new AbstractLocalName() {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        @Override
        public Namer getNamer() {
            return emptyNamer;
        }

        public String toString() {
            return EMPTY_NAME_STRING;
        }

        @Override
        public Name getLocalName() {
            return this;
        }

        @Override
        public String toXPath() {
            return "//*";
        }

        @Override
        public void appendXPathLocalProperties(StringBuilder sb) { }

    });

    private EmptyNamer() {
        super(NamerType.noneOf());
    }

    public String toString() {
        return "EmptyNamer";
    }

    @Override
    public Name naming(GUITreeNode node) {
        return EmptyNamer.emptyName;
    }
}
