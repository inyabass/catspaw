package clients;

import config.ConfigReader;
import logging.Logger;
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
        kafkaWriter.write(KafkaConfig.TEST_REQUEST_TOPIC, "thekey3", "thevalue3");
        int i = 0;
    }

    public KafkaWriter() {
        this.kafkaProducer = this.createClient();
    }

    private KafkaProducer<String, String> createClient() {
        Properties properties = new Properties();
        try {
            properties.put(KafkaConfig.BOOTSTRAP_SERVERS, ConfigReader.get(KafkaConfig.BOOTSTRAP_SERVERS));
            properties.put(KafkaConfig.KEY_SERIALIZER, ConfigReader.get(KafkaConfig.KEY_SERIALIZER));
            properties.put(KafkaConfig.VALUE_SERIALIZER, ConfigReader.get(KafkaConfig.VALUE_SERIALIZER));
            properties.put(KafkaConfig.ACKS, ConfigReader.get(KafkaConfig.ACKS));
            properties.put(KafkaConfig.RETRIES, ConfigReader.get(KafkaConfig.RETRIES));
            properties.put(KafkaConfig.CLIENT_ID, ConfigReader.get(KafkaConfig.CLIENT_ID));
        } catch (Throwable t) {
            Assert.fail("Unable to Get Producer Configuration: " + t.getMessage());
        }
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<String, String>(properties);
        logger.info("Kafka Producer Created");
        return kafkaProducer;
    }

    public void write(String topic, String key, String value) throws Throwable {
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, key, value);
        RecordMetadata recordMetadata = this.kafkaProducer.send(producerRecord).get();
        this.kafkaProducer.flush();
        logger.info("Record posted to " + topic + " offset " + recordMetadata.offset());
    }
}
