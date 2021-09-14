package com.inyabass.catspaw.clients;

import com.inyabass.catspaw.config.ConfigProperties;
import com.inyabass.catspaw.config.ConfigReader;
import com.inyabass.catspaw.listeners.Listener;
import com.inyabass.catspaw.logging.Logger;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.Assert;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class KafkaReader {

    final static Logger logger = new Logger(MethodHandles.lookup().lookupClass());

    private KafkaConsumer<String, String> kafkaConsumer = null;
    private Listener listener = null;
    private String topic = null;
    private String groupId = null;
    private Duration pollDuration = null;

    public KafkaReader(Listener listener, String topic, String groupId, Duration pollDuration) {
        this.listener = listener;
        this.topic = topic;
        this.groupId = groupId;
        this.pollDuration = pollDuration;
        this.kafkaConsumer = this.createClient();
    }

    private KafkaConsumer<String, String> createClient() {
        Properties properties = new Properties();
        try {
            properties.put(ConfigProperties.BOOTSTRAP_SERVERS, ConfigReader.get(ConfigProperties.BOOTSTRAP_SERVERS));
            properties.put(ConfigProperties.KEY_DESERIALIZER, ConfigReader.get(ConfigProperties.KEY_DESERIALIZER));
            properties.put(ConfigProperties.VALUE_DESERIALIZER, ConfigReader.get(ConfigProperties.VALUE_DESERIALIZER));
            properties.put(ConfigProperties.CLIENT_ID, ConfigReader.get(ConfigProperties.CLIENT_ID));
            properties.put(ConfigProperties.GROUP_ID, this.groupId);
            properties.put(ConfigProperties.ENABLE_AUTO_COMMIT, ConfigReader.get(ConfigProperties.ENABLE_AUTO_COMMIT));
            properties.put(ConfigProperties.MAX_POLL_RECORDS, ConfigReader.get(ConfigProperties.MAX_POLL_RECORDS));
            properties.put(ConfigProperties.AUTO_OFFSET_RESET, ConfigReader.get(ConfigProperties.AUTO_OFFSET_RESET));
            properties.put(ConfigProperties.ALLOW_AUTO_CREATE_TOPICS, ConfigReader.get(ConfigProperties.ALLOW_AUTO_CREATE_TOPICS));
        } catch (Throwable t) {
            Assert.fail("Unable to Get Consumer Configuration: " + t.getMessage());
        }
        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<String, String>(properties);
        kafkaConsumer.subscribe(Collections.singletonList(this.topic));
        logger.debug("Kafka Consumer Created on Topic '" + this.topic + "' Group ID '" + this.groupId + "'");
        return kafkaConsumer;
    }

    public void pollLoop() {
        while(true) {
            ConsumerRecords<String, String> consumerRecords = this.kafkaConsumer.poll(this.pollDuration);
            for(ConsumerRecord<String, String> consumerRecord: consumerRecords) {
                this.kafkaConsumer.commitSync();
//                this.kafkaConsumer.pause();
                this.listener.processRecord(consumerRecord);
            }
        }
    }
}
