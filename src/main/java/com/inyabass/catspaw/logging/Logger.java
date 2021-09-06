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
        if(this.logger.isInfoEnabled()) {
            logger.info(message);
        }
    }

    public void info(String id, String message) {
        if(this.logger.isInfoEnabled()) {
            logger.info("[" + id + "] " + message);
        }
    }

    public void debug(String message) {
        if(this.logger.isDebugEnabled()) {
            logger.info(message);
        }
    }

    public void debug(String id, String message) {
        if(this.logger.isDebugEnabled()) {
            logger.debug("[" + id + "] " + message);
        }
    }

    public void trace(String message) {
        if(this.logger.isTraceEnabled()) {
            logger.info(message);
        }
    }

    public void trace(String id, String message) {
        if(this.logger.isTraceEnabled()) {
            logger.trace("[" + id + "] " + message);
        }
    }

    public void error(String message) {
        if(this.logger.isErrorEnabled()) {
            logger.info(message);
        }
    }

    public void error(String id, String message) {
        if(this.logger.isErrorEnabled()) {
            logger.error("[" + id + "] " + message);
        }
    }

    public void warn(String message) {
        if(this.logger.isWarnEnabled()) {
            logger.info(message);
        }
    }

    public void warn(String id, String message) {
        if(this.logger.isWarnEnabled()) {
            logger.warn("[" + id + "] " + message);
        }
    }
}
