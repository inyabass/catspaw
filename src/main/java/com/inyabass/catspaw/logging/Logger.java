package com.inyabass.catspaw.logging;

import org.slf4j.LoggerFactory;

public class Logger {

    Class clazz = null;
    org.slf4j.Logger logger = null;

    public Logger(Class clazz) {
       this.clazz = clazz;
       this.logger = LoggerFactory.getLogger(this.clazz);
    }

    public void info(String message) {
        logger.info(message);
    }

    public void info(String id, String message) {
        logger.info("[" + id + "] " + message);
    }

    public void debug(String message) {
        logger.debug(message);
    }

    public void debug(String id, String message) {
        logger.debug("[" + id + "] " + message);
    }

    public void trace(String message) {
        logger.trace(message);
    }

    public void trace(String id, String message) {
        logger.trace("[" + id + "] " + message);
    }

    public void error(String message) {
        logger.error(message);
    }

    public void error(String id, String message) {
        logger.error("[" + id + "] " + message);
    }

    public void warn(String message) {
        logger.warn(message);
    }

    public void warn(String id, String message) {
        logger.warn("[" + id + "] " + message);
    }
}
