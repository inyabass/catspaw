package runtime;

import clients.KafkaConfig;
import clients.KafkaReader;

public class TestRequestListener {

    private static String TOPIC = KafkaConfig.TEST_REQUEST_TOPIC;

    public static void main(String[] args) {
        TestRequestListener testRequestListener = new TestRequestListener();
        KafkaReader kafkaReader = new KafkaReader(testRequestListener, TOPIC);
        kafkaReader.pollLoop();
    }

    public void processRecord(int i) {
       System.out.println(i);
    }

}
