package com.inyabass.catspaw.data;

import com.inyabass.catspaw.config.ConfigReader;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public enum StandardModel {

    TEST_REQUEST("test-request.json"),
    JOB_STATUS_UPDATE("status-update.json");

    private String fileName = null;

    StandardModel(String fileName) {
        this.fileName = ConfigReader.FILE_BASE + "json/" + fileName;
    }

    public String getFilename() {
        if(this.fileName!=null&&this.fileName.length()>0) {
            return this.fileName;
        } else {
            return null;
        }
    }

    public String getJson() {
        String fileName = this.getFilename();
        if(fileName==null) {
            return null;
        }
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
        if(inputStream==null) {
            return null;
        }
        try {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (Throwable t) {
            return null;
        }
    }
}
