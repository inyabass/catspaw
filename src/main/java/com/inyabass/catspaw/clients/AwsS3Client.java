package com.inyabass.catspaw.clients;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.inyabass.catspaw.config.ConfigProperties;
import com.inyabass.catspaw.config.ConfigReader;
import com.inyabass.catspaw.logging.Logger;

import java.io.File;
import java.lang.invoke.MethodHandles;

public class AwsS3Client {

    final static Logger logger = new Logger(MethodHandles.lookup().lookupClass());

    private AmazonS3 s3Client = null;

    public AwsS3Client() throws Throwable {
        String accessKey = ConfigReader.get(ConfigProperties.AWS_ACCESS_KEY);
        String secretKey = ConfigReader.get(ConfigProperties.AWS_SECRET_KEY);
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
    }

    public void store(File file) {

    }
}
