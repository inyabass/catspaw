package com.inyabass.catspaw.config;

import com.inyabass.catspaw.logging.Logger;
import com.inyabass.catspaw.util.Util;

import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Resolver {

    final static Logger logger = new Logger(MethodHandles.lookup().lookupClass());

    public static final String CONFIG_PREFIX = "$";
    public static final String ENV_VAR_PREFIX = "#";
    public static final String REGEX = "([\\" + ENV_VAR_PREFIX + "|\\" + CONFIG_PREFIX + "]\\{[A-Za-z0-9_-]+\\})";

    public static String resolve(String toResolve) {
        String tempResolve = toResolve;
        if(tempResolve.startsWith("\"")&&tempResolve.endsWith("\"")) {
            tempResolve = tempResolve.substring(1, tempResolve.length() - 1);
        }
        Pattern pattern = Pattern.compile(REGEX);
        Matcher matcher = null;
        boolean working = true;
        ArrayList<String> unresolveable = new ArrayList<>();
        while(working) {
            int numResolutions = 0;
            matcher = pattern.matcher(tempResolve);
            if(matcher.find()) {
                String token = matcher.group();
                if(!unresolveable.contains(token)) {
                    String resolvedToken = Resolver.resolveToken(token);
                    if (resolvedToken.equals(token)) {
                        unresolveable.add(token);
                    } else {
                        tempResolve = tempResolve.replace(token, resolvedToken);
                        numResolutions++;
                    }
                }
            } else {
                working = false;
            }
            if(numResolutions==0) {
                working = false;
            }
        }
        return tempResolve;
    }

    public static boolean isSpecialCharacter(String value) {
        if(value.length() > 1) {
            value = value.substring(0, 1);
        }
        if(value.equals(Resolver.CONFIG_PREFIX) || value.equals(Resolver.ENV_VAR_PREFIX)) {
            return true;
        }
        return false;
    }

    private static String resolveToken(String key) {
        String token = key.substring(2, key.length() - 1);
        switch (key.substring(0, 1)) {
            case Resolver.CONFIG_PREFIX: {
                return Resolver.resolveConfig(token);
            }
            case Resolver.ENV_VAR_PREFIX: {
                return Resolver.resolveEnvironmentVariable(token);
            }
        }
        return key;
    }

    private static String resolveConfig(String key) {
        try {
            ConfigReader.ConfigReader();
            if (ConfigReader.exists(key)) {
                logger.debug("Resolved embedded Config Item: '" + key + "' to '" + ConfigReader.get(key) + "'");
                return ConfigReader.get(key);
            } else {
                return Resolver.CONFIG_PREFIX + "{" + key + "}";
            }
        } catch (Throwable throwable) {
            logger.error("Unable to resolve '" + key + "': " + throwable.getMessage());
        }
        return Resolver.CONFIG_PREFIX + "{" + key + "}";
    }

    private static String resolveEnvironmentVariable(String key) {
        if(key.toUpperCase().equals("HOSTNAME")||key.toUpperCase().equals("COMPUTERNAME")) {
            try {
                return InetAddress.getLocalHost().getHostName();
            } catch (Throwable t) {
                return Resolver.ENV_VAR_PREFIX + "{" + key + "}";
            }
        }
        key = ResolverEnvVarMapping.get(key);
        if(System.getenv(key)!=null) {
            logger.debug("Resolved embedded Environment Variable: '" + key + "' to '" + System.getenv(key));
            return System.getenv(key);
        }
        return Resolver.ENV_VAR_PREFIX + "{" + key + "}";
    }
}
