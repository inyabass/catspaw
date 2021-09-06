package com.inyabass.catspaw.data;

import java.io.InputStream;

public class TestResponseModel extends TestRequestModel {

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
