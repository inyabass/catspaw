package com.inyabass.catspaw.listeners;

import com.inyabass.catspaw.api.InvalidPayloadException;
import com.inyabass.catspaw.config.ConfigProperties;
import com.inyabass.catspaw.config.ConfigReader;
import com.inyabass.catspaw.data.JobStatusUpdateModel;
import com.inyabass.catspaw.data.StandardModel;
import com.inyabass.catspaw.logging.Logger;
import com.inyabass.catspaw.sqldata.SqlDataModel;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.http.HttpHeaders;
import org.springframework.http.HttpStatus;

public class ListenerHelper {

    public final static String JOB_UPDATE_ENDPOINT = "/jobStatusUpdate";

    public static void jobStatusUpdate(Logger logger, String guid, String status, String statusMessage, String resultJson, String stdout, String urls) throws Throwable {
        logger.info(guid, "Updating cats.jobs table with Status '" + status + "'");
        JobStatusUpdateModel jobStatusUpdateModel = new JobStatusUpdateModel(StandardModel.JOB_STATUS_UPDATE);
        jobStatusUpdateModel.setGuid(guid);
        jobStatusUpdateModel.setStatus(status);
        jobStatusUpdateModel.setStatusMessage(statusMessage);
        if(resultJson!=null) {
            jobStatusUpdateModel.addResultJson(resultJson);
        }
        if(stdout!=null) {
            jobStatusUpdateModel.addStdout(stdout);
        }
        if(urls!=null) {
            jobStatusUpdateModel.addUrls(urls);
        }
        String apiHost = ConfigReader.get(ConfigProperties.API_HOST);
        String apiPath = apiHost + JOB_UPDATE_ENDPOINT;
        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header(HttpHeaders.CONTENT_TYPE, "application/json");
        requestSpecification.header(HttpHeaders.ACCEPT, "application/json");
        requestSpecification.body(jobStatusUpdateModel.export());
        Response response = requestSpecification.post(apiPath);
        if(response.getStatusCode()!= HttpStatus.OK.value()) {
            logger.error(guid, "Unable to Update cats.jobs table : " + response.getStatusCode());
        } else {
            logger.info(guid, "Table cats.jobs successfully updated");
        }
    }
}
