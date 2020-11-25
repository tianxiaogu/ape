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
package com.android.commands.monkey.ape.model.xpathaction;

import javax.xml.xpath.XPathExpression;

import com.android.commands.monkey.ape.model.ActionType;

/**
 * An experimental feature.
 * @author txgu
 *
 */
public class XPathAction {

    private final String exprStr;

    private final XPathExpression expr;

    private ActionType action;

    private String text;
    private int throttle;

    public XPathAction(String exprStr, XPathExpression expr) {
        this.exprStr = exprStr;
        this.expr = expr;
    }

    public void setAction(ActionType action) {
        this.action = action;
    }

    public String getExprStr() {
        return exprStr;
    }

    public XPathExpression getExpr() {
        return expr;
    }

    public ActionType getAction() {
        return action;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public void setThrottle(int throttle) {
        this.throttle = throttle;
    }

    public int getThrottle() {
        return throttle;
    }

}
