package com.inyabass.catspaw.listeners;

import com.inyabass.catspaw.clients.AwsS3Client;
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
import java.io.FileNotFoundException;
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
        this.process();
    }

    public void process() {
        logger.info(this.guid, "Start Processing " + this.inputJson);
        //
        // Parse Kafka Test Request json payload
        //
        logger.info(this.guid, "Parsing JSON Payload");
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
        testResponseModel.addStatus("new");
        testResponseModel.addStatusMessage("");
        //
        // Figure out working directory and clear it if found or create it if not found
        //
        logger.info(this.guid, "Creating or clearing Working Directory");
        String workingDirectory = null;
        try {
            workingDirectory = ConfigReader.get(ConfigProperties.SCRIPTPROCESSOR_WORKING_DIRECTORY);
            if(workingDirectory.toUpperCase().equals("$RANDOM")) {
                workingDirectory = "cats" + String.valueOf(System.currentTimeMillis()) + "_" + Util.getGuid();
                logger.info(this.guid, "Using Random Working Directory: " + workingDirectory);
            }
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
        logger.info(this.guid, "Building script to clone repository");
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
        logger.info(this.guid, "Executing script to clone repository");
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
            scriptProcessor.addLine("cd " + cloneToDirectory);
        } catch (Throwable t) {
            testResponseModel.setStatus("error");
            testResponseModel.setStatusMessage("Unable to build Change Directory Command for '" + cloneToDirectory + "': " + t.getMessage());
            this.abendWriteTestResponse(t, "Unable to build Change Directory Command for '" + cloneToDirectory + "'", testResponseModel);
            return;
        }
        String branch = null;
        try {
            branch = testRequestModel.getBranch();
            logger.info(this.guid, "Using branch '" + branch + "'");
            scriptProcessor.addLine("git checkout " + branch);
        } catch (Throwable t) {
            logger.info(this.guid, "Using default branch");
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
            try {
                this.writeResultsToS3(scriptProcessor.getStdoutFile(), null, null, testResponseModel);
            } catch (Throwable t2) {
                logger.error(this.guid, "Unable to Write to Amazon S3: " + t2.getMessage());
            }
            this.abendWriteTestResponse(null, "Unable to clear working directory: " + scriptProcessor.getExitValue(), testResponseModel);
            return;
        }
        logger.info(this.guid, "Checking cloned directory exists");
        String clonedDirectory = workingDirectory + ScriptProcessor.fs + cloneToDirectory;
        String clonedDirectoryFull = workingDirectoryFull + ScriptProcessor.fs + cloneToDirectory;
        if(!Files.exists(Paths.get(clonedDirectoryFull))) {
            testResponseModel.setStatus("error");
            testResponseModel.setStatusMessage("Clone-to Directory not found - Repo was not cloned successfully");
            try {
                this.writeResultsToS3(scriptProcessor.getStdoutFile(), null, null, testResponseModel);
            } catch (Throwable t2) {
                logger.error(this.guid, "Unable to Write to Amazon S3: " + t2.getMessage());
            }
            return;
        }
        logger.info(this.guid, "Repository Cloned successfully");
        File cloneStdoutFile = scriptProcessor.getStdoutFile();
        //
        // Override any parameters in files specified by the Test Request
        //
        logger.info(this.guid, "Overriding any parameters");
        this.overrideParameters(testRequestModel, clonedDirectoryFull);
        //
        // Build and execute Script to perform Testing
        //
        logger.info(this.guid, "Building script to perform tests");
        scriptProcessor = new ScriptProcessor();
        scriptProcessor.setWorkingDirectory(clonedDirectory);
        String configurationFile = null;
        try {
            configurationFile = testRequestModel.getConfigurationFile();
            logger.info(this.guid, "Using Configuration File '" + configurationFile + "'");
        } catch (Throwable t) {
            logger.info(this.guid, "Using default Configuration File");
        }
        String tagExpression = null;
        try {
            tagExpression = testRequestModel.getTagExpression();
            logger.info(this.guid, "Using Tag Expression '" + tagExpression + "'");
        } catch (Throwable t) {
            testResponseModel.setStatus("error");
            testResponseModel.setStatusMessage("Cannot determine tagExpression: " + t.getMessage());
            try {
                this.writeResultsToS3(cloneStdoutFile, null, null, testResponseModel);
            } catch (Throwable t2) {
                logger.error(this.guid, "Unable to Write to Amazon S3: " + t2.getMessage());
            }
            this.abendWriteTestResponse(t, "Cannot determine tagExpression", testResponseModel);
            return;
        }
        scriptProcessor.addLine("mvn compile");
        String command = "./runtests.sh \"" + tagExpression + "\"";
        if(configurationFile!=null) {
            command += " " + configurationFile;
        }
        scriptProcessor.addLine(command);
        logger.info(this.guid, "Executing script to run tests");
        try {
            scriptProcessor.run();
        } catch (Throwable t) {
            testResponseModel.setStatus("error");
            testResponseModel.setStatusMessage("Could not execute script to execute tests: " + t.getMessage());
            try {
                this.writeResultsToS3(cloneStdoutFile, scriptProcessor.getStdoutFile(), null, testResponseModel);
            } catch (Throwable t2) {
                logger.error(this.guid, "Unable to Write to Amazon S3: " + t2.getMessage());
            }
            this.abendWriteTestResponse(t, "Could not execute script to execute tests", testResponseModel);
            return;
        }
        if(scriptProcessor.getExitValue()!=0) {
            logger.warn(this.guid, "Non-Zero exit code from script to execute tests: " + scriptProcessor.getExitValue());
            testResponseModel.setStatus("warn");
            testResponseModel.setStatusMessage("Non-Zero exit code from script to execute tests: " + scriptProcessor.getExitValue());
        }
        logger.info(this.guid, "Test Script Execution Complete");
        File execStdoutFile = scriptProcessor.getStdoutFile();
        //
        // Capture outout JSON and write to S3
        //
        logger.info(this.guid, "Capturing output JSON File");
        String jsonFileLocation = null;
        try {
            jsonFileLocation = ConfigReader.get(ConfigProperties.JSON_FILE_LOCATION);
        } catch (Throwable t) {
            testResponseModel.setStatus("error");
            testResponseModel.setStatusMessage("Could not determine output json filename: " + t.getMessage());
            try {
                this.writeResultsToS3(cloneStdoutFile, execStdoutFile, null, testResponseModel);
            } catch (Throwable t2) {
                logger.error(this.guid, "Unable to Write to Amazon S3: " + t2.getMessage());
            }
            this.abendWriteTestResponse(t, "Could not determine output json filename", testResponseModel);
            return;
        }
        File jsonOutputFile = new File(clonedDirectoryFull + ScriptProcessor.fs + jsonFileLocation);
        if(!jsonOutputFile.exists()) {
            logger.warn(this.guid, "Could not find Json Output file: " + clonedDirectoryFull + ScriptProcessor.fs + jsonFileLocation);
            if(testResponseModel.getStatus().equals("new")) {
                testResponseModel.setStatus("warn");
                testResponseModel.setStatusMessage("Could not find Json Output file: " + jsonFileLocation);
            }
            jsonOutputFile = null;
        }
        try {
            this.writeResultsToS3(cloneStdoutFile, execStdoutFile, jsonOutputFile, testResponseModel);
        } catch (Throwable t) {
            testResponseModel.setStatus("error");
            testResponseModel.setStatusMessage("Could not Write Results to S3: " + t.getMessage());
            this.abendWriteTestResponse(t, "Could not Write Results to S3", testResponseModel);
            return;
        }
        //
        // Write to Test Response
        //
        logger.info(this.guid, "Writing message to test-response topic");
        try {
            this.kafkaWriter.write(ConfigProperties.TEST_RESPONSE_TOPIC, testResponseModel.getGuid(), testResponseModel.export());
            logger.info(this.guid, "Written to test-response: " + testResponseModel.export());
        } catch (Throwable t) {
            this.abendMessage(t, "Unable to Write to test-response");
            return;
        }
        logger.error(this.guid, "End Processing - SUCCESS");
    }

    private void abendMessage(Throwable t, String message) {
        String messageToWrite = message;
        if(t!=null) {
            messageToWrite += ": " + t.getMessage();
        }
        logger.error(this.guid, messageToWrite);
        logger.error(this.guid, "End Processing - ERROR");
    }

    private void abendWriteTestResponse(Throwable t, String message, TestResponseModel testResponseModel) {
        logger.info(this.guid, "Writing message to test-response topic (after error)");
        try {
            this.kafkaWriter.write(ConfigProperties.TEST_RESPONSE_TOPIC, testResponseModel.getGuid(), testResponseModel.export());
            logger.info(this.guid, "Written to test-response: " + testResponseModel.export());
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
        configFileDirectory = Util.convertPath(configFileDirectory);
        int fileEntries = 0;
        try {
            fileEntries = testRequestModel.getConfigurationSize();
        } catch (Throwable t) {
            logger.warn(this.guid, "Could not get the number of Configuration File Entries");
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
                logger.warn("Properties File does not exist in repo: " + propertiesFile);
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
                logger.warn(this.guid, "Unable to read or parse Configuration file " + propertiesFile + ": " + t.getMessage());
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
                logger.warn(this.guid, "Unable to rewrite properties file " + propertiesFile + " :" + t.getMessage());
            }
        }
    }

    private void writeResultsToS3(File cloneStdoutFile, File execStdoutFile, File jsonFile, TestResponseModel testResponseModel) throws Throwable {
        logger.info(this.guid, "Writing results to AWS S3 Bucket");
        String tempDir = System.getProperty("java.io.tmpdir");
        if (cloneStdoutFile != null || execStdoutFile != null) {
            logger.info(this.guid, "Writing stdout file(s) to AWS S3");
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
                throw t;
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
                throw t;
            }
            File S3ZippedStdListFile = Util.zipInPlace(stdListPath.toFile());
            testResponseModel.addStdout(S3ZippedStdListFile.getName());
            try {
                this.writeFileToS3(S3ZippedStdListFile);
            } catch (Throwable t) {
                logger.error(this.guid, "Unable to Write Stdout File(s) to AWS S3");
                throw t;
            }
            logger.info(this.guid, "Stdout file(s) written to AWS S3");
        }
        if(jsonFile!=null) {
            logger.info(this.guid, "Writing JSON log file to AWS S3");
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
            testResponseModel.addResultJson(S3ZippedJsonFile.getName());
            try {
                this.writeFileToS3(S3ZippedJsonFile);
            } catch (Throwable t) {
                logger.error(this.guid, "Unable to Write JSON Log File to AWS S3");
                throw t;
            }
            logger.info(this.guid, "JSON log file written to AWS S3");
        }
    }

    private void writeFileToS3(File file) throws Throwable {
        if(!file.exists()) {
            logger.warn(this.guid, "Could not find file to write to S3: " + file.getName());
            throw new FileNotFoundException("Could not find file to write to S3: " + file.getName());
        }
        AwsS3Client awsS3Client = null;
        try {
            awsS3Client = new AwsS3Client();
        } catch (Throwable t) {
            logger.error(this.guid, "Unable to create Amazon S3 Client");
            throw t;
        }
        awsS3Client.putObject(file);
    }
}
