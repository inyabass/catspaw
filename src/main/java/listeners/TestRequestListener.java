package listeners;

import clients.KafkaConfig;
import clients.KafkaReader;
import logging.Logger;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.lang.invoke.MethodHandles;

public class TestRequestListener implements Listener {

    final static Logger logger = new Logger(MethodHandles.lookup().lookupClass());

    String key = null;
    String value = null;

    public static void main(String[] args) {
        TestRequestListener testRequestListener = new TestRequestListener();
        KafkaReader kafkaReader = new KafkaReader(testRequestListener, KafkaConfig.TEST_REQUEST_TOPIC, KafkaConfig.TEST_REQUEST_LISTENER_GROUP, KafkaConfig.TEST_REQUEST_LISTENER_POLL_DURATION);
        kafkaReader.pollLoop();
    }

    public void processRecord(ConsumerRecord<String, String> consumerRecord) {
        this.key = consumerRecord.key();
        this.value = consumerRecord.value();
        logger.info("key: " + this.key);
        logger.info("value: " + this.value);
    }
}
