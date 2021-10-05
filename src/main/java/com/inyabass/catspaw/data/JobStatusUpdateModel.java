package com.inyabass.catspaw.data;

import java.io.InputStream;

public class JobStatusUpdateModel extends DataModel {

    private static String GUID_PATH = "guid";
    private static String STATUS_PATH = "status";
    private static String STATUS_MESSAGE_PATH = "statusMessage";

    public JobStatusUpdateModel() {
    }

    public JobStatusUpdateModel(StandardModel standardModel) {
        this.load(standardModel.getJson());
    }

    public JobStatusUpdateModel(String json) {
        this.load(json);
    }

    public JobStatusUpdateModel(InputStream inputStream) {
        this.load(inputStream);
    }

    public String getGuid() {
        return this.getString(GUID_PATH);
    }

    public String getStatus() {
        return this.getString(STATUS_PATH);
    }

    public String getStatusMessage() {
        return this.getString(STATUS_MESSAGE_PATH);
    }
}
