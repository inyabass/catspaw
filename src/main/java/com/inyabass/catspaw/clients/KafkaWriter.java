package com.inyabass.catspaw.clients;

import com.inyabass.catspaw.config.ConfigProperties;
import com.inyabass.catspaw.config.ConfigReader;
import com.inyabass.catspaw.logging.Logger;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.Assert;

import java.lang.invoke.MethodHandles;
import java.util.Properties;

public class KafkaWriter {

    final static Logger logger = new Logger(MethodHandles.lookup().lookupClass());

    private KafkaProducer<String, String> kafkaProducer = null;

    public static void main(String[] args) throws Throwable {
        KafkaWriter kafkaWriter = new KafkaWriter();
        kafkaWriter.write(ConfigProperties.TEST_REQUEST_TOPIC, "thekey4", "thevalue4");
        int i = 0;
    }

    public KafkaWriter() {
        this.kafkaProducer = this.createClient();
    }

    private KafkaProducer<String, String> createClient() {
        Properties properties = new Properties();
        try {
            properties.put(ConfigProperties.BOOTSTRAP_SERVERS, ConfigReader.get(ConfigProperties.BOOTSTRAP_SERVERS));
            properties.put(ConfigProperties.KEY_SERIALIZER, ConfigReader.get(ConfigProperties.KEY_SERIALIZER));
            properties.put(ConfigProperties.VALUE_SERIALIZER, ConfigReader.get(ConfigProperties.VALUE_SERIALIZER));
            properties.put(ConfigProperties.ACKS, ConfigReader.get(ConfigProperties.ACKS));
            properties.put(ConfigProperties.RETRIES, ConfigReader.get(ConfigProperties.RETRIES));
            properties.put(ConfigProperties.CLIENT_ID, ConfigReader.get(ConfigProperties.CLIENT_ID));
        } catch (Throwable t) {
            Assert.fail("Unable to Get Producer Configuration: " + t.getMessage());
        }
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<String, String>(properties);
        logger.debug("Kafka Producer Created");
        return kafkaProducer;
    }

    public void write(String topic, String key, String value) throws Throwable {
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, key, value);
        RecordMetadata recordMetadata = this.kafkaProducer.send(producerRecord).get();
        this.kafkaProducer.flush();
        logger.info(key, "Record posted to " + topic + " offset " + recordMetadata.offset());
    }
}
