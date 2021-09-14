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
            logger.error(this.guid, "Unable to Parse JSON: " + t.getMessage());
            logger.error(this.guid, "End Processing - ERROR");
            return;
        }
        //
        // Set Test Request to "in-progress" and put to Couchdb
        //
        logger.info(this.guid, "Set status to 'in-progress' and add couchdb record");
        testRequestModel.setStatus("in-progress");
        if(ListenerHelper.putCouchdbCatsEntry(logger, testRequestModel)) {
            logger.info(this.guid, "couchdb record put - status 'in-progress'");
        } else {
            logger.error(this.guid, "End Processing - ERROR");
            return;
        }
        //
        // Figure out working directory and clear it if found or create it if not found
        //
        logger.info(this.guid, "Create or clear Working Directory");
        String workingDirectory = null;
        try {
            workingDirectory = ConfigReader.get(ConfigProperties.SCRIPTPROCESSOR_WORKING_DIRECTORY);
        } catch (Throwable t) {
            logger.error(this.guid, "Unable to determine working directory: " + t.getMessage());
            testRequestModel.setStatus("error");
            testRequestModel.setStatusMessage("Unable to determine working directory: " + t.getMessage());
            ListenerHelper.updateCouchdbCatsEntry(logger, testRequestModel);
            logger.info(this.guid, "couchdb record update - status 'error'");
            logger.error(this.guid, "End Processing - ERROR");
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
                logger.error(this.guid, "Unable to clear working directory: " + t.getMessage());
                testRequestModel.setStatus("error");
                testRequestModel.setStatusMessage("Unable to clear working directory: " + t.getMessage());
                ListenerHelper.updateCouchdbCatsEntry(logger, testRequestModel);
                logger.info(this.guid, "couchdb record update - status 'error'");
                logger.error(this.guid, "End Processing - ERROR");
                return;
            }
            if(scriptProcessor.getExitValue()!=0) {
                logger.error(this.guid, "Unable to clear working directory: " + scriptProcessor.getExitValue());
                testRequestModel.setStatus("error");
                testRequestModel.setStatusMessage("Unable to clear working directory: " + scriptProcessor.getExitValue());
                ListenerHelper.updateCouchdbCatsEntry(logger, testRequestModel);
                logger.info(this.guid, "couchdb record update - status 'error'");
                logger.error(this.guid, "End Processing - ERROR");
                return;
            } else {
                logger.info(this.guid, "Working Directory " + workingDirectoryFull + " Cleared");
            }
        } else {
            try {
                Files.createDirectories(Paths.get(workingDirectoryFull));
            } catch (Throwable t) {
                logger.error(this.guid, "Unable to create working directory: " + t.getMessage());
                testRequestModel.setStatus("error");
                testRequestModel.setStatusMessage("Unable to create working directory: " + t.getMessage());
                ListenerHelper.updateCouchdbCatsEntry(logger, testRequestModel);
                logger.info(this.guid, "couchdb record update - status 'error'");
                logger.error(this.guid, "End Processing - ERROR");
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
            logger.error(this.guid, "Unable to get Git Repo URL: " + t.getMessage());
            testRequestModel.setStatus("error");
            testRequestModel.setStatusMessage("Unable to get Git Repo URL: " + t.getMessage());
            ListenerHelper.updateCouchdbCatsEntry(logger, testRequestModel);
            logger.info(this.guid, "couchdb record update - status 'error'");
            logger.error(this.guid, "End Processing - ERROR");
            return;
        }
        String cloneToDirectory = null;
        try {
            cloneToDirectory = ConfigReader.get(ConfigProperties.GIT_CLONE_TO_DIRECTORY);
        } catch (Throwable t) {
            logger.error(this.guid, "Unable to get Clone-to Directory: " + t.getMessage());
            testRequestModel.setStatus("error");
            testRequestModel.setStatusMessage("Unable to get Clone-to Directory: " + t.getMessage());
            ListenerHelper.updateCouchdbCatsEntry(logger, testRequestModel);
            logger.info(this.guid, "couchdb record update - status 'error'");
            logger.error(this.guid, "End Processing - ERROR");
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
            logger.error(this.guid, "Unable to build 'git clone' command: " + t.getMessage());
            testRequestModel.setStatus("error");
            testRequestModel.setStatusMessage("Unable to build 'git clone' command: " + t.getMessage());
            ListenerHelper.updateCouchdbCatsEntry(logger, testRequestModel);
            logger.info(this.guid, "couchdb record update - status 'error'");
            logger.error(this.guid, "End Processing - ERROR");
            return;
        }
        try {
            scriptProcessor.run();
        } catch (Throwable t) {
            logger.error(this.guid, "Could not execute script to clone repo: " + t.getMessage());
            testRequestModel.setStatus("error");
            testRequestModel.setStatusMessage("Could not execute script to clone repo:: " + t.getMessage());
            ListenerHelper.updateCouchdbCatsEntry(logger, testRequestModel);
            this.writeResultsToS3(scriptProcessor.getStdoutFile(), null, null);
            logger.info(this.guid, "couchdb record update - status 'error'");
            logger.error(this.guid, "End Processing - ERROR");
            return;
        }
        if(scriptProcessor.getExitValue()!=0) {
            logger.error(this.guid, "Non-Zero exit code from script to clone repo: " + scriptProcessor.getExitValue());
            testRequestModel.setStatus("error");
            testRequestModel.setStatusMessage("Non-Zero exit code from script to clone repo: " + scriptProcessor.getExitValue());
            ListenerHelper.updateCouchdbCatsEntry(logger, testRequestModel);
            this.writeResultsToS3(scriptProcessor.getStdoutFile(), null, null);
            logger.info(this.guid, "couchdb record update - status 'error'");
            logger.error(this.guid, "End Processing - ERROR");
            return;
        }
        String clonedDirectory = workingDirectoryFull + ScriptProcessor.fs + cloneToDirectory;
        if(!Files.exists(Paths.get(clonedDirectory))) {
            logger.error("Clone-to Directory not found - Repo was not cloned successfully");
            testRequestModel.setStatus("error");
            testRequestModel.setStatusMessage("Clone-to Directory not found - Repo was not cloned successfully");
            ListenerHelper.updateCouchdbCatsEntry(logger, testRequestModel);
            this.writeResultsToS3(scriptProcessor.getStdoutFile(), null, null);
            logger.info(this.guid, "couchdb record update - status 'error'");
            logger.error(this.guid, "End Processing - ERROR");
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
            logger.error("Cannot determine tagExpression: " + t.getMessage());
            testRequestModel.setStatus("error");
            testRequestModel.setStatusMessage("Cannot determine tagExpression: " + t.getMessage());
            ListenerHelper.updateCouchdbCatsEntry(logger, testRequestModel);
            this.writeResultsToS3(cloneStdoutFile, null, null);
            logger.info(this.guid, "couchdb record update - status 'error'");
            logger.error(this.guid, "End Processing - ERROR");
            return;
        }
        scriptProcessor.addLine("mvn exec:exec etc etc");
        try {
            scriptProcessor.run();
        } catch (Throwable t) {
            logger.error(this.guid, "Could not execute script to execute tests: " + t.getMessage());
            testRequestModel.setStatus("error");
            testRequestModel.setStatusMessage("Could not execute script to execute tests: " + t.getMessage());
            ListenerHelper.updateCouchdbCatsEntry(logger, testRequestModel);
            this.writeResultsToS3(cloneStdoutFile, scriptProcessor.getStdoutFile(), null);
            logger.info(this.guid, "couchdb record update - status 'error'");
            logger.error(this.guid, "End Processing - ERROR");
            return;
        }
        if(scriptProcessor.getExitValue()!=0) {
            logger.error(this.guid, "Non-Zero exit code from script to execute tests: " + scriptProcessor.getExitValue());
            testRequestModel.setStatus("error");
            testRequestModel.setStatusMessage("Non-Zero exit code from script to execute tests: " + scriptProcessor.getExitValue());
            ListenerHelper.updateCouchdbCatsEntry(logger, testRequestModel);
            this.writeResultsToS3(cloneStdoutFile, scriptProcessor.getStdoutFile(), null);
            logger.info(this.guid, "couchdb record update - status 'error'");
            logger.error(this.guid, "End Processing - ERROR");
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
            logger.error(this.guid, "Could not determine output json filename: " + t.getMessage());
            testRequestModel.setStatus("error");
            testRequestModel.setStatusMessage("Could not determine output json filename: " + t.getMessage());
            ListenerHelper.updateCouchdbCatsEntry(logger, testRequestModel);
            this.writeResultsToS3(cloneStdoutFile, execStdoutFile, null);
            logger.info(this.guid, "couchdb record update - status 'error'");
            logger.error(this.guid, "End Processing - ERROR");
            return;
        }
        File jsonOutputFile = new File(clonedDirectory + ScriptProcessor.fs + jsonFileLocation);
        if(!jsonOutputFile.exists()) {
            logger.error(this.guid, "Could not find Json Output file: " + jsonFileLocation);
            testRequestModel.setStatus("error");
            testRequestModel.setStatusMessage("Could not find Json Output file: " + jsonFileLocation);
            ListenerHelper.updateCouchdbCatsEntry(logger, testRequestModel);
            this.writeResultsToS3(cloneStdoutFile, execStdoutFile, null);
            logger.info(this.guid, "couchdb record update - status 'error'");
            logger.error(this.guid, "End Processing - ERROR");
            return;
        }
        this.writeResultsToS3(cloneStdoutFile, execStdoutFile, jsonOutputFile);
        //
        // Write Test Response Kafka message status "new"
        //
        TestResponseModel testResponseModel = new TestResponseModel(testRequestModel.export());
        testResponseModel.setStatus("new");
        testResponseModel.delete_id();
        testResponseModel.delete_rev();
        try {
            this.kafkaWriter.write(ConfigProperties.TEST_RESPONSE_TOPIC, testResponseModel.getGuid(), testResponseModel.export());
        } catch (Throwable t) {
            logger.error(this.guid, "Unable to Write to test-response: " + t.getMessage());
            testRequestModel.setStatus("error");
            testRequestModel.setStatusMessage("Unable to Write to test-response: " + t.getMessage());
            ListenerHelper.updateCouchdbCatsEntry(logger, testRequestModel);
            logger.info(this.guid, "couchdb record update - status 'error'");
            logger.info(this.guid, "End Processing - ERROR");
            return;
        }
        // Update Couchdb entry status to "done"
        //
        testRequestModel.setStatus("done");
        if(!ListenerHelper.updateCouchdbCatsEntry(logger, testRequestModel)) {
            logger.info(this.guid, "End Processing - ERROR");
            return;
       } else {
            logger.info(this.guid, "couchdb record update - status 'done'");
        }
        logger.info(this.guid, "End Processing - SUCCESS");
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

    private void writeResultsToS3(File cloneStdoutFile, File execStdoutFile, File jsonFile) {
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
        }
    }

    private void writeFileToS3(File file) {
        if(!file.exists()) {
            logger.warn(this.guid, "Could not find file to write to S3: " + file.getName());
            return;
        }
    }
}
