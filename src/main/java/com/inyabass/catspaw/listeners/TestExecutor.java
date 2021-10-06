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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TestExecutor implements Listener {

    final static Logger logger = new Logger(MethodHandles.lookup().lookupClass());

    private boolean debug = false;
    private String guid = null;
    private String inputJson = null;
    private KafkaWriter kafkaWriter = new KafkaWriter();
    private List<String> tempFiles = new ArrayList<>();
    private static String MVN_COMMAND = "mvn --batch-mode";
    private String capturedJsonFileName = null;
    private String capturedStdoutFileName = null;

    public TestExecutor() throws Throwable {
        logger.info("TestExecutor starting");
        ConfigReader.ConfigReader();
        String debugString = "false";
        try {
            debugString = ConfigReader.get(ConfigProperties.TESTEXECUTOR_DEBUG);
        } catch (Throwable t) {
        }
        if(debugString!=null) {
            try {
                this.debug = Boolean.parseBoolean(debugString);
            } catch (Throwable t) {
            }
        }
        if(this.debug) {
            logger.info("Debug Enabled");
        }
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
        this.tempFiles = new ArrayList<>();
        this.guid = consumerRecord.key();
        this.inputJson = consumerRecord.value();
        this.process();
        if(!this.debug) {
            Util.cleanUpTempFiles(this.tempFiles, this.guid);
        }
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
        testResponseModel.addStatus(TestResponseModel.STATUS_NEW);
        testResponseModel.addStatusMessage("Awaiting being picked up");
        //
        // Update cats.jobs status
        //
        try {
            ListenerHelper.jobStatusUpdate(logger, testResponseModel.getGuid(), testResponseModel.getStatus(), testResponseModel.getStatusMessage(), this.capturedJsonFileName, this.capturedStdoutFileName, null);
        } catch (Throwable t) {
            logger.warn(this.guid, "Unable to Update cats.jobs table");
        }
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
            testResponseModel.setStatus(TestResponseModel.STATUS_ERROR);
            testResponseModel.setStatusMessage("Unable to determine working directory: " + t.getMessage());
            this.abendWriteTestResponse(t, "Unable to determine working directory", testResponseModel);
            return;
        }
        String workingDirectoryFull = null;
        workingDirectoryFull = Util.getTemp() + workingDirectory;
        if(Files.exists(Paths.get(workingDirectoryFull))) {
            logger.info(guid, "Working Directory " + workingDirectoryFull + " exists - clearing");
            ScriptProcessor scriptProcessor = new ScriptProcessor();
            scriptProcessor.setWorkingDirectory(workingDirectory);
            scriptProcessor.addLine("rm -fR *");
            try {
                scriptProcessor.run();
            } catch (Throwable t) {
                testResponseModel.setStatus(TestResponseModel.STATUS_ERROR);
                testResponseModel.setStatusMessage("Unable to clear working directory: " + t.getMessage());
                this.abendWriteTestResponse(t, "Unable to clear working directory", testResponseModel);
                return;
            }
            if(scriptProcessor.getExitValue()!=0) {
                testResponseModel.setStatus(TestResponseModel.STATUS_ERROR);
                testResponseModel.setStatusMessage("Unable to clear working directory: " + scriptProcessor.getExitValue());
                this.abendWriteTestResponse(null, "Unable to clear working directory: " + scriptProcessor.getExitValue(), testResponseModel);
                return;
            } else {
                logger.info(this.guid, "Working Directory " + workingDirectoryFull + " Cleared");
            }
            if(!this.debug) {
                this.tempFiles.add(scriptProcessor.getStdoutFile().getAbsolutePath());
            }
        } else {
            try {
                Files.createDirectories(Paths.get(workingDirectoryFull));
            } catch (Throwable t) {
                testResponseModel.setStatus(TestResponseModel.STATUS_ERROR);
                testResponseModel.setStatusMessage("Unable to create working directory: " + t.getMessage());
                this.abendWriteTestResponse(t, "Unable to create working directory", testResponseModel);
                return;
            }
            logger.info(this.guid, "Created Working Directory " + workingDirectoryFull);
        }
        //
        // Build script to execute git clone
        //
        logger.info(this.guid, "Building script to clone repository");
        String repoUrl = null;
        try {
            repoUrl = ConfigReader.get(ConfigProperties.GIT_REPO_URL);
        } catch (Throwable t) {
            testResponseModel.setStatus(TestResponseModel.STATUS_ERROR);
            testResponseModel.setStatusMessage("Unable to get Git Repo URL: " + t.getMessage());
            this.abendWriteTestResponse(t, "Unable to get Git Repo URL", testResponseModel);
            return;
        }
        String cloneToDirectory = null;
        try {
            cloneToDirectory = ConfigReader.get(ConfigProperties.GIT_CLONE_TO_DIRECTORY);
        } catch (Throwable t) {
            testResponseModel.setStatus(TestResponseModel.STATUS_ERROR);
            testResponseModel.setStatusMessage("Unable to get Clone-to Directory: " + t.getMessage());
            this.abendWriteTestResponse(t, "Unable to get Clone-to Directory", testResponseModel);
            return;
        }
        //
        // Update cats.jobs status
        //
        try {
            ListenerHelper.jobStatusUpdate(logger, testResponseModel.getGuid(), TestResponseModel.STATUS_CLONING, "Repository being cloned", this.capturedJsonFileName, this.capturedStdoutFileName, null);
        } catch (Throwable t) {
            logger.warn(this.guid, "Unable to Update cats.jobs table");
        }
        //
        // Clone Repository
        //
        logger.info(this.guid, "Executing script to clone and compile repository");
        ScriptProcessor scriptProcessor = new ScriptProcessor();
        scriptProcessor.setWorkingDirectory(workingDirectory);
        try {
            scriptProcessor.addLine("git clone " + repoUrl + " " + cloneToDirectory);
        } catch (Throwable t) {
            testResponseModel.setStatus(TestResponseModel.STATUS_ERROR);
            testResponseModel.setStatusMessage("Unable to build 'git clone' command: " + t.getMessage());
            this.abendWriteTestResponse(t, "Unable to build 'git clone' command", testResponseModel);
            return;
        }
        try {
            scriptProcessor.addLine("cd " + cloneToDirectory);
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            testResponseModel.setStatus(TestResponseModel.STATUS_ERROR);
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
        scriptProcessor.addLine(MVN_COMMAND + " compile");
        try {
            scriptProcessor.run();
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            testResponseModel.setStatus(TestResponseModel.STATUS_ERROR);
            testResponseModel.setStatusMessage("Could not execute script to clone and compile repo:: " + t.getMessage());
            this.abendWriteTestResponse(t, "Could not execute script to clone and compile repo", testResponseModel);
            return;
        }
        if(scriptProcessor.getExitValue()!=0) {
            testResponseModel.setStatus(TestResponseModel.STATUS_ERROR);
            testResponseModel.setStatusMessage("Non-Zero exit code from script to clone repo: " + scriptProcessor.getExitValue());
            try {
                this.writeResultsToS3(scriptProcessor.getStdoutFile(), null, null, testResponseModel);
            } catch (Throwable t2) {
                logger.error(this.guid, "Unable to Write to Amazon S3: " + t2.getMessage());
            }
            this.abendWriteTestResponse(null, "Unable to clear working directory: " + scriptProcessor.getExitValue(), testResponseModel);
            return;
        }
        if(!this.debug) {
            this.tempFiles.add(scriptProcessor.getStdoutFile().getAbsolutePath());
        }
        logger.info(this.guid, "Checking cloned directory exists");
        String clonedDirectory = workingDirectory + ScriptProcessor.fs + cloneToDirectory;
        String clonedDirectoryFull = workingDirectoryFull + ScriptProcessor.fs + cloneToDirectory;
        if(!Files.exists(Paths.get(clonedDirectoryFull))) {
            testResponseModel.setStatus(TestResponseModel.STATUS_ERROR);
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
        // Update cats.jobs status
        //
        try {
            ListenerHelper.jobStatusUpdate(logger, testResponseModel.getGuid(), TestResponseModel.STATUS_CLONED, "Repository was cloned successfully", this.capturedJsonFileName, this.capturedStdoutFileName, null);
        } catch (Throwable t) {
            logger.warn(this.guid, "Unable to Update cats.jobs table");
        }
        //
        // Override any parameters in files specified by the Test Request
        //
        logger.info(this.guid, "Overriding any parameters");
        Util.overrideParameters(testRequestModel, clonedDirectoryFull, this.guid);
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
            testResponseModel.setStatus(TestResponseModel.STATUS_ERROR);
            testResponseModel.setStatusMessage("Cannot determine tagExpression: " + t.getMessage());
            try {
                this.writeResultsToS3(cloneStdoutFile, null, null, testResponseModel);
            } catch (Throwable t2) {
                logger.error(this.guid, "Unable to Write to Amazon S3: " + t2.getMessage());
            }
            this.abendWriteTestResponse(t, "Cannot determine tagExpression", testResponseModel);
            return;
        }
        String command = "./runtests.sh \"" + tagExpression + "\"";
        if(configurationFile!=null) {
            command += " " + configurationFile;
        }
        scriptProcessor.addLine(command);
        logger.info(this.guid, "Executing script to run tests");
        //
        // Update cats.jobs status
        //
        try {
            ListenerHelper.jobStatusUpdate(logger, testResponseModel.getGuid(), TestResponseModel.STATUS_RUNNING_TESTS, "Running Tests", this.capturedJsonFileName, this.capturedStdoutFileName, null);
        } catch (Throwable t) {
            logger.warn(this.guid, "Unable to Update cats.jobs table");
        }
        try {
            scriptProcessor.run();
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            testResponseModel.setStatus(TestResponseModel.STATUS_ERROR);
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
            logger.warn(this.guid, "Tests failed to execute successfully: " + scriptProcessor.getExitValue());
            testResponseModel.setStatus(TestResponseModel.STATUS_FAILED);
            testResponseModel.setStatusMessage("Tests failed to execute successfully: " + scriptProcessor.getExitValue());
        } else {
            testResponseModel.setStatus(TestResponseModel.STATUS_TESTS_RUN);
            testResponseModel.setStatusMessage("Tests Executed Successfully");
        }
        logger.info(this.guid, "Test Script Execution Complete");
        if(!this.debug) {
            this.tempFiles.add(scriptProcessor.getStdoutFile().getAbsolutePath());
        }
        File execStdoutFile = scriptProcessor.getStdoutFile();
        //
        // Update cats.jobs status
        //
        try {
            ListenerHelper.jobStatusUpdate(logger, testResponseModel.getGuid(), testResponseModel.getStatus(), testResponseModel.getStatusMessage(), this.capturedJsonFileName, this.capturedStdoutFileName, null);
        } catch (Throwable t) {
            logger.warn(this.guid, "Unable to Update cats.jobs table");
        }
        //
        // Capture outout JSON and write to S3
        //
        logger.info(this.guid, "Capturing output JSON File");
        String jsonFileLocation = null;
        try {
            jsonFileLocation = ConfigReader.get(ConfigProperties.JSON_FILE_LOCATION);
        } catch (Throwable t) {
            testResponseModel.setStatus(TestResponseModel.STATUS_ERROR);
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
                testResponseModel.setStatus(TestResponseModel.STATUS_WARN);
                testResponseModel.setStatusMessage("Could not find Json Output file: " + jsonFileLocation);
            }
            jsonOutputFile = null;
        }
        try {
            this.writeResultsToS3(cloneStdoutFile, execStdoutFile, jsonOutputFile, testResponseModel);
        } catch (Throwable t) {
            testResponseModel.setStatus(TestResponseModel.STATUS_ERROR);
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
        logger.info(this.guid, "End Processing - SUCCESS");
    }

    private void abendMessage(Throwable t, String message) {
        String messageToWrite = message;
        if(t!=null) {
            messageToWrite += ": " + t.getMessage();
        }
        logger.error(this.guid, messageToWrite);
        //
        // Update cats.jobs status
        //
        try {
            ListenerHelper.jobStatusUpdate(logger, this.guid, TestResponseModel.STATUS_ERROR, message, this.capturedJsonFileName, this.capturedStdoutFileName, null);
        } catch (Throwable t2) {
            logger.warn(this.guid, "Unable to Update cats.jobs table");
        }
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

    private void writeResultsToS3(File cloneStdoutFile, File execStdoutFile, File jsonFile, TestResponseModel testResponseModel) throws Throwable {
        logger.info(this.guid, "Writing results to AWS S3 Bucket");
        String tempDir = Util.getTemp();
        if (cloneStdoutFile != null || execStdoutFile != null) {
            logger.info(this.guid, "Writing stdout file(s) to AWS S3");
            String stdListFileName = this.guid + "_stdlist.log";
            String stdListFileNameFull = null;
            stdListFileNameFull = tempDir + stdListFileName;
            Path stdListPath = Paths.get(stdListFileNameFull);
            File stdListFile = stdListPath.toFile();
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
            if(S3ZippedStdListFile==null) {
                logger.error(this.guid, "Unable to zip " + stdListPath.toFile().getName() + " in place");
            }
            testResponseModel.addStdout(S3ZippedStdListFile.getName());
            this.capturedStdoutFileName = S3ZippedStdListFile.getName();
            try {
                Util.writeFileToS3(S3ZippedStdListFile, this.guid);
            } catch (Throwable t) {
                logger.error(this.guid, "Unable to Write Stdout File(s) to AWS S3");
                throw t;
            }
            logger.info(this.guid, "Stdout file(s) written to AWS S3");
            if(!this.debug) {
                this.tempFiles.add(S3ZippedStdListFile.getAbsolutePath());
                if(cloneStdoutFile!=null) {
                    this.tempFiles.add(cloneStdoutFile.getAbsolutePath());
                }
                if(execStdoutFile!=null) {
                    this.tempFiles.add(execStdoutFile.getAbsolutePath());
                }
            }
        }
        if(jsonFile!=null) {
            logger.info(this.guid, "Writing JSON log file to AWS S3");
            String jsonFileName = this.guid + ".json";
            String jsonFileNameFull = null;
            if(Util.isWindows()) {
                jsonFileNameFull = tempDir + jsonFileName;
            } else {
                jsonFileNameFull = tempDir + "/" + jsonFileName;
            }
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
            if(S3ZippedJsonFile==null) {
                logger.error(this.guid, "Unable to zip " + jsonPath.toFile().getName() + " in place");
            }
            testResponseModel.addResultJson(S3ZippedJsonFile.getName());
            this.capturedJsonFileName = S3ZippedJsonFile.getName();
            try {
                Util.writeFileToS3(S3ZippedJsonFile, this.guid);
            } catch (Throwable t) {
                logger.error(this.guid, "Unable to Write JSON Log File to AWS S3");
                throw t;
            }
            logger.info(this.guid, "JSON log file written to AWS S3");
            if(!this.debug) {
                this.tempFiles.add(jsonFile.getAbsolutePath());
            }
        }
    }
}
