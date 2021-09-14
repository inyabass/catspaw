package com.inyabass.catspaw.config;

import org.junit.Assert;

public class ConfigWriter {

    public static void main(String[] args) {
        Assert.assertTrue("Must Provide Node ID and config file to be written", args.length==2);
    }
}
