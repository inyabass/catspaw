package com.inyabass.catspaw.listeners;

import com.inyabass.catspaw.clients.KafkaReader;
import com.inyabass.catspaw.config.ConfigProperties;
import com.inyabass.catspaw.config.ConfigReader;
import com.inyabass.catspaw.logging.Logger;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class TestResponser implements Listener {

    final static Logger logger = new Logger(MethodHandles.lookup().lookupClass());

    private boolean debug = false;
    private String guid = null;
    private String inputJson = null;
    private List<String> tempFiles = new ArrayList<>();

    public TestResponser() throws Throwable {
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
        TestResponser testResponser = new TestResponser();
        String topic = ConfigProperties.TEST_RESPONSE_TOPIC;
        String groupId = ConfigReader.get(ConfigProperties.TEST_RESPONSE_LISTENER_GROUP_ID);
        Duration duration = Duration.ofSeconds(Integer.parseInt(ConfigReader.get(ConfigProperties.POLL_DURATION)));
        KafkaReader kafkaReader = new KafkaReader(testResponser, topic, groupId, duration);
        kafkaReader.pollLoop();
    }

    public void processRecord(ConsumerRecord<String, String> consumerRecord) {
        this.tempFiles = new ArrayList<>();
        this.guid = consumerRecord.key();
        this.inputJson = consumerRecord.value();
        this.process();
        if(!this.debug) {
            this.cleanUpTempFiles();
        }
    }

    public void process() {
    }

    private void cleanUpTempFiles() {
        logger.info(this.guid, "Cleaning up Temporary Files");
        for(String file: this.tempFiles) {
            if(Files.exists(Paths.get(file))) {
                try {
                    Files.delete(Paths.get(file));
                    logger.info(this.guid, "File " + file + " deleted");
                } catch (Throwable t) {
                    logger.warn(this.guid, "Unable to delete " + file + " : " + t.getMessage());
                }
            }
        }
    }
}
