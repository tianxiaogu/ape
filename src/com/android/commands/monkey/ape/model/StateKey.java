package com.android.commands.monkey.ape.model;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Arrays;

import com.android.commands.monkey.ape.naming.Name;
import com.android.commands.monkey.ape.naming.NamerFactory;
import com.android.commands.monkey.ape.naming.Naming;
import com.android.commands.monkey.ape.utils.Config;
import com.android.commands.monkey.ape.utils.Logger;

import android.content.ComponentName;

public class StateKey implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * The class name of the activity
     */
    private String activity;
    private Naming naming;
    private Name[] widgets;
    private int hashCode;

    public StateKey(ComponentName activity, Naming naming, Name[] widgets) {
        this.activity = activity.getClassName();
        this.naming = naming;
        this.widgets = widgets;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((activity == null) ? 0 : activity.hashCode());
            result = prime * result + ((naming == null) ? 0 : naming.hashCode());
            result = prime * result + Arrays.hashCode(widgets);
            hashCode = result;
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StateKey other = (StateKey) obj;
        if (activity == null) {
            if (other.activity != null)
                return false;
        } else if (!activity.equals(other.activity))
            return false;
        if (naming == null) {
            if (other.naming != null)
                return false;
        } else if (!naming.equals(other.naming))
            return false;
        if (!Arrays.equals(widgets, other.widgets))
            return false;
        return true;
    }


    public void printWidgets() {
        int count = 1;
        for (Name w : widgets) {
            Logger.format("%5d %s", count++, w);
        }
    }

    public String toString() {
        return this.activity + '@' + hashCode() + '@' + this.naming + '@' + "[W=" + widgets.length + "]";
    }

    public void dumpState() {
        for (int i = 0; i < widgets.length; i++) {
            Logger.format("%3d %s", i, widgets[i]);
        }
    }

    public void saveState(PrintWriter pw) {
        for (int i = 0; i < widgets.length; i++) {
            pw.format("%3d %s\n", i, widgets[i]);
        }
    }

    public boolean containsTarget(Name target) {
        int index = Arrays.binarySearch(widgets, target);
        return index >= 0;
    }

    public Naming getNaming() {
        return naming;
    }

    public Name[] getWidgets() {
        return widgets;
    }

    public String getActivity() {
        return activity;
    }

    public boolean isTrivialState() {
        if (widgets.length <= Config.trivialStateWidgetThreshold) {
            return true;
        }
        int action = 0;
        for (Name name : widgets) {
            if (NamerFactory.hasAction(name)) {
                action ++;
            }
        }
        return action <= Config.trivialStateActionThreshold;
    }
}
