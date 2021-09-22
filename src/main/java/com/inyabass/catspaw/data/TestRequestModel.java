package com.inyabass.catspaw.data;

import java.io.InputStream;
import java.util.List;

public class TestRequestModel extends DataModel {

    private static String GUID_PATH = "guid";
    private static String REQUESTOR_PATH = "requestor";
    private static String TIME_REQUESTED_PATH = "timeRequested";
    private static String TAG_EXPRESSION_PATH = "tagExpression";
    private static String BRANCH_PATH = "branch";
    private static String CONFIGURATION_FILE_PATH = "configurationFile";
    private static String CONFIGURATION_PATH = "configuration";
    private static String PROPERTIES_FILE_PATH = CONFIGURATION_PATH + "[" + REPLACEABLE + "].propertiesFile";
    private static String PROPERTIES_PATH = CONFIGURATION_PATH + "[" + REPLACEABLE + "].properties";
    private static String TARGETS_PATH = "targets";
    private static String TARGET_PATH = TARGETS_PATH + "[" + REPLACEABLE + "].target";
    private static String OPTIONS_PATH = TARGETS_PATH + "[" + REPLACEABLE + "].options";

    public TestRequestModel() {
    }

    public TestRequestModel(StandardModel standardModel) {
        this.load(standardModel.getJson());
    }

    public TestRequestModel(String json) {
        this.load(json);
    }

    public TestRequestModel(InputStream inputStream) {
        this.load(inputStream);
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

    public String getRequestor() {
        return this.getString(REQUESTOR_PATH);
    }

    public String getTimeRequested() {
        return this.getString(TIME_REQUESTED_PATH);
    }

    public void addTimeRequested(String time) {
        this.addString(ROOT, TIME_REQUESTED_PATH, time);
    }

    public void setTimeRequested(String timeRequested) {
        this.setString(TIME_REQUESTED_PATH, timeRequested);
    }

    public String getTagExpression() {
        return this.getString(TAG_EXPRESSION_PATH);
    }

    public String getBranch() {
        return this.getString(BRANCH_PATH);
    }

    public String getConfigurationFile() {
        return this.getString(CONFIGURATION_FILE_PATH);
    }

    public int getConfigurationSize() {
        return this.getSizeOfArray(CONFIGURATION_PATH);
    }

    public String getPropertiesFile(int index) {
        return this.getString(this.replace(PROPERTIES_FILE_PATH, index));
    }

    public List<String> getPropertiesList(int index) {
        return getPropertiesOf(this.replace(PROPERTIES_PATH, index));
    }

    public String getProperty(int index, String propertyName) {
        return getString(this.replace(PROPERTIES_PATH, index) + "." + propertyName);
    }

    public int getTargetsSize() {
        return this.getSizeOfArray(TARGETS_PATH);
    }

    public String getTarget(int index) {
        return this.getString(this.replace(TARGET_PATH, index));
    }

    public List<String> getOptionsList(int index) {
        return getPropertiesOf(this.replace(OPTIONS_PATH, index));
    }
}
