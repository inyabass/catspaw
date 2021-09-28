package com.inyabass.catspaw.clients;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.inyabass.catspaw.config.ConfigProperties;
import com.inyabass.catspaw.config.ConfigReader;
import com.inyabass.catspaw.logging.Logger;
import com.inyabass.catspaw.util.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AwsS3Client {

    final static Logger logger = new Logger(MethodHandles.lookup().lookupClass());

    private AmazonS3 s3Client = null;
    private String bucketName = null;
    private Regions regions = null;

    public static void main(String[] args) throws Throwable {
        AwsS3Client awsS3Client = new AwsS3Client();
        File file = awsS3Client.getObject("1b48a2eb-066f-471b-b245-08b109c2f917.json.zip");
        int i = 0;
    }

    public AwsS3Client() throws Throwable {
        this.setBucketName(ConfigReader.get(ConfigProperties.AWS_BUCKET_NAME));
        this.setRegions(ConfigReader.get(ConfigProperties.AWS_REGION_NAME));
        this.createClient();
    }

    private void createClient() throws Throwable {
        String accessKey = ConfigReader.get(ConfigProperties.AWS_ACCESS_KEY);
        String secretKey = ConfigReader.get(ConfigProperties.AWS_SECRET_KEY);
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
        this.s3Client = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCreds)).withRegion(this.regions).build();
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public void setRegions(String regionName) throws Throwable {
        try {
            this.regions = Regions.fromName(regionName);
        } catch (Throwable t) {
            logger.error("Invalid Region '" + regionName + "'");
        }
        this.createClient();
    }

    public void putObject(File file) {
        this.s3Client.putObject(this.bucketName, file.getName(), file);
    }

    public void putObject(String fileName, File file) {
        this.s3Client.putObject(this.bucketName, fileName, file);
    }

    public File getObject(String objectName) {
        String fileName = Util.getTemp() + "s3dwn_" + System.currentTimeMillis() + objectName;
        Path filePath = Paths.get(fileName);
        S3Object s3Object = this.s3Client.getObject(this.bucketName, objectName);
        S3ObjectInputStream s3ObjectInputStream = s3Object.getObjectContent();
        try {
            Files.copy(s3ObjectInputStream, filePath);
        } catch (Throwable t) {
            logger.error("Unable to copy S3 Object to File: " + fileName + " : " + t.getMessage());
            return null;
        }
        return filePath.toFile();
    }
}
