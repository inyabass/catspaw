package clients;

public class KafkaConfig {

    public static final String BOOTSTRAP_SERVERS = "bootstrap.servers";
    public static final String KEY_SERIALIZER = "key.serializer";
    public static final String VALUE_SERIALIZER = "value.serializer";
    public static final String ACKS = "acks";
    public static final String RETRIES = "retries";
    public static final String CLIENT_ID = "client.id";
    public static final String GROUP_ID = "group.id";
    public static final String ENABLE_AUTO_COMMIT = "enable.auto.commit";
    public static final String MAX_POLL_RECORDS = "max.poll.records";
    public static final String KEY_DESERIALIZER = "key.deserializer";
    public static final String VALUE_DESERIALIZER = "value.deserializer";

    public static final String TEST_REQUEST_TOPIC = "test-request";
    public static final String TEST_RESPONSE_TOPIC = "test-response";
}
