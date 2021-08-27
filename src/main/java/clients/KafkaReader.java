package clients;

import config.ConfigReader;
import listeners.Listener;
import logging.Logger;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.Assert;
import listeners.TestRequestListener;

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
            properties.put(KafkaConfig.BOOTSTRAP_SERVERS, ConfigReader.get(KafkaConfig.BOOTSTRAP_SERVERS));
            properties.put(KafkaConfig.KEY_DESERIALIZER, ConfigReader.get(KafkaConfig.KEY_DESERIALIZER));
            properties.put(KafkaConfig.VALUE_DESERIALIZER, ConfigReader.get(KafkaConfig.VALUE_DESERIALIZER));
            properties.put(KafkaConfig.CLIENT_ID, ConfigReader.get(KafkaConfig.CLIENT_ID));
            properties.put(KafkaConfig.GROUP_ID, this.groupId);
            properties.put(KafkaConfig.ENABLE_AUTO_COMMIT, ConfigReader.get(KafkaConfig.ENABLE_AUTO_COMMIT));
            properties.put(KafkaConfig.MAX_POLL_RECORDS, ConfigReader.get(KafkaConfig.MAX_POLL_RECORDS));
            properties.put(KafkaConfig.AUTO_OFFSET_RESET, ConfigReader.get(KafkaConfig.AUTO_OFFSET_RESET));
        } catch (Throwable t) {
            Assert.fail("Unable to Get Consumer Configuration: " + t.getMessage());
        }
        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<String, String>(properties);
        kafkaConsumer.subscribe(Collections.singletonList(this.topic));
        logger.info("Kafka Consumer Created - Topic '" + this.topic + "' Group ID '" + this.groupId + "'");
        return kafkaConsumer;
    }

    public void pollLoop() {
        while(true) {
            ConsumerRecords<String, String> consumerRecords = this.kafkaConsumer.poll(this.pollDuration);
            for(ConsumerRecord<String, String> consumerRecord: consumerRecords) {
                this.kafkaConsumer.commitSync();
                this.listener.processRecord(consumerRecord);
            }
        }
    }
}
