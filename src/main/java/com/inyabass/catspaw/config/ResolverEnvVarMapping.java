package com.inyabass.catspaw.config;

import com.inyabass.catspaw.util.Util;

import java.util.HashMap;

public class ResolverEnvVarMapping {

    private static HashMap<String, String> unixToWindows = new HashMap<>();
    private static HashMap<String, String> windowsToUnix = new HashMap<>();

    static {
    }

    private static void add(String unixValue, String windowsValue) {
        unixToWindows.put(unixValue, windowsValue);
        windowsToUnix.put(windowsValue, unixValue);
    }

    public static String get(String key) {
        if(Util.isWindows()) {
            if(unixToWindows.containsKey(key)) {
                return unixToWindows.get(key);
            }
        }
        if(Util.isUnix()) {
            if(windowsToUnix.containsKey(key)) {
                return windowsToUnix.get(key);
            }
        }
        return key;
    }

}
