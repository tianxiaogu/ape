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

import java.io.Serializable;


public abstract class AbstractName implements Name, Serializable {


    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private int order;

    protected transient String cachedXpathString;

    public AbstractName() {
        setOrder(-1);
    }

    @Override
    public int compareTo(Name o) {
        int ret = order - ((AbstractName)o).order;
        if (ret == 0) {
            if (this != o) {
                throw new IllegalStateException();
            }
        }
        return ret;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getOrder() {
        return this.order;
    }

    public String toXPath() {
        if (cachedXpathString == null) {
            StringBuilder sb = new StringBuilder();
            toXPath(sb);
            cachedXpathString = sb.toString();
        }
        return cachedXpathString;
    }

    /**
     * Not a strictly complete implementation.
     */
    public boolean refinesTo(Name name) {
        if (!getNamer().refinesTo(name.getNamer())) {
            return false;
        }
        Name localName = getLocalName();
        return localName.refinesTo(name.getLocalName());
    }
}
