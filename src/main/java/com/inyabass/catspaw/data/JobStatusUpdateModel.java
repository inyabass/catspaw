package com.inyabass.catspaw.data;

import java.io.InputStream;

public class JobStatusUpdateModel extends DataModel {

    private static String GUID_PATH = "guid";
    private static String STATUS_PATH = "status";
    private static String STATUS_MESSAGE_PATH = "statusMessage";
    private static String RESULT_JSON_PATH = "resultJson";
    private static String STDOUT_PATH = "stdout";
    private static String URLS_PATH = "urls";

    public JobStatusUpdateModel() {
    }

    public JobStatusUpdateModel(StandardModel standardModel) {
        this.load(standardModel.getJson());
    }

    public JobStatusUpdateModel(String json) {
        this.load(json);
    }

    public void addResultJson(String json) {
        this.addString(ROOT, RESULT_JSON_PATH, json);
    }

    public void addGuid(String guid) {
        this.addString(ROOT, GUID_PATH, guid);
    }

    public String getGuid() {
        return this.getString(GUID_PATH);
    }

    public void setGuid(String guid) {
        this.setString(GUID_PATH, guid);
    }

    public String getStatus() {
        return this.getString(STATUS_PATH);
    }

    public void setStatus(String value) {
        this.setString(STATUS_PATH, value);
    }

    public void addStatus(String json) {
        this.addString(ROOT, STATUS_PATH, json);
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

    public void setStdout(String stdout) {
        this.setString(STDOUT_PATH, stdout);
    }

    public String getStdout() {
        return this.getString(STDOUT_PATH);
    }

    public void addUrls(String urls) {
        this.addString(ROOT, URLS_PATH, urls);
    }

    public void setUrls(String urls) {
        this.setString(URLS_PATH, urls);
    }

    public String getUrls() {
        return this.getString(URLS_PATH);
    }
}
