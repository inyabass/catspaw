package com.inyabass.catspaw.clients;

import com.inyabass.catspaw.config.ConfigProperties;
import com.inyabass.catspaw.config.ConfigReader;
import com.inyabass.catspaw.logging.Logger;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;

public class Couchdb {

    final static Logger logger = new Logger(MethodHandles.lookup().lookupClass());

    private String serverUrl = null;
    private RequestSpecification requestSpecification = null;
    private Response response = null;

    public static void main(String[] args) throws Throwable {
        Couchdb couchdb = new Couchdb(ConfigReader.get(ConfigProperties.COUCHDB_SERVER));
        couchdb.get("2e2a18c3-8159-475b-ba54-aee1d6e6f302");
        System.out.println(couchdb.getBodyAsJsonString());
        int i = 0;
    }

    public Couchdb(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    private RequestSpecification getRequestSpecification() {
        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Content-Type", "application/json");
        requestSpecification.header("Accept", "application/json");
        return requestSpecification;
    }

    public void get(String id) throws Throwable {
        this.requestSpecification = this.getRequestSpecification();
        this.response = this.requestSpecification.get(new URL(this.serverUrl + id));
    }

    public void post(String id, String body) throws Throwable {
        this.requestSpecification = this.getRequestSpecification();
        this.requestSpecification.body(body);
        this.response = this.requestSpecification.post(new URL(this.serverUrl + id));
        int i = 0;
    }

    public void put(String id, String body) throws Throwable {
        this.requestSpecification = this.getRequestSpecification();
        this.requestSpecification.body(body);
        this.response = this.requestSpecification.put(new URL(this.serverUrl + id));
    }

    public int getHttpStatus() {
        return this.response.getStatusCode();
    }

    public String getHttpStatusMessage() {
        return this.response.getStatusLine();
    }

    public InputStream getBodyAsInputStream() {
        return this.response.getBody().asInputStream();
    }

    public String getBodyAsJsonString() {
        return this.response.getBody().asString();
    }
}
