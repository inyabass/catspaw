package com.inyabass.catspaw.util;

import com.inyabass.catspaw.clients.AwsS3Client;
import com.inyabass.catspaw.config.ConfigProperties;
import com.inyabass.catspaw.config.ConfigReader;
import com.inyabass.catspaw.data.TestRequestModel;
import com.inyabass.catspaw.logging.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Util {

    private final static Logger logger = new Logger(MethodHandles.lookup().lookupClass());

    public final static SimpleDateFormat STANDARD_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd-HH:mm:ss");
    public final static SimpleDateFormat SPRING_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    public static void main(String[] args) throws Throwable {
        File unzippedFile = unzipInPlace(new File("src/main/resources/typical.json.zip"));
        int i = 0;
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
        String unZippedFileName = fileToUnzip.getAbsolutePath();
        if(!unZippedFileName.endsWith(".zip")) {
            logger.error("File " + unZippedFileName + " did not have .zip at the end of the name");
            return null;
        }
        unZippedFileName = unZippedFileName.substring(0, unZippedFileName.length() - 4);
        Path outputPath = Paths.get(unZippedFileName);
        try {
            Files.delete(outputPath);
        } catch (Throwable t) {
            logger.warn("Could not delete " + unZippedFileName + " : " + t.getMessage());
        }
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(fileToUnzip);
        } catch (Throwable t) {
            logger.error("Unable to create FileInputStream on zipfile :" + fileToUnzip.getName());
            return null;
        }
        ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
        try {
            int available = zipInputStream.available();
            if(available!=1) {
                logger.error("File contained more than 1 file :" + fileToUnzip.getName());
                return null;
            }
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            Files.copy(zipInputStream, outputPath);
        } catch (Throwable t) {
            logger.error("Unable to Unzip file " + fileToUnzip.getName() + "into " + unZippedFileName + " : " + t.getMessage());
            return null;
        }
        return outputPath.toFile();
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

    public static String getTemp() {
        String tempDir = System.getProperty("java.io.tmpdir");
        if(Util.isWindows()) {
            if(!tempDir.endsWith("\\")) {
                tempDir += "\\";
            }
        } else {
            if(!tempDir.endsWith("/")) {
                tempDir += "/";
            }
        }
        return tempDir;
    }

    public static void writeFileToS3(File file, String reference) throws Throwable {
        if(!file.exists()) {
            logger.warn(reference, "Could not find file to write to S3: " + file.getName());
            throw new FileNotFoundException("Could not find file to write to S3: " + file.getName());
        }
        AwsS3Client awsS3Client = null;
        try {
            awsS3Client = new AwsS3Client();
        } catch (Throwable t) {
            logger.error(reference, "Unable to create Amazon S3 Client");
            throw t;
        }
        awsS3Client.putObject(file);
    }

    public static void cleanUpTempFiles(List<String> tempFiles, String reference) {
        logger.info(reference, "Cleaning up Temporary Files");
        for (String file : tempFiles) {
            if (Files.exists(Paths.get(file))) {
                try {
                    Files.delete(Paths.get(file));
                    logger.info(reference, "File " + file + " deleted");
                } catch (Throwable t) {
                    logger.warn(reference, "Unable to delete " + file + " : " + t.getMessage());
                }
            }
        }
    }

    public static void overrideParameters(TestRequestModel testRequestModel, String clonedDirectory, String reference) {
        String configFileDirectory = null;
        try {
            configFileDirectory = ConfigReader.get(ConfigProperties.CONFIG_DIRECTORY);
        } catch (Throwable t) {
            logger.error(reference, "Unable to determine repo config directory: " + t.getMessage());
            return;
        }
        configFileDirectory = Util.convertPath(configFileDirectory);
        int fileEntries = 0;
        try {
            fileEntries = testRequestModel.getConfigurationSize();
        } catch (Throwable t) {
            logger.warn(reference, "Could not get the number of Configuration File Entries");
            return;
        }
        if(fileEntries==0) {
            return;
        }
        for(int i = 0;i<fileEntries;i++) {
            String propertiesFile = null;
            try {
                propertiesFile = testRequestModel.getPropertiesFile(i);
            } catch (Throwable t) {
                logger.error(reference, "No Properties File Specified");
                continue;
            }
            String propertiesFileFull = clonedDirectory + ScriptProcessor.fs + configFileDirectory + ScriptProcessor.fs + propertiesFile;
            if(!Files.exists(Paths.get(propertiesFileFull))) {
                logger.warn(reference, "Properties File does not exist in repo: " + propertiesFile);
                continue;
            }
            int itemEntries = 0;
            List<String> propertiesList = null;
            try {
                propertiesList = testRequestModel.getPropertiesList(i);
                itemEntries = propertiesList.size();
            } catch (Throwable t) {
                logger.warn(reference, "For file " + propertiesFile + " no config items were specified");
                continue;
            }
            Properties properties = new Properties();
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(new File(propertiesFileFull));
                if(fileInputStream==null) {
                    logger.warn(reference, "Unable to get a FileInputStream for " + propertiesFile);
                    continue;
                }
                properties.load(fileInputStream);
                fileInputStream.close();
            } catch (Throwable t) {
                logger.warn(reference, "Unable to read or parse Configuration file " + propertiesFile + ": " + t.getMessage());
                continue;
            }
            for(String propertyName: propertiesList) {
                String propertyValue = testRequestModel.getProperty(i, propertyName);
                if(properties.containsKey(propertyName)) {
                    properties.setProperty(propertyName, propertyValue);
                } else {
                    properties.put(propertyName, propertyValue);
                }
            }
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(new File(propertiesFileFull));
                if(fileOutputStream==null) {
                    logger.error(reference, "Unable to create FileOutputStream to rewrite file " + propertiesFile);
                    continue;
                }
                properties.store(fileOutputStream, "Updated");
                fileOutputStream.close();
            } catch (Throwable t) {
                logger.warn(reference, "Unable to rewrite properties file " + propertiesFile + " :" + t.getMessage());
            }
        }
    }

    public static Set<File> getFilesInDirectory(String directoryName, int level) throws Throwable {
        Stream<Path> pathStream = Files.walk(Paths.get(directoryName), level);
        Set<File> returnValue = new HashSet<>();
        pathStream.forEach(path -> {
            if(!Files.isDirectory(path)) {
                returnValue.add(path.toFile());
            }
        });
        return returnValue;
    }
}