package com.inyabass.catspaw.listeners;

import com.inyabass.catspaw.clients.KafkaReader;
import com.inyabass.catspaw.clients.KafkaWriter;
import com.inyabass.catspaw.config.ConfigProperties;
import com.inyabass.catspaw.config.ConfigReader;
import com.inyabass.catspaw.data.TestRequestModel;
import com.inyabass.catspaw.data.TestResponseModel;
import com.inyabass.catspaw.logging.Logger;
import com.inyabass.catspaw.util.ScriptProcessor;
import com.inyabass.catspaw.util.Util;
import org.apache.commons.io.FileUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class TestExecutor implements Listener {

    final static Logger logger = new Logger(MethodHandles.lookup().lookupClass());

    private String guid = null;
    private String inputJson = null;
    private KafkaWriter kafkaWriter = new KafkaWriter();

    public TestExecutor() throws Throwable {
        logger.info("TestExecutor starting");
        ConfigReader.ConfigReader();
    }

    public static void main(String[] args) throws Throwable {
        TestExecutor testExecutor = new TestExecutor();
        String topic = ConfigProperties.TEST_REQUEST_TOPIC;
        String groupId = ConfigReader.get(ConfigProperties.TEST_REQUEST_LISTENER_GROUP_ID);
        Duration duration = Duration.ofSeconds(Integer.parseInt(ConfigReader.get(ConfigProperties.POLL_DURATION)));
        KafkaReader kafkaReader = new KafkaReader(testExecutor, topic, groupId, duration);
        kafkaReader.pollLoop();
    }

    public void processRecord(ConsumerRecord<String, String> consumerRecord) {
        this.guid = consumerRecord.key();
        this.inputJson = consumerRecord.value();
        logger.info(this.guid, "Start Processing " + this.inputJson);
        //
        // Parse Kafka Test Request json payload
        //
        logger.info(this.guid, "Parse JSON Payload");
        TestRequestModel testRequestModel = null;
        try {
            testRequestModel = new TestRequestModel(this.inputJson);
        } catch (Throwable t) {
            this.abendMessage(t, "Unable to Parse JSON");
            return;
        }
        //
        // Create Test Response Model from Test Request Model
        //
        TestResponseModel testResponseModel = new TestResponseModel(testRequestModel.export());
        testResponseModel.setStatus("new");
        //
        // Figure out working directory and clear it if found or create it if not found
        //
        logger.info(this.guid, "Create or clear Working Directory");
        String workingDirectory = null;
        try {
            workingDirectory = ConfigReader.get(ConfigProperties.SCRIPTPROCESSOR_WORKING_DIRECTORY);
        } catch (Throwable t) {
            testResponseModel.setStatus("error");
            testResponseModel.setStatusMessage("Unable to determine working directory: " + t.getMessage());
            this.abendWriteTestResponse(t, "Unable to determine working directory", testResponseModel);
            return;
        }
        String workingDirectoryFull = System.getProperty("java.io.tmpdir") + workingDirectory;
        if(Files.exists(Paths.get(workingDirectoryFull))) {
            logger.info(guid, "Working Directory " + workingDirectoryFull + " exists - clearing");
            ScriptProcessor scriptProcessor = new ScriptProcessor();
            scriptProcessor.setWorkingDirectory(workingDirectory);
            scriptProcessor.addLine("rm -fR *");
            try {
                scriptProcessor.run();
            } catch (Throwable t) {
                testResponseModel.setStatus("error");
                testResponseModel.setStatusMessage("Unable to clear working directory: " + t.getMessage());
                this.abendWriteTestResponse(t, "Unable to clear working directory", testResponseModel);
                return;
            }
            if(scriptProcessor.getExitValue()!=0) {
                testResponseModel.setStatus("error");
                testResponseModel.setStatusMessage("Unable to clear working directory: " + scriptProcessor.getExitValue());
                this.abendWriteTestResponse(null, "Unable to clear working directory: " + scriptProcessor.getExitValue(), testResponseModel);
                return;
            } else {
                logger.info(this.guid, "Working Directory " + workingDirectoryFull + " Cleared");
            }
        } else {
            try {
                Files.createDirectories(Paths.get(workingDirectoryFull));
            } catch (Throwable t) {
                testResponseModel.setStatus("error");
                testResponseModel.setStatusMessage("Unable to create working directory: " + t.getMessage());
                this.abendWriteTestResponse(t, "Unable to create working directory", testResponseModel);
                return;
            }
            logger.info(this.guid, "Created Working Directory " + workingDirectoryFull);
        }
        //
        // Build script to execute git pull
        //
        logger.info(this.guid, "Build script to clone repo");
        String repoUrl = null;
        try {
            repoUrl = ConfigReader.get(ConfigProperties.GIT_REPO_URL);
        } catch (Throwable t) {
            testResponseModel.setStatus("error");
            testResponseModel.setStatusMessage("Unable to get Git Repo URL: " + t.getMessage());
            this.abendWriteTestResponse(t, "Unable to get Git Repo URL", testResponseModel);
            return;
        }
        String cloneToDirectory = null;
        try {
            cloneToDirectory = ConfigReader.get(ConfigProperties.GIT_CLONE_TO_DIRECTORY);
        } catch (Throwable t) {
            testResponseModel.setStatus("error");
            testResponseModel.setStatusMessage("Unable to get Clone-to Directory: " + t.getMessage());
            this.abendWriteTestResponse(t, "Unable to get Clone-to Directory", testResponseModel);
            return;
        }
        //
        // Clone Repository
        //
        ScriptProcessor scriptProcessor = new ScriptProcessor();
        scriptProcessor.setWorkingDirectory(workingDirectory);
        try {
            scriptProcessor.addLine("git clone " + repoUrl + " " + cloneToDirectory);
        } catch (Throwable t) {
            testResponseModel.setStatus("error");
            testResponseModel.setStatusMessage("Unable to build 'git clone' command: " + t.getMessage());
            this.abendWriteTestResponse(t, "Unable to build 'git clone' command", testResponseModel);
            return;
        }
        try {
            scriptProcessor.run();
        } catch (Throwable t) {
            testResponseModel.setStatus("error");
            testResponseModel.setStatusMessage("Could not execute script to clone repo:: " + t.getMessage());
            this.abendWriteTestResponse(t, "Could not execute script to clone repo", testResponseModel);
            return;
        }
        if(scriptProcessor.getExitValue()!=0) {
            testResponseModel.setStatus("error");
            testResponseModel.setStatusMessage("Non-Zero exit code from script to clone repo: " + scriptProcessor.getExitValue());
            this.writeResultsToS3(scriptProcessor.getStdoutFile(), null, null, testResponseModel);
            this.abendWriteTestResponse(null, "Unable to clear working directory: " + scriptProcessor.getExitValue(), testResponseModel);
            return;
        }
        String clonedDirectory = workingDirectoryFull + ScriptProcessor.fs + cloneToDirectory;
        if(!Files.exists(Paths.get(clonedDirectory))) {
            testResponseModel.setStatus("error");
            testResponseModel.setStatusMessage("Clone-to Directory not found - Repo was not cloned successfully");
            this.writeResultsToS3(scriptProcessor.getStdoutFile(), null, null, testResponseModel);
            return;
        }
        File cloneStdoutFile = scriptProcessor.getStdoutFile();
        //
        // Override any parameters in files specificed by the Test Request
        //
        this.overrideParameters(testRequestModel, clonedDirectory);
        //
        // Build and execute Script to perform Testing
        //
        scriptProcessor = new ScriptProcessor();
        scriptProcessor.setWorkingDirectory(clonedDirectory);
        String branch = null;
        try {
            branch = testRequestModel.getBranch();
            scriptProcessor.addLine("git checkout " + branch);
        } catch (Throwable t) {
            // Branch is optional
        }
        String tagExpression = null;
        try {
            tagExpression = testRequestModel.getTagExpression();
        } catch (Throwable t) {
            testResponseModel.setStatus("error");
            testResponseModel.setStatusMessage("Cannot determine tagExpression: " + t.getMessage());
            this.writeResultsToS3(cloneStdoutFile, null, null, testResponseModel);
            this.abendWriteTestResponse(t, "Cannot determine tagExpression", testResponseModel);
            return;
        }
        scriptProcessor.addLine("mvn exec:exec etc etc");
        try {
            scriptProcessor.run();
        } catch (Throwable t) {
            testResponseModel.setStatus("error");
            testResponseModel.setStatusMessage("Could not execute script to execute tests: " + t.getMessage());
            this.writeResultsToS3(cloneStdoutFile, scriptProcessor.getStdoutFile(), null, testResponseModel);
            this.abendWriteTestResponse(t, "Could not execute script to execute tests", testResponseModel);
            return;
        }
        if(scriptProcessor.getExitValue()!=0) {
            testResponseModel.setStatus("error");
            testResponseModel.setStatusMessage("Non-Zero exit code from script to execute tests: " + scriptProcessor.getExitValue());
            this.writeResultsToS3(cloneStdoutFile, scriptProcessor.getStdoutFile(), null, testResponseModel);
            this.abendWriteTestResponse(null, "Non-Zero exit code from script to execute tests: " + scriptProcessor.getExitValue(), testResponseModel);
            return;
        }
        File execStdoutFile = scriptProcessor.getStdoutFile();
        //
        // Capture outout JSON and write to S3
        //
        String jsonFileLocation = null;
        try {
            jsonFileLocation = ConfigReader.get(ConfigProperties.JSON_FILE_LOCATION);
        } catch (Throwable t) {
            testResponseModel.setStatus("error");
            testResponseModel.setStatusMessage("Could not determine output json filename: " + t.getMessage());
            this.writeResultsToS3(cloneStdoutFile, execStdoutFile, null, testResponseModel);
            this.abendWriteTestResponse(t, "Could not determine output json filename", testResponseModel);
            return;
        }
        File jsonOutputFile = new File(clonedDirectory + ScriptProcessor.fs + jsonFileLocation);
        if(!jsonOutputFile.exists()) {
            logger.error(this.guid, "Could not find Json Output file: " + jsonFileLocation);
            testResponseModel.setStatus("error");
            testResponseModel.setStatusMessage("Could not find Json Output file: " + jsonFileLocation);
            this.writeResultsToS3(cloneStdoutFile, execStdoutFile, null, testResponseModel);
            this.abendWriteTestResponse(null, "Could not find Json Output file: " + jsonFileLocation, testResponseModel);
            return;
        }
        this.writeResultsToS3(cloneStdoutFile, execStdoutFile, jsonOutputFile, testResponseModel);
        //
        // Write to Test Response
        try {
            this.kafkaWriter.write(ConfigProperties.TEST_RESPONSE_TOPIC, testResponseModel.getGuid(), testResponseModel.export());
        } catch (Throwable t) {
            this.abendMessage(t, "Unable to Write to test-response");
            return;
        }
    }

    private void abendMessage(Throwable t, String message) {
        String messageToWrite = message;
        try {
            messageToWrite += ": " + t.getMessage();
        } catch (Throwable t2) {
        }
        logger.error(this.guid, messageToWrite);
        logger.error(this.guid, "End Processing - ERROR");
    }

    private void abendWriteTestResponse(Throwable t, String message, TestResponseModel testResponseModel) {
        try {
            this.kafkaWriter.write(ConfigProperties.TEST_RESPONSE_TOPIC, testResponseModel.getGuid(), testResponseModel.export());
        } catch (Throwable t2) {
            this.abendMessage(t, message + ": Also Unable to Write to test-response: " + t2.getMessage());
            return;
        }
        this.abendMessage(t, message);
    }

    private void overrideParameters(TestRequestModel testRequestModel, String clonedDirectory) {
        String configFileDirectory = null;
        try {
            configFileDirectory = ConfigReader.get(ConfigProperties.CONFIG_DIRECTORY);
        } catch (Throwable t) {
            logger.error(this.guid, "Unable to determine repo config directory: " + t.getMessage());
            return;
        }
        int fileEntries = 0;
        try {
            fileEntries = testRequestModel.getConfigurationSize();
        } catch (Throwable t) {
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
                logger.error(this.guid, "No Properties File Specified");
                continue;
            }
            String propertiesFileFull = clonedDirectory + ScriptProcessor.fs + configFileDirectory + ScriptProcessor.fs + propertiesFile;
            if(!Files.exists(Paths.get(propertiesFileFull))) {
                logger.error("Properties File does not exist in repo: " + propertiesFile);
                continue;
            }
            int itemEntries = 0;
            List<String> propertiesList = null;
            try {
                propertiesList = testRequestModel.getPropertiesList(i);
                itemEntries = propertiesList.size();
            } catch (Throwable t) {
                logger.warn(this.guid, "For file " + propertiesFile + " no config items were specified");
                continue;
            }
            Properties properties = new Properties();
            try {
                properties.load(new FileInputStream(new File(propertiesFileFull)));
            } catch (Throwable t) {
                logger.error(this.guid, "Unable to read or parse Configuration file " + propertiesFile + ": " + t.getMessage());
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
            try {
                properties.store(new FileOutputStream(new File(propertiesFileFull)), "Updated");
            } catch (Throwable t) {
                logger.error(this.guid, "Unable to rewrite properties file " + propertiesFile + " :" + t.getMessage());
            }
        }
    }

    private void writeResultsToS3(File cloneStdoutFile, File execStdoutFile, File jsonFile, TestResponseModel testResponseModel) {
        String tempDir = System.getProperty("java.io.tmpdir");
        if (cloneStdoutFile != null || execStdoutFile != null) {
            String stdListFileName = this.guid + "_stdlist.log";
            String stdListFileNameFull = tempDir + stdListFileName;
            Path stdListPath = Paths.get(stdListFileNameFull);
            try {
                if (Files.exists(stdListPath)) {
                    Files.delete(stdListPath);
                } else {
                    Files.createFile(stdListPath);
                }
            } catch (Throwable t) {
                logger.error(this.guid, "Unable to create StdList file: " + stdListFileName + " " + t.getMessage());
            }
            try {
                FileOutputStream stdListOutputStream = new FileOutputStream(stdListPath.toFile());
                if (cloneStdoutFile != null) {
                    if (cloneStdoutFile.exists()) {
                        FileUtils.copyFile(cloneStdoutFile, stdListOutputStream);
                    }
                }
                if (execStdoutFile != null) {
                    if (execStdoutFile.exists()) {
                        FileUtils.copyFile(execStdoutFile, stdListOutputStream);
                    }
                }
                stdListOutputStream.close();
            } catch (Throwable t) {
                logger.error(this.guid, "Unable to create StdList file: " + stdListFileName + " " + t.getMessage());
            }
            File S3ZippedStdListFile = Util.zipInPlace(stdListPath.toFile());
            this.writeFileToS3(S3ZippedStdListFile);
            testResponseModel.addStdout(S3ZippedStdListFile.getName());
        }
        if(jsonFile!=null) {
            String jsonFileName = this.guid + ".json";
            String jsonFileNameFull = tempDir + jsonFileName;
            Path jsonPath = Paths.get(jsonFileNameFull);
            try {
                if (Files.exists(jsonPath)) {
                    Files.delete(jsonPath);
                } else {
                    Files.createFile(jsonPath);
                }
            } catch (Throwable t) {
                logger.error(this.guid, "Unable to create Json Output file: " + jsonFileName + " " + t.getMessage());
            }
            try {
                FileOutputStream jsonOutputStream = new FileOutputStream(jsonPath.toFile());
                FileUtils.copyFile(jsonFile, jsonOutputStream);
                jsonOutputStream.close();
            } catch (Throwable t) {
                logger.error(this.guid, "Unable to create Json file: " + jsonFileName + " " + t.getMessage());
            }
            File S3ZippedJsonFile = Util.zipInPlace(jsonPath.toFile());
            this.writeFileToS3(S3ZippedJsonFile);
            testResponseModel.addResultJson(S3ZippedJsonFile.getName());
        }
    }

    private void writeFileToS3(File file) {
        if(!file.exists()) {
            logger.warn(this.guid, "Could not find file to write to S3: " + file.getName());
            return;
        }
    }
}
