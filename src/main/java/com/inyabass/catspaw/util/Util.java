package com.inyabass.catspaw.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class Util {

    public final static SimpleDateFormat STANDARD_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd-HH:mm:ss");

    public static boolean isValidTimeStamp(String timeStamp) {
        try {
            STANDARD_DATE_FORMAT.parse(timeStamp);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static String getTimeStampNow() {
        return STANDARD_DATE_FORMAT.format(new Date());
    }

    public static String getGuid() {
        return UUID.randomUUID().toString();
    }
}
