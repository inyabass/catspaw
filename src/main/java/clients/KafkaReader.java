package clients;

import config.ConfigReader;
import logging.Logger;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.Assert;
import runtime.TestRequestListener;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Properties;

public class KafkaReader {

    final static Logger logger = new Logger(MethodHandles.lookup().lookupClass());

    private KafkaConsumer<String, String> kafkaConsumer = null;
    private TestRequestListener testRequestListener = null;

    public KafkaReader(TestRequestListener testRequestListener, String topic) {
        this.testRequestListener = testRequestListener;
        this.kafkaConsumer = this.createClient();
        this.kafkaConsumer.subscribe(Collections.singletonList(topic));
    }

    private KafkaConsumer<String, String> createClient() {
        Properties properties = new Properties();
        try {
            properties.put(KafkaConfig.BOOTSTRAP_SERVERS, ConfigReader.get(KafkaConfig.BOOTSTRAP_SERVERS));
            properties.put(KafkaConfig.KEY_DESERIALIZER, ConfigReader.get(KafkaConfig.KEY_DESERIALIZER));
            properties.put(KafkaConfig.VALUE_DESERIALIZER, ConfigReader.get(KafkaConfig.VALUE_DESERIALIZER));
            properties.put(KafkaConfig.CLIENT_ID, ConfigReader.get(KafkaConfig.CLIENT_ID));
            properties.put(KafkaConfig.GROUP_ID, ConfigReader.get(KafkaConfig.GROUP_ID));
            properties.put(KafkaConfig.ENABLE_AUTO_COMMIT, ConfigReader.get(KafkaConfig.ENABLE_AUTO_COMMIT));
            properties.put(KafkaConfig.MAX_POLL_RECORDS, ConfigReader.get(KafkaConfig.MAX_POLL_RECORDS));
        } catch (Throwable t) {
            Assert.fail("Unable to Get Consumer Configuration: " + t.getMessage());
        }
        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<String, String>(properties);
        logger.info("Kafka Consumer Created");
        return kafkaConsumer;
    }

    public void pollLoop() {
        for(int i = 1;i<=10;i++) {
            testRequestListener.processRecord(i);
        }
    }
}
