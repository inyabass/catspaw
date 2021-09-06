package com.inyabass.catspaw.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class Util {

    public final static SimpleDateFormat STANDARD_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd-HH:mm:ss");
    public final static SimpleDateFormat SPRING_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    public static void main(String[] args) {
        System.out.println(getGuid());
        System.out.println(getGuid());
        System.out.println(getGuid());
        System.out.println(getGuid());
    }

    public static boolean isValidTimeStamp(String timeStamp) {
        try {
            STANDARD_DATE_FORMAT.parse(timeStamp);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static String getStandardTimeStampNow() {
        return STANDARD_DATE_FORMAT.format(new Date());
    }

    public static String getSpringTimeStampNow() {
        return SPRING_DATE_FORMAT.format(new Date());
    }

    public static String getGuid() {
        return UUID.randomUUID().toString();
    }

    public static String buildSpringJsonResponse(int status, String error) {
        return "{\"timestamp\":" + Util.getSpringTimeStampNow() + "\",\"status\":"+ status + ",\"error\":\"" + error + "\",\"path\":\"/\"}";
    }
}