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
