package com.inyabass.catspaw.api;

public class InvalidPayloadException extends RuntimeException {

    public InvalidPayloadException(String message) {
        super(message);
    }
}
