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
package com.android.commands.monkey.ape.utils;

import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class XPathBuilder {

    static XPathFactory factory;
    static XPath xpath;
    static Map<String, XPathExpression> cached = new HashMap<>();

    static {
        factory = XPathFactory.newInstance();
        xpath = factory.newXPath();
    }

    private XPathBuilder() {}

    public static XPathExpression compile(String exprStr) throws XPathExpressionException {
        return xpath.compile(exprStr);
    }

    public static XPathExpression compileAbortOnError(String exprStr) {
        try {
            return compile(exprStr);
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Cannot compile xpath " + exprStr, e);
        }
    }
}
