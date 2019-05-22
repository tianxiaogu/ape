package com.android.commands.monkey.ape.model.xpathaction;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.xml.xpath.XPathExpression;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.commands.monkey.ape.model.ActionType;
import com.android.commands.monkey.ape.naming.NamerType;
import com.android.commands.monkey.ape.utils.Logger;
import com.android.commands.monkey.ape.utils.XPathBuilder;

public class XPathletReader {

    public List<XPathlet> read(File jsonFile) {
        try (InputStream is = new FileInputStream(jsonFile)) {
            int bufSize = is.available();
            byte[] buffer = new byte[bufSize];
            is.read(buffer);
            String jsonString = new String(buffer);
            JSONArray root = new JSONArray(jsonString);
            int length = root.length();
            List<XPathlet> pathlets = new ArrayList<XPathlet>(length);

            for (int i = 0; i < length; i++) {
                pathlets.add(build(root.getJSONObject(i)));
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

    private XPathlet build(JSONObject json) {
        try {
            String exprStr = json.getString("xpath");
            XPathExpression expr = XPathBuilder.compile(exprStr);
            XPathlet xpathlet = new XPathlet(exprStr, expr);
            if (json.has("actions")) {
                ActionType[] actionTypes = parseActionTypes(json.getJSONArray("actions"));
                xpathlet.setActions(actionTypes);
            }
            if (json.has("text")) {
                String text = json.getString("text");
                xpathlet.setText(text);
            }
            if (json.has("throttle")) {
                int throttle = json.getInt("throttle");
                xpathlet.setThrottle(throttle);
            }
            if (json.has("namer")) {
                // EnumSet<NamerType> namerTypes = parseNamerType(json.getJSONArray("namer"));
                throw new RuntimeException("Not implemented yet..");
            }
            return xpathlet;
        } catch (Exception e) {
            e.printStackTrace();
            Logger.wprintln("Malformed json: " + json);
            System.exit(1);
        }
        return null;
    }

    EnumSet<NamerType> parseNamerType(JSONArray jsonArray) {
        return null;
    }

    private ActionType[] parseActionTypes(JSONArray actions) throws JSONException {
        if (actions == null) {
            return null;
        }
        int length = actions.length();
        ActionType[] actionTypes = new ActionType[length];
        for (int i = 0; i < length; i++) {
            String t = actions.getString(i);
            actionTypes[i] = ActionType.valueOf(t);
        }
        return actionTypes;
    }
}
