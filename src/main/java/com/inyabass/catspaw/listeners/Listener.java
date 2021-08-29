package com.inyabass.catspaw.listeners;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface Listener {

    public void processRecord(ConsumerRecord<String, String> record);

}
