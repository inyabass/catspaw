package com.inyabass.catspaw.util;

import com.inyabass.catspaw.logging.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Util {

    private final static Logger logger = new Logger(MethodHandles.lookup().lookupClass());

    public final static SimpleDateFormat STANDARD_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd-HH:mm:ss");
    public final static SimpleDateFormat SPRING_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    public static void main(String[] args) {
        File file = new File("src/main/resources/logback.xml");
        File zippedFile = zipInPlace(file);
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

    public static File zipInPlace(File fileToZip) {
        if(!fileToZip.exists()) {
            logger.error("File to zip " + fileToZip.getName() + " does not exist");
            return null;
        }
        String inputFileName = fileToZip.getAbsolutePath();
        String zipFileName = inputFileName + ".zip";
        Path zipFilePath = Paths.get(zipFileName);
        if(Files.exists(zipFilePath)) {
            try {
                Files.delete(zipFilePath);
            } catch (Throwable t) {
                logger.error("Unable to delete Zipfile " + zipFilePath.toFile().getAbsolutePath() + " " + t.getMessage());
                return null;
            }
        }
        File zipFile = null;
        try {
            zipFile = Files.createFile(zipFilePath).toFile();
        } catch (Throwable t) {
            logger.error("Unable to create Zipfile " + zipFilePath.toFile().getAbsolutePath() + " " + t.getMessage());
            return null;
        }
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(zipFileName);
        } catch (Throwable t) {
            logger.error("Unable to create FileOutputStream on Zipfile " + zipFileName + " " + t.getMessage());
            return null;
        }
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(fileToZip);
        } catch (Throwable t) {
            logger.error("Unable to create FileInputStream on Source File " + inputFileName + " " + t.getMessage());
            return null;
        }
        ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
        try {
            zipOutputStream.putNextEntry(zipEntry);
        } catch (Throwable t) {
            logger.error("Unable to put Zip Entry: " + t.getMessage());
            return null;
        }
        try {
            byte[] data = new byte[1024];
            int length = 0;
            while ((length = fileInputStream.read(data)) >= 0) {
                zipOutputStream.write(data, 0, length);
            }
        } catch (Throwable t) {
            logger.info("Unable to add data to Zip Entry: " + t.getMessage());
            return null;
        }
        try {
            zipOutputStream.close();
        } catch (Throwable t) {
            logger.error("Unable to close ZipOutputStream: " + t.getMessage());
            return null;
        }
        try {
            fileInputStream.close();
        } catch (Throwable t) {
            logger.error("Unable to close FileInputStream: " + t.getMessage());
            return null;
        }
        try {
            fileOutputStream.close();
        } catch (Throwable t) {
            logger.error("Unable to close FileOutputStream: " + t.getMessage());
            return null;
        }
        logger.debug("File " + inputFileName + " zipped as " + zipFileName);
        return zipFile;
    }

    public static File unzipInPlace(File fileToUnzip) {
        return null;
    }

    public static boolean isWindows() {
        if(System.getProperty("os.name").toLowerCase().contains("windows")) {
            return true;
        }
        return false;
    }

    public static boolean isUnix() {
        return !isWindows();
    }

    public static String convertPath(String fromPath) {
        if(isUnix()) {
            return fromPath.replaceAll("\\\\", "/");
        }
        return fromPath.replaceAll("/", "\\\\");
    }
}