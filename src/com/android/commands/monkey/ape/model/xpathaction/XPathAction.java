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
