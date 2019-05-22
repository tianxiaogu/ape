package com.android.commands.monkey.ape.model.xpathaction;

import java.util.EnumSet;

import javax.xml.xpath.XPathExpression;

import com.android.commands.monkey.ape.model.ActionType;
import com.android.commands.monkey.ape.naming.NamerType;

public class XPathlet {

    private final String exprStr;

    private final XPathExpression expr;

    private ActionType[] actions;

    public void setActions(ActionType[] actions) {
        this.actions = actions;
    }

    private String text;
    private int throttle;

    private EnumSet<NamerType> namerTypes;

    public XPathlet(String exprStr, XPathExpression expr) {
        this.exprStr = exprStr;
        this.expr = expr;
    }

    public String getExprStr() {
        return exprStr;
    }

    public XPathExpression getExpr() {
        return expr;
    }

    public ActionType[] getActions() {
        return actions;
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

    public EnumSet<NamerType> getNamerType() {
        return namerTypes;
    }

    public void setNamerTypes(EnumSet<NamerType> namerType) {
        this.namerTypes = namerType;
    }
}
