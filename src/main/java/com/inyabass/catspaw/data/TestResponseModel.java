package com.inyabass.catspaw.data;

import java.io.InputStream;

public class TestResponseModel extends TestRequestModel {

    private static String STATUS_PATH = "status";
    private static String STATUS_MESSAGE_PATH = "statusMessage";
    private static String RESULT_JSON_PATH = "resultJson";
    private static String STDOUT_PATH = "stdout";

    public TestResponseModel(String json) {
        this.load(json);
    }

    public TestResponseModel(InputStream inputStream) {
        this.load(inputStream);
    }

    public void addResultJson(String json) {
        this.addString(ROOT, RESULT_JSON_PATH, json);
    }

    public String getStatus() {
        return this.getString(STATUS_PATH);
    }

    public void setStatus(String value) {
        this.setString(STATUS_PATH, value);
    }

    public String getStatusMessage() {
        return this.getString(STATUS_MESSAGE_PATH);
    }

    public void setStatusMessage(String value) {
        this.setString(STATUS_MESSAGE_PATH, value);
    }

    public void addStatusMessage(String message) {
        this.addString(ROOT, STATUS_MESSAGE_PATH, message);
    }

    public String getResultJson() {
        return this.getString(RESULT_JSON_PATH);
    }

    public void setResultJson(String json) {
        this.setString(RESULT_JSON_PATH, json);
    }

    public void addStdout(String stdout) {
        this.addString(ROOT, STDOUT_PATH, stdout);
    }

    public String getStdout() {
        return this.getString(STDOUT_PATH);
    }

    public void setStdout(String stdout) {
        this.setString(STDOUT_PATH, stdout);
    }
}
