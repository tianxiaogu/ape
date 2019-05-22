package com.android.commands.monkey.ape.model.xpathaction;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathExpression;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.commands.monkey.ape.model.ActionType;
import com.android.commands.monkey.ape.utils.Logger;
import com.android.commands.monkey.ape.utils.XPathBuilder;

public class XPathActionReader {

    public List<XPathActionSequence> read(File jsonFile) {
        try (InputStream is = new FileInputStream(jsonFile)) {
            int bufSize = is.available();
            byte[] buffer = new byte[bufSize];
            is.read(buffer);
            String jsonString = new String(buffer);
            JSONArray root = new JSONArray(jsonString);
            int length = root.length();
            List<XPathActionSequence> pathlets = new ArrayList<>(length);

            for (int i = 0; i < length; i++) {
                pathlets.add(buildActions(root.getJSONObject(i)));
            }
            return pathlets;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        System.exit(1);
        return null;
    }

    private XPathActionSequence buildActions(JSONObject json) throws JSONException {
        double prob = json.getDouble("prob");
        JSONArray actionArray = json.getJSONArray("actions");
        int length = actionArray.length();
        List<XPathAction> actions = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            actions.add(build(actionArray.getJSONObject(i)));
        }
        return new XPathActionSequence(prob, actions);
    }

    private XPathAction build(JSONObject json) {
        try {
            String exprStr = json.getString("xpath");
            XPathExpression expr = XPathBuilder.compile(exprStr);
            XPathAction xpathlet = new XPathAction(exprStr, expr);
            if (json.has("action")) {
                ActionType actionTypes = parseActionType(json.getString("action"));
                xpathlet.setAction(actionTypes);
            }
            if (json.has("text")) {
                String text = json.getString("text");
                xpathlet.setText(text);
            }
            if (json.has("throttle")) {
                int throttle = json.getInt("throttle");
                xpathlet.setThrottle(throttle);
            }
            return xpathlet;
        } catch (Exception e) {
            e.printStackTrace();
            Logger.wprintln("Malformed json: " + json);
            System.exit(1);
        }
        return null;
    }

    private ActionType parseActionType(String action) throws JSONException {
        if (action == null) {
            return null;
        }
        return ActionType.valueOf(action.toUpperCase());
    }
}
