package com.inyabass.catspaw.listeners;

import com.inyabass.catspaw.clients.KafkaReader;
import com.inyabass.catspaw.clients.KafkaWriter;
import com.inyabass.catspaw.config.ConfigProperties;
import com.inyabass.catspaw.config.ConfigReader;
import com.inyabass.catspaw.data.TestRequestModel;
import com.inyabass.catspaw.data.TestResponseModel;
import com.inyabass.catspaw.logging.Logger;
import com.inyabass.catspaw.util.ScriptProcessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;

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
        String groupId = ConfigReader.get(ConfigProperties.GROUP_ID);
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
        TestRequestModel testRequestModel = null;
        try {
            testRequestModel = new TestRequestModel(this.inputJson);
        } catch (Throwable t) {
            logger.error(this.guid, "Unable to Parse JSON: " + t.getMessage());
            logger.info(this.guid, "End Processing - ERROR");
            return;
        }
        //
        // Set Test Request to "in-progress" and put to Couchdb
        //
        testRequestModel.setStatus("in-progress");
        if(ListenerHelper.putCouchdbCatsEntry(logger, testRequestModel)) {
            logger.info(this.guid, "couchdb record put - status 'in-progress'");
        } else {
            logger.info(this.guid, "End Processing - ERROR");
            return;
        }
        //
        // Figure out working directory and clear it if found or create it if not found
        //
        String workingDirectory = null;
        try {
            workingDirectory = ConfigReader.get(ConfigProperties.SCRIPTPROCESSOR_WORKING_DIRECTORY);
        } catch (Throwable t) {
            logger.error(this.guid, "Unable to determine working directory: " + t.getMessage());
            testRequestModel.setStatus("error");
            testRequestModel.setStatusMessage("Unable to determine working directory: " + t.getMessage());
            ListenerHelper.updateCouchdbCatsEntry(logger, testRequestModel);
            logger.info(this.guid, "couchdb record update - status 'error'");
            logger.info(this.guid, "End Processing - ERROR");
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
                logger.info(this.guid, "End Processing - ERROR");
                return;
            }
            if(scriptProcessor.getExitValue()!=0) {
                logger.error(this.guid, "Unable to clear working directory: " + scriptProcessor.getExitValue());
                testRequestModel.setStatus("error");
                testRequestModel.setStatusMessage("Unable to clear working directory: " + scriptProcessor.getExitValue());
                ListenerHelper.updateCouchdbCatsEntry(logger, testRequestModel);
                logger.info(this.guid, "couchdb record update - status 'error'");
                logger.info(this.guid, "End Processing - ERROR");
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
                logger.info(this.guid, "End Processing - ERROR");
                return;
            }
            logger.info(this.guid, "Created Working Directory " + workingDirectoryFull);
        }
        // build script
        // execute script
        // capture stdout and zip it up
        // post zipped object to S3
        // capture test results json and zip it up
        // post zipped object to S3

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
}
