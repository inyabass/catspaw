package com.inyabass.catspaw.clients;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.inyabass.catspaw.config.ConfigProperties;
import com.inyabass.catspaw.config.ConfigReader;
import com.inyabass.catspaw.logging.Logger;

import java.io.File;
import java.lang.invoke.MethodHandles;

public class AwsS3Client {

    final static Logger logger = new Logger(MethodHandles.lookup().lookupClass());

    private AmazonS3 s3Client = null;
    private String bucketName = null;
    private Regions regions = null;

    public static void main(String[] args) throws Throwable {
        AwsS3Client awsS3Client = new AwsS3Client();
        awsS3Client.setRegions(Regions.EU_WEST_1.getName());
        int i = 1;
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

    public void putObject(String folder, File file) {
        this.s3Client.putObject(this.bucketName, folder + "/" + file.getName(), file);
    }
}
