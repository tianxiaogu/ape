package com.android.commands.monkey.ape;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.commands.monkey.ApeAPIAdapter;
import com.android.commands.monkey.ape.utils.Logger;
import com.android.commands.monkey.ape.utils.Utils;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.view.IInputMethodManager;

import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.IActivityManager;
import android.app.admin.IDevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManagerGlobal;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.view.IWindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodInfo;

public class AndroidDevice {

    private static Set<String> blacklistPermissions = new HashSet<String>();

    public static IActivityManager iActivityManager;

    public static IWindowManager iWindowManager;

    public static IPackageManager iPackageManager;

    public static IDevicePolicyManager iDevicePolicyManager;

    public static IStatusBarService iStatusBarService;

    public static IInputMethodManager iInputMethodManager;

    public static IPowerManager iPowerManager;

    public static boolean useADBKeyboard;

    public static Set<String> inputMethodPackages = new HashSet<>();

    public static void initializeAndroidDevice(IActivityManager mAm, IWindowManager mWm, IPackageManager mPm) {
        iActivityManager = mAm;
        iWindowManager = mWm;
        iPackageManager = mPm;

        iDevicePolicyManager = IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"));
        if (iDevicePolicyManager == null) {
            System.err.println("** Error: Unable to connect to deveice policy manager; is the system " + "running?");
        }

        iStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
        if (iStatusBarService == null) {
            System.err.println("** Error: Unable to connect to status bar service; is the system " + "running?");
        }

        iInputMethodManager = IInputMethodManager.Stub.asInterface(ServiceManager.getService("input_method"));
        if (iInputMethodManager == null) {
            System.err.println(
                    "** Error: Unable to connect to input method manager service; is the system " + "running?");
        }

        boolean useADBKeyboard = false;
        List<InputMethodInfo> inputMethods = ApeAPIAdapter.getEnabledInputMethodList(iInputMethodManager);
        if (inputMethods != null) {
            for (InputMethodInfo imi : inputMethods) {
                Logger.iprintln("InputMethod ID: " + imi.getId());
                inputMethodPackages.add(imi.getComponent().getPackageName());
                if (IME_ADB_KEYBOARD.equals(imi.getId())) {
                    Logger.iprintln("Find ADBKeybaord.");
                    useADBKeyboard = true;
                }
            }
        }

        AndroidDevice.useADBKeyboard = useADBKeyboard;

        iPowerManager = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
        if (iPowerManager == null) {
            System.err.println("** Error: Unable to connect to power manager service; is the system " + "running?");
        }
    }

    public static int getRotation() {
        try {
            return iWindowManager.getRotation();
        } catch (RemoteException e) {
            return -1;
        }
    }

    public static Rect getDisplayBounds() {
        android.view.Display display = DisplayManagerGlobal.getInstance().getRealDisplay(android.view.Display.DEFAULT_DISPLAY);
        Point size = new Point();
        display.getSize(size);
        Rect bounds = new Rect();
        bounds.top = 0;
        bounds.left = 0;
        bounds.right = size.x;
        bounds.bottom = size.y;
        return bounds;
    }
    
    public static ComponentName getTopActivityComponentName() {
        List<RunningTaskInfo> taskInfo = ApeAPIAdapter.getTasks(iActivityManager, Integer.MAX_VALUE);
        if (taskInfo == null || taskInfo.isEmpty()) {
            return null;
        }
        RunningTaskInfo task = taskInfo.get(0);
        ComponentName componentInfo = task.topActivity;
        return componentInfo;
    }

    public static boolean isVirtualKeyboardOpened() {
        int height = 0;
        try {
            height = AndroidDevice.iInputMethodManager.getInputMethodWindowVisibleHeight();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return height != 0;
    }

    public static void rotate(int degree, boolean persist) {
        try {
            iWindowManager.freezeRotation(degree);
            if (persist) {
                iWindowManager.thawRotation();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void checkInteractive() {
        try {
            if (!iPowerManager.isInteractive()) {
                Logger.format("Power Manager says we are NOT interactive");
                int ret = Runtime.getRuntime().exec(new String[] { "input", "keyevent", "26" }).waitFor();
                Logger.format("Wakeup ret code %d %s", ret, (iPowerManager.isInteractive() ? "Interactive" : "Not interactive"));
            } else {
                Logger.format("Power Manager says we are interactive");
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static int killPids(int ... pids) {
        try {
            iActivityManager.killPids(pids, "Monkey", true);
            return 0;
        } catch (SecurityException e) {
            return -1;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * https://github.com/senzhk/ADBKeyBoard
     */
    private static String IME_MESSAGE = "ADB_INPUT_TEXT";
    private static String IME_CHARS = "ADB_INPUT_CHARS";
    private static String IME_KEYCODE = "ADB_INPUT_CODE";
    private static String IME_EDITORCODE = "ADB_EDITOR_CODE";
    private static String IME_ADB_KEYBOARD = "com.android.adbkeyboard/.AdbIME";

    public static boolean checkAndSetInputMethod() {
        try {
            if (useADBKeyboard == false) {
                return false;
            }
            iInputMethodManager.setInputMethod(null, IME_ADB_KEYBOARD);
            return true;
        } catch (RemoteException e) {
            Logger.wformat("Fail to set input method to %s", IME_ADB_KEYBOARD);
            e.printStackTrace();
        }
        return false;
    }

    public static String[] getGrantedPermissions(String packageName) {
        try {
            PackageInfo packageInfo = iPackageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS,
                    UserHandle.myUserId());
            if (packageInfo == null) {
                return new String[0];
            }
            if (packageInfo.requestedPermissions == null) {
                return new String[0];
            }
            for (String s : packageInfo.requestedPermissions) {
                Logger.dformat("%s requrested permission %s", packageName, s);
            }
            return packageInfo.requestedPermissions;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean grantRuntimePermission(String packageName, String permission) {
        try {
            int ret = executeCommandAndWaitFor(new String[] { "pm", "grant", packageName, permission });
            return ret == 0;
        } catch (Exception e) {
            Logger.wformat("Granting saved permission %s top %s results in error %s", permission, packageName, e);
        }
        return false;
    }

    public static boolean grantRuntimePermissions(String packageName, String[] savedPermissions, String reason) {
        try {
            Logger.iformat("Try to grant saved permission to %s for %s... ", packageName, reason);
            for (String permission : savedPermissions) {
                try {
                    Logger.iformat("Grant saved permission %s to %s... ", permission, packageName);
                    // mPm.grantRuntimePermission(packageName, permission,
                    // UserHandle.myUserId());
                    if (grantRuntimePermission(packageName, permission)) {
                        Logger.iformat("Permission %s is granted to %s... ", permission, packageName);
                    } else {
                        Logger.iformat("Permission %s is NOT granted to %s... ", permission, packageName);
                    }
                } catch (RuntimeException e) {
                    if (!blacklistPermissions.contains(permission)) {
                        Logger.wformat("Granting saved permission %s top %s results in error %s", permission,
                                packageName, e);
                    }
                    blacklistPermissions.add(permission);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static void waitForNotify(Object lock) {
        synchronized (lock) {
            try {
                lock.wait(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } // at most wait for 5s
        }
    }

    public static boolean checkNativeApp(String packageName) {
        final PackageStats[] result = new PackageStats[1];
        try {
            IPackageStatsObserver observer = new IPackageStatsObserver() {

                @Override
                public IBinder asBinder() {
                    return null;
                }

                @Override
                public void onGetStatsCompleted(PackageStats pStats, boolean succeeded) throws RemoteException {
                    synchronized (this) {
                        if (succeeded) {
                            result[0] = pStats;
                        }
                        this.notifyAll();
                    }

                }

            };
            iPackageManager.getPackageSizeInfo(packageName, UserHandle.myUserId(), observer);
            waitForNotify(observer);
            if (result[0] != null) {
                PackageStats stat = result[0];
                Logger.format("Code size: %d", stat.codeSize);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int executeCommandAndWaitFor(String[] cmd) throws InterruptedException, IOException {
        return Runtime.getRuntime().exec(cmd).waitFor();
    }

    public static List<Integer> getPIDs(String packageName) {
        List<Integer> pids = new ArrayList<Integer>(3);
        try {
            List<RunningAppProcessInfo> processes = iActivityManager.getRunningAppProcesses();
            for (RunningAppProcessInfo proc : processes) {
                for (String pkg : proc.pkgList) {
                    if (packageName.equals(pkg)) {
                        pids.add(proc.pid);
                        break;
                    }
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return pids;
    }

    public static boolean stopPackage(String packageName) {
        int retryCount = 10;
        while (retryCount-- > 0) {
            List<Integer> pids = getPIDs(packageName);
            if (pids.isEmpty()) {
                return true;
            }
            Logger.println("Stop all packages, retry count " + retryCount);
            try {
                Logger.println("Try to stop package " + packageName);
                iActivityManager.forceStopPackage(packageName, UserHandle.myUserId());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static boolean clearPackage(String packageName) {
        try {
            int ret = executeCommandAndWaitFor(new String[] { "pm", "clear", packageName });
            return ret == 0;
        } catch (Exception e) {
            Logger.wformat("Clear package %s results in error %s", packageName, e);
        }
        return false;
    }

    public static boolean clearPackage(String packageName, String[] savedPermissions) {
        return clearPackage(packageName) && grantRuntimePermissions(packageName, savedPermissions, "clearing package");
    }

    public static boolean isInputMethod(String packageName) {
        return inputMethodPackages.contains(packageName);
    }

    public static boolean switchToLastInputMethod() {
        try {
            iInputMethodManager.switchToLastInputMethod(null);
            return true;
        } catch (RemoteException e) {
            Logger.wprintln("Fail to switch to last input method");
            e.printStackTrace();
        }
        return false;
    }

    public static void sendIMEActionGo() {
        sendIMEAction(EditorInfo.IME_ACTION_GO);
    }

    public static void sendIMEAction(int actionId) {
        // adb shell am broadcast -a ADB_EDITOR_CODE --ei code 2
        Intent intent = new Intent();
        intent.setAction(IME_EDITORCODE);
        intent.putExtra("code", actionId);
        sendIMEIntent(intent);
    }

    public static boolean sendIMEIntent(Intent intent) {
        try {
            if (checkAndSetInputMethod()) {
                return broadcastIntent(intent);
            }
            return false;
        } finally {
        }
    }

    public static boolean sendChars(int[] chars) {
        Intent intent = new Intent();
        intent.setAction(IME_CHARS);
        intent.putExtra("chars", chars);
        return sendIMEIntent(intent);
    }

    public static boolean sendInputKeyCode(int keycode) {
        Intent intent = new Intent();
        intent.setAction(IME_KEYCODE);
        intent.putExtra("code", keycode);
        return sendIMEIntent(intent);
    }

    private static boolean broadcastIntent(Intent intent) {
        try {
            iActivityManager.broadcastIntent(null, intent, null, null, 0, null, null, null, 0, null, false, false, 0);
            return true;
        } catch (RemoteException e) {
            Logger.wformat("Broadcast Intent error", intent);
            return false;
        }
    }

    public static boolean sendText(String text) {
        Intent intent = new Intent();
        intent.setAction(IME_MESSAGE);
        intent.putExtra("msg", text);
        return sendIMEIntent(intent);
        // sendIMEActionGo();
    }
    static Pattern FOCUSED_STACK_PATTERN = Pattern.compile("mFocusedStack=ActivityStack[{][a-z0-9]+ stackId=([0-9]+), [0-9]+ tasks[}]");
    static Pattern DISPLAY_PATTERN  = Pattern.compile("^Display #([0-9]+) .*:$");
    static Pattern STACK_PATTERN    = Pattern.compile("^  Stack #([0-9]+):$");
    static Pattern TASK_PATTERN     = Pattern.compile("^    Task id #([0-9]+)$");
    static Pattern ACTIVITY_PATTERN = Pattern.compile("^      [*] Hist #[0-9]+: ActivityRecord[{][0-9a-z]+ u[0-9]+ ([^ /]+)/([^ ]+) t[0-9]+[}]$");
    public static class Display {
        int id;
        int focusedStackId;
        List<Stack> stacks = new ArrayList<>();
        public Display(int id) {
            this.id = id;
        }
    }

    public static class Stack {
        int id;
        List<Task> tasks = new ArrayList<>();
        public Stack(int id) {
            this.id = id;
        }
        public List<Task> getTasks() {
            return tasks;
        }
        public void dump() {
            Logger.iformat("Stack #%d, sz=%d", id, tasks.size());
            for (Task task : tasks) {
                Logger.iformat("- Task #%d, sz=%d", task.id, task.activities.size());
                for (Activity activity : task.activities) {
                    Logger.iformat("  - %s", activity.activity);
                }
            }
        }
    }

    public static class Task {
        int id;
        List<Activity> activities = new ArrayList<>();
        public Task(int id) {
            this.id = id;
        }
        public List<Activity> getActivities() {
            return this.activities;
        }
    }

    public static class Activity {
        public final ComponentName activity;

        public Activity(ComponentName activity) {
            this.activity = activity;
        }

    }
    public static Stack getFocusedStack() {
        String[] cmd = new String[] {
                //"/Users/tianxiaogu/Library/Android/sdk/platform-tools/adb", "shell", "dumpsys", "activity", "a"
                "dumpsys", "activity", "a"
        };

        try {
            String output = Utils.getProcessOutput(cmd);
            String line = null;
            Display currentDisplay = null;
            Stack currentStack = null;
            Task currentTask = null;
            Activity currentActivity = null;
            List<Display> displays = new ArrayList<>();
            BufferedReader br = new BufferedReader(new StringReader(output));
            while ((line = br.readLine()) != null) {
                Matcher m = DISPLAY_PATTERN.matcher(line);
                if (m.matches()) {
                    currentDisplay = new Display(Integer.valueOf(m.group(1)));
                    displays.add(currentDisplay);
                    continue;
                }
                m = STACK_PATTERN.matcher(line);
                if (m.matches()) {
                    currentStack = new Stack(Integer.valueOf(m.group(1)));
                    currentDisplay.stacks.add(currentStack);
                    continue;
                }
                m = TASK_PATTERN.matcher(line);
                if (m.matches()) {
                    currentTask = new Task(Integer.valueOf(m.group(1)));
                    currentStack.tasks.add(currentTask);
                    continue;
                }
                m = ACTIVITY_PATTERN.matcher(line);
                if (m.matches()) {
                    String packageName = m.group(1);
                    String className = m.group(2);
                    if (className.startsWith(".")) {
                        className = packageName + className;
                    }
                    ComponentName comp = new ComponentName(packageName, className);
                    currentActivity = new Activity(comp);
                    currentTask.activities.add(currentActivity);
                    continue;
                }
                m = FOCUSED_STACK_PATTERN.matcher(line);
                if (m.find()) {
                    int focusedStackId = Integer.valueOf(m.group(1));
                    currentDisplay.focusedStackId = focusedStackId;
                }
            }
            for (Display d : displays) {
                for (Stack s : d.stacks) {
                    if (s.id == d.focusedStackId) {
                        return s;
                    }
                }
            }
        } catch (IOException e) {
        } catch (InterruptedException e) {
        }
        return null;
    }

    public static void main(String[] args) {
        getFocusedStack();
    }
}

