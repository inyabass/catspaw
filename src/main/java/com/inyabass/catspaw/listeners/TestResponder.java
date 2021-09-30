package com.inyabass.catspaw.listeners;

import com.inyabass.catspaw.clients.AwsS3Client;
import com.inyabass.catspaw.clients.KafkaReader;
import com.inyabass.catspaw.clients.SmtpClient;
import com.inyabass.catspaw.config.ConfigProperties;
import com.inyabass.catspaw.config.ConfigReader;
import com.inyabass.catspaw.data.TestResponseModel;
import com.inyabass.catspaw.logging.Logger;
import com.inyabass.catspaw.util.ScriptProcessor;
import com.inyabass.catspaw.util.Util;
import org.apache.commons.io.FileUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class TestResponder implements Listener {

    final static Logger logger = new Logger(MethodHandles.lookup().lookupClass());

    private boolean debug = false;
    private String guid = null;
    private String inputJson = null;
    private List<String> tempFiles = new ArrayList<>();
    private static String MVN_COMMAND = "mvn --batch-mode";

    private static final String PRODUCE_REPORT = "report";
    private static final String PRODUCE_EMAIL = "email";
    private static final String POST_TO_TEAMS = "teams";

    public TestResponder() throws Throwable {
        logger.info("TestExecutor starting");
        ConfigReader.ConfigReader();
        String debugString = "false";
        try {
            debugString = ConfigReader.get(ConfigProperties.TESTRESPONDER_DEBUG);
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
        TestResponder testResponder = new TestResponder();
        String topic = ConfigProperties.TEST_RESPONSE_TOPIC;
        String groupId = ConfigReader.get(ConfigProperties.TEST_RESPONSE_LISTENER_GROUP_ID);
        Duration duration = Duration.ofSeconds(Integer.parseInt(ConfigReader.get(ConfigProperties.POLL_DURATION)));
        KafkaReader kafkaReader = new KafkaReader(testResponder, topic, groupId, duration);
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
        TestResponseModel testResponseModel = null;
        try {
            testResponseModel = new TestResponseModel(this.inputJson);
        } catch (Throwable t) {
            this.abendMessage(t, "Unable to Parse JSON");
            return;
        }
        //
        // If flagged as error then Send Notification Email and quit
        //
        if(testResponseModel.getStatus().equals(TestResponseModel.STATUS_ERROR)) {
            this.reportError(testResponseModel);
            return;
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
            this.abendMessage(t, "Unable to determine working directory");
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
                this.abendMessage(t, "Unable to clear working directory");
                return;
            }
            if(scriptProcessor.getExitValue()!=0) {
                this.abendMessage(null, "Unable to clear working directory: " + scriptProcessor.getExitValue());
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
                this.abendMessage(t, "Unable to create working directory");
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
            this.abendMessage(t, "Unable to get Git Repo URL");
            return;
        }
        String cloneToDirectory = null;
        try {
            cloneToDirectory = ConfigReader.get(ConfigProperties.GIT_CLONE_TO_DIRECTORY);
        } catch (Throwable t) {
            this.abendMessage(t, "Unable to get Clone-to Directory");
            return;
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
            this.abendMessage(t, "Unable to build 'git clone' command");
            return;
        }
        try {
            scriptProcessor.addLine("cd " + cloneToDirectory);
        } catch (Throwable t) {
            this.abendMessage(t, "Unable to build Change Directory Command for '" + cloneToDirectory + "'");
            return;
        }
        String branch = null;
        try {
            branch = testResponseModel.getBranch();
            logger.info(this.guid, "Using branch '" + branch + "'");
            scriptProcessor.addLine("git checkout " + branch);
        } catch (Throwable t) {
            logger.info(this.guid, "Using default branch");
        }
        scriptProcessor.addLine(MVN_COMMAND + " compile");
        try {
            scriptProcessor.run();
        } catch (Throwable t) {
            this.abendMessage(t, "Could not execute script to clone repo");
            return;
        }
        if(scriptProcessor.getExitValue()!=0) {
            try {
                this.writeStdoutFilesToS3(scriptProcessor.getStdoutFile(), null);
            } catch (Throwable t2) {
                logger.error(this.guid, "Unable to Write to Amazon S3: " + t2.getMessage());
            }
            this.abendMessage(null, "Unable to clear working directory: " + scriptProcessor.getExitValue());
            return;
        }
        if(!this.debug) {
            this.tempFiles.add(scriptProcessor.getStdoutFile().getAbsolutePath());
        }
        logger.info(this.guid, "Checking cloned directory exists");
        String clonedDirectory = workingDirectory + ScriptProcessor.fs + cloneToDirectory;
        String clonedDirectoryFull = workingDirectoryFull + ScriptProcessor.fs + cloneToDirectory;
        if(!Files.exists(Paths.get(clonedDirectoryFull))) {
            try {
                this.writeStdoutFilesToS3(scriptProcessor.getStdoutFile(), null);
            } catch (Throwable t2) {
                logger.error(this.guid, "Unable to Write to Amazon S3: " + t2.getMessage());
            }
            this.abendMessage(null, "Cloned Directory did not exist: " + clonedDirectoryFull);
            return;
        }
        logger.info(this.guid, "Repository Cloned and compiled successfully");
        File cloneStdoutFile = scriptProcessor.getStdoutFile();
        //
        // Override any parameters in files specified by the Test Request
        //
        logger.info(this.guid, "Overriding any parameters");
        Util.overrideParameters(testResponseModel, clonedDirectoryFull, this.guid);
        //
        // Pull Results Zipped JSON file from S3
        //
        String jsonZippedFileName = null;
        try {
            jsonZippedFileName = testResponseModel.getResultJson();
        } catch (Throwable t) {
            this.abendMessage(null, "Unable to pull the JSON Zipfile name from the test response: " + t.getMessage());
            return;
        }
        AwsS3Client awsS3Client = null;
        try {
            awsS3Client = new AwsS3Client();
        } catch (Throwable t) {
            this.abendMessage(null, "Unable to instantiate an S3 Client instance: " + t.getMessage());
            return;
        }
        File jsonZippedFile = awsS3Client.getObject(jsonZippedFileName);
        if(jsonZippedFile==null) {
            this.abendMessage(null, "Unable to pull the JSON Zipfile from S3 - see logs as to why");
            return;
        }
        //
        // Unzip the file in place
        //
        File jsonUnzippedFile = Util.unzipInPlace(jsonZippedFile);
        if(jsonUnzippedFile==null) {
            this.abendMessage(null, "Unable to Unzip the JSON Zipfile from S3 in place - see logs as to why");
            return;
        }
        //
        // Push the Results json file into the correct place in the repo
        //
        String jsonFileLocation = null;
        try {
            jsonFileLocation = ConfigReader.get(ConfigProperties.JSON_FILE_LOCATION);
        } catch (Throwable t) {
            this.abendMessage(t, "Unable to get JSON File location");
            return;
        }
        String targetJsonFileInRepo = clonedDirectoryFull + ScriptProcessor.fs + Util.convertPath(jsonFileLocation);
        Path targetPath = Paths.get(targetJsonFileInRepo);
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(jsonUnzippedFile);
        } catch (Throwable t) {
            this.abendMessage(null, "Unable to create a FileInputStream for the unzipped JSON file: " + t.getMessage());
            return;
        }
        try {
            Files.copy(inputStream, targetPath);
        } catch (Throwable t) {
            this.abendMessage(null, "Unable to copy JSON file into target place in repo: " + t.getMessage());
            return;
        }
        //
        // Create a properties file to be picked up by Reporter for insertion into reports
        //
        logger.info(this.guid, "Creating Properties File to be picked up by Reporter");
        String xferPropertiesFile = null;
        try {
            xferPropertiesFile = ConfigReader.get(ConfigProperties.EXPORTED_CONFIG_FILE);
        } catch (Throwable t) {
            this.abendMessage(t, "Unable to get Transfer Properties file name");
            return;
        }
        if(Files.exists(Paths.get(xferPropertiesFile))) {
            try {
                Files.delete(Paths.get(xferPropertiesFile));
            } catch (Throwable t) {
                this.abendMessage(t, "Unable to delete already existing Transfer Properties file");
                return;
            }
        }
        Properties xferProperties = new Properties();
        xferProperties.put("cats.guid", testResponseModel.getGuid());
        xferProperties.put("cats.requestor", testResponseModel.getRequestor());
        xferProperties.put("cats.timerequested", testResponseModel.getTimeRequested());
        xferProperties.put("cats.tagexpression", testResponseModel.getTagExpression());
        xferProperties.put("cats.branch", testResponseModel.getBranch());
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(new File(clonedDirectoryFull + ScriptProcessor.fs + xferPropertiesFile));
        } catch (Throwable t) {
            this.abendMessage(t, "Unable to create FileoutputStream on Transfer Properties file");
            return;
        }
        try {
            xferProperties.store(fileOutputStream, "Transfer Properties File");
            fileOutputStream.close();
        } catch (Throwable t) {
            this.abendMessage(t, "Unable to create Transfer Properties file");
            return;
        }
        //
        // Build and execute Script to Generate Report
        //
        logger.info(this.guid, "Building script to Execute Reports");
        scriptProcessor = new ScriptProcessor();
        scriptProcessor.setWorkingDirectory(clonedDirectory);
        String reports = "overview detailed";
        try {
            reports = testResponseModel.getReports();
            logger.info(this.guid, "Using Reports '" + reports + "'");
        } catch (Throwable t) {
            logger.info(this.guid, "Using default Reports");
        }
        String command = "./runreports.sh \"" + reports + "\" " + xferPropertiesFile;
        scriptProcessor.addLine(command);
        logger.info(this.guid, "Executing script to run Reports");
        try {
            scriptProcessor.run();
        } catch (Throwable t) {
            try {
                this.writeStdoutFilesToS3(cloneStdoutFile, scriptProcessor.getStdoutFile());
            } catch (Throwable t2) {
                logger.error(this.guid, "Unable to Write to Amazon S3: " + t2.getMessage());
            }
            this.abendMessage(t, "Could not execute script to execute tests");
            return;
        }
        if(scriptProcessor.getExitValue()!=0) {
            this.abendMessage(null, "Non-Zero exit code from script to execute tests: " + scriptProcessor.getExitValue());
            return;
        }
        logger.info(this.guid, "Report Execution Complete");
        if(!this.debug) {
            this.tempFiles.add(scriptProcessor.getStdoutFile().getAbsolutePath());
        }
       //
        // Upload reports html to S3 Reporting bucket
        //
        String reportsDirectory = null;
        try {
            reportsDirectory = ConfigReader.get(ConfigProperties.REPORTS_DIRECTORY);
        } catch (Throwable t) {
            this.abendMessage(t, "Unable to get JSON File location");
            return;
        }
        String reportsDirectoryFull = clonedDirectoryFull + ScriptProcessor.fs + Util.convertPath(reportsDirectory);
        Set<File> reportFiles = null;
        try {
            reportFiles = Util.getFilesInDirectory(reportsDirectoryFull, Integer.MAX_VALUE);
        } catch (Throwable t) {
            this.abendMessage(t, "Unable to get Any Report Files : " + t.getMessage());
            return;
        }
        String reportsBucket = null;
        try {
            reportsBucket = ConfigReader.get(ConfigProperties.AWS_REPORTING_BUCKET_NAME);
        } catch (Throwable t) {
            this.abendMessage(t, "Unable to get Reporting Bucket Name");
            return;
        }
        awsS3Client.setBucketName(reportsBucket);
        ArrayList<String> htmlFiles = new ArrayList<>();
        for(File reportFile: reportFiles) {
            String reportFileName = reportFile.getPath();
            reportFileName = this.guid + reportFileName.replace(reportsDirectoryFull, "").replaceAll("\\\\", "/");
            logger.info(this.guid, "Uploading " + reportFile.getName() + " to AWS S3");
            awsS3Client.putObject(reportFileName, reportFile);
            if(reportFileName.endsWith(".html")||reportFileName.endsWith(".htm")) {
                htmlFiles.add(reportFileName);
            }
        }
        ArrayList<String> reportFullUrls = new ArrayList<>();
        if(htmlFiles.size()>0) {
            String baseWebUrl = null;
            try {
                baseWebUrl = ConfigReader.get(ConfigProperties.AWS_WEB_BASE_URL);
                logger.info(this.guid, "Getting List of Report URLs");
                for(String htmlFileName: htmlFiles) {
                    String webAddress = baseWebUrl + htmlFileName;
                    logger.info(this.guid, "URL: " + webAddress);
                    reportFullUrls.add(webAddress);
                }
            } catch (Throwable t) {
            }
        }
        //
        // Send notification Email
        //
        logger.info(this.guid, "Sending Notification Email");
        String to = testResponseModel.getEmailTo();
        String subject = "Test Results for job " + this.guid;
        String body = "<html><head></head><body>";
        String runStatus = testResponseModel.getStatus();
        if(!runStatus.equals(TestResponseModel.STATUS_NEW)) {
            body += "<p align=\"left\">";
            body += "Job Status was : " + runStatus + "<br>";
            body += "Status message was : " + testResponseModel.getStatusMessage();
            body += "</p>";
        }
        body += "<p align=\"left\">Test Reports</p>";
        body += "<p align=\"left\">";
        for(String webAddress: reportFullUrls) {
            body += "<a href=\"" + webAddress + "\">" + webAddress + "</a><br>";
        }
        body += "</p>";
        body += "</body></html>";
        try {
            Util.sendEmail(to, subject, body);
        } catch (Throwable t) {
            this.abendMessage(t, "Unable to Send Notification Email");
            return;
        }
        logger.info(this.guid, "Email Notification Sent Successfully");
        logger.info(this.guid, "End Processing - SUCCESS");
    }

    private void abendMessage(Throwable t, String message) {
        String messageToWrite = message;
        if(t!=null) {
            messageToWrite += ": " + t.getMessage();
        }
        logger.error(this.guid, messageToWrite);
        logger.error(this.guid, "End Processing - ERROR");
    }

    private void reportError(TestResponseModel testResponseModel) {
        logger.error(this.guid, "Test Response showed status Error : " + testResponseModel.getStatusMessage());
        logger.info(this.guid, "Sending Notification Email (Error)");
        String to = testResponseModel.getEmailTo();
        String subject = "Test Results for job " + this.guid + " (Failed)";
        String body = "<html><head></head><body>";
        body += "<p align=\"left\">Error Message : " + testResponseModel.getStatusMessage() + "</p>";
        body += "</body></html>";
        try {
            Util.sendEmail(to, subject, body);
        } catch (Throwable t) {
            this.abendMessage(t, "Unable to Send Notification Email (Error)");
            return;
        }
    }

    private void writeStdoutFilesToS3(File cloneStdoutFile, File execStdoutFile) throws Throwable {
        logger.info(this.guid, "Writing reporting stdout to AWS S3 Bucket");
        String tempDir = Util.getTemp();
        if (cloneStdoutFile != null || execStdoutFile != null) {
            logger.info(this.guid, "Writing stdout file(s) to AWS S3");
            String stdListFileName = this.guid + "_report_stdlist.log";
            String stdListFileNameFull = null;
            stdListFileNameFull = tempDir + stdListFileName;
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
    }
}
