package com.inyabass.catspaw.listeners;

import com.inyabass.catspaw.clients.KafkaConfig;
import com.inyabass.catspaw.clients.KafkaReader;
import com.inyabass.catspaw.config.ConfigReader;
import com.inyabass.catspaw.logging.Logger;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.lang.invoke.MethodHandles;
import java.time.Duration;

public class TestRequestListener implements Listener {

    final static Logger logger = new Logger(MethodHandles.lookup().lookupClass());

    String key = null;
    String value = null;

    public static void main(String[] args) throws Throwable {
        TestRequestListener testRequestListener = new TestRequestListener();
        String topic = ConfigReader.get(KafkaConfig.TOPIC);
        String groupId = ConfigReader.get(KafkaConfig.GROUP_ID);
        Duration duration = Duration.ofSeconds(Integer.parseInt(ConfigReader.get(KafkaConfig.POLL_DURATION)));
        KafkaReader kafkaReader = new KafkaReader(testRequestListener, topic, groupId, duration);
        kafkaReader.pollLoop();
    }

    public void processRecord(ConsumerRecord<String, String> consumerRecord) {
        this.key = consumerRecord.key();
        this.value = consumerRecord.value();
        logger.info("key: " + this.key);
        logger.info("value: " + this.value);
    }
}
