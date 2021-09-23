package com.inyabass.catspaw.config;

import com.inyabass.catspaw.logging.Logger;
import org.junit.Assert;

import java.io.File;
import java.io.FileInputStream;
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

    public static final String CATSPAW_CONFIG_ENV_VARIABLE = "CATSPAW_CONFIG";
    public static final String CATSPAW_CONFIG_JVM_PROPERTY = "catspaw.config";

    private static Properties properties = null;
    private static ArrayList<String> paths = new ArrayList<>();
    private static boolean resolveEnvironmentVariables = true;

    public static void main(String[] args) throws Throwable {
        String s = get("bootstrap.servers");
        int i = 0;
    }

    public static void ConfigReader() throws Throwable {
        if(properties != null) {
            return;
        }
        String fileNames = null;
        logger.info("Reading Environment Variable: " + ConfigReader.CATSPAW_CONFIG_ENV_VARIABLE + " to get Config File Names");
        fileNames = System.getenv(CATSPAW_CONFIG_ENV_VARIABLE);
        if(fileNames==null||fileNames.equals("")) {
            logger.info("Not found so Reading Java Property: " + ConfigReader.CATSPAW_CONFIG_JVM_PROPERTY + " to get Config File Names");
            fileNames = System.getProperty(ConfigReader.CATSPAW_CONFIG_JVM_PROPERTY);
        }
        if(fileNames == null) {
            Assert.fail("No Configuration File(s) could be found");
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
                ConfigReader.paths.add(parts[i].trim());
            }
        } else {
            logger.debug("Single Configuration File Found");
            ConfigReader.paths.add(fileNames.trim());
        }
        logger.debug("Loading first config file from: " + ConfigReader.paths.get(0).trim());
        InputStream inputStream = new FileInputStream(new File(ConfigReader.paths.get(0).trim()));
        Assert.assertNotNull("Invalid Configuration file: " + ConfigReader.paths.get(0).trim(), inputStream);
        ConfigReader.properties.load(inputStream);
        if(ConfigReader.paths.size() > 1) {
            for(int i = 1;i < paths.size(); i++) {
                Properties propertiesToMerge = new Properties();
                logger.debug("Loading Other Config File: " + ConfigReader.paths.get(i).trim());
                inputStream = inputStream = new FileInputStream(new File(ConfigReader.paths.get(i).trim()));
                Assert.assertNotNull("Invalid Configuration file: " + ConfigReader.paths.get(i).trim(), inputStream);
                propertiesToMerge.load(inputStream);
                ConfigReader.mergeInWithOverride(propertiesToMerge);
            }
        }
        try {
            String crev = ConfigReader.get("configreader.resolve.environment.variables");
            ConfigReader.setResolveEnvironmentVariables(Boolean.parseBoolean(crev));
        } catch (Throwable t) {
            // Ignore
        }
    }

    public static boolean exists(String property) throws Throwable {
        if(ConfigReader.resolveEnvironmentVariables&&System.getenv(property.toUpperCase().replaceAll("\\.", "_"))!=null) {
            return true;
        }
        return existsInternal(property);
    }

    private static boolean existsInternal(String property) throws Throwable {
        ConfigReader.checkAndLoadProperties();
        return ConfigReader.properties.containsKey(property);
    }

    public static String get(String property) throws Throwable {
        if(ConfigReader.resolveEnvironmentVariables&&System.getenv(property.toUpperCase().replaceAll("\\.", "_"))!=null) {
            String value = System.getenv(property.toUpperCase().replaceAll("\\.", "_"));
            if(value.startsWith("$e:")) {
                return Resolver.resolve(decode(value.substring(3)));
            } else {
                return Resolver.resolve(value);
            }
        }
        ConfigReader.checkAndLoadProperties();
        if(ConfigReader.existsInternal(property)) {
            String value = (String) ConfigReader.properties.get(property);
            if(value.startsWith("$e:")) {
                return Resolver.resolve(decode(value.substring(3)));
            } else {
                return Resolver.resolve(value);
            }
        }
        Assert.fail("Property '" + property + "' does not exist in Config");
        return null;
    }

    public static void set(String property, String value) throws Throwable {
        ConfigReader.checkAndLoadProperties();
        if(ConfigReader.existsInternal(property)) {
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

    public static void setResolveEnvironmentVariables(boolean value) {
        ConfigReader.resolveEnvironmentVariables = value;
    }
}
