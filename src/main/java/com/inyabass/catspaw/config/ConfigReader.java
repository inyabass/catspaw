package com.inyabass.catspaw.config;

import com.inyabass.catspaw.logging.Logger;
import org.junit.Assert;

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Properties;
import java.util.Set;

public class ConfigReader {

    final static Logger logger = new Logger(MethodHandles.lookup().lookupClass());

    public static final String FILE_BASE = "com/inyabass/catspaw/";

    private static final String ENVIRONMENT_VARIABLE = "catspaw.config";

    private static Properties properties = null;
    private static ArrayList<String> paths = new ArrayList<>();

    public static void main(String[] args) throws Throwable {
        String encoded = encode("abc123");
        System.out.println(encoded);
        String decoded = decode(encoded);
        System.out.println(decoded);
    }

    public static void ConfigReader() throws Throwable {
        if(properties != null) {
            return;
        }
        String fileNames = System.getProperty(ConfigReader.ENVIRONMENT_VARIABLE);
        logger.debug("Reading Environment Variable: " + ConfigReader.ENVIRONMENT_VARIABLE + " to get Config File Names");
        if(fileNames == null) {
            Assert.fail("No Configuration File(s) Specified in Environment Variable '" + ConfigReader.ENVIRONMENT_VARIABLE + "'");
        }
        ConfigReader.load(fileNames);
    }

    public static void ConfigReader(String fileNames) throws Throwable {
        if(properties != null) {
            return;
        }
        logger.debug("Loading Configuration from File(s): " + fileNames);
        ConfigReader.load(fileNames);
    }

    private static void load(String fileNames) throws Throwable {
        ConfigReader.properties = new Properties();
        if(fileNames.contains(",")) {
            logger.debug("Multiple Configuration Files Found");
            String[] parts = fileNames.trim().split(",");
            for(int i = 0; i < parts.length; i++) {
                ConfigReader.paths.add(FILE_BASE + parts[i].trim());
            }
        } else {
            logger.debug("Single Configuration File Found");
            ConfigReader.paths.add(FILE_BASE + fileNames.trim());
        }
        logger.debug("Loading first config file from: " + ConfigReader.paths.get(0).trim());
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(ConfigReader.paths.get(0).trim());
        Assert.assertNotNull("Invalid Configuration file: " + ConfigReader.paths.get(0).trim(), inputStream);
        ConfigReader.properties.load(inputStream);
        if(ConfigReader.paths.size() > 1) {
            for(int i = 1;i < paths.size(); i++) {
                Properties propertiesToMerge = new Properties();
                logger.debug("Loading Other Config File: " + ConfigReader.paths.get(i).trim());
                inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(ConfigReader.paths.get(i).trim());
                Assert.assertNotNull("Invalid Configuration file: " + ConfigReader.paths.get(i).trim(), inputStream);
                propertiesToMerge.load(inputStream);
                ConfigReader.mergeInWithOverride(propertiesToMerge);
            }
        }
    }

    public static boolean exists(String property) throws Throwable {
        ConfigReader.checkAndLoadProperties();
        return ConfigReader.properties.containsKey(property);
    }

    public static String get(String property) throws Throwable {
        ConfigReader.checkAndLoadProperties();
        if(ConfigReader.exists(property)) {
            String value = (String) ConfigReader.properties.get(property);
            if(value.startsWith("$e:")) {
                return decode(value.substring(3));
            } else {
                return value;
            }
        }
        Assert.fail("Property '" + property + "' does not exist in Config");
        return null;
    }

    public static void set(String property, String value) throws Throwable {
        ConfigReader.checkAndLoadProperties();
        if(ConfigReader.exists(property)) {
            ConfigReader.properties.replace(property, value);
        } else {
            ConfigReader.properties.put(property, value);
        }
    }

    public static void setEncoded(String property, String value) throws Throwable {
        set(property, "$e:" + encode(value));
    }

    public static void mergeInWithOverride(Properties mergeProperties) throws Throwable {
        if(mergeProperties.size() == 0) {
            return;
        }
        Set keys = mergeProperties.keySet();
        for(Object key: keys) {
            ConfigReader.set((String) key, (String) mergeProperties.get(key));
        }
    }

    private static void checkAndLoadProperties() throws Throwable {
        if(ConfigReader.properties == null) {
            ConfigReader.ConfigReader();
        }
    }

    private static String encode(String decoded) {
        Base64.Encoder encoder = Base64.getEncoder();
        return new String(encoder.encode(decoded.getBytes()), StandardCharsets.UTF_8);
    }

    private static String decode(String encoded) {
        Base64.Decoder decoder = Base64.getDecoder();
        return new String(decoder.decode(encoded.getBytes()), StandardCharsets.UTF_8);
    }
}
