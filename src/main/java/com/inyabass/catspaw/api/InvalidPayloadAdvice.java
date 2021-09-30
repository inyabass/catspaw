package com.inyabass.catspaw.api;

import com.inyabass.catspaw.util.Util;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class InvalidPayloadAdvice {

    @ResponseBody
    @ExceptionHandler(InvalidPayloadException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String InvalidPayloadHandler(InvalidPayloadException ex) {
        return Util.buildSpringJsonResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), null);
    }
}
