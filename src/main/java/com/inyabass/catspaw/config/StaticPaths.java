package com.inyabass.catspaw.config;

public class StaticPaths {

    private static final String PACKAGE = "inyabass";
    //
    // Paths Relative to the Project Root
    //
    public static final String PROJECT_ROOT = "src/main/resources";
    //
    // Paths Relative to the Project Root Resources (used by .getResourceAsStream() typically
    //
    public static final String PROJECT_RESOURCE_ROOT = "com/" + PACKAGE + "/catspaw";
    public static final String FREEMARKER_TEMPLATES = PROJECT_RESOURCE_ROOT + "/freemarkertemplates";
}
