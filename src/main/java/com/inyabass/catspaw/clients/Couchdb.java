package com.inyabass.catspaw.clients;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.io.InputStream;
import java.net.URL;

public class Couchdb {

    private String serverUrl = null;
    private RequestSpecification requestSpecification = null;
    private Response response = null;

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

    public void post(String body, String id) throws Throwable {
        this.requestSpecification = this.getRequestSpecification();
        this.requestSpecification.body(body);
        this.response = this.requestSpecification.post(new URL(this.serverUrl + id));
    }

    public int getHttpStatus() {
        return this.response.getStatusCode();
    }

    public InputStream getBodyAsInputStream() {
        return this.response.getBody().asInputStream();
    }

    public String getBodyAsJsonString() {
        return this.response.getBody().asString();
    }
}
