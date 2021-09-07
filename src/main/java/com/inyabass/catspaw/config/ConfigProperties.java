package com.inyabass.catspaw.config;

import java.time.Duration;

public class ConfigProperties {

    // Kafka
    public static final String BOOTSTRAP_SERVERS = "bootstrap.servers";
    public static final String KEY_SERIALIZER = "key.serializer";
    public static final String VALUE_SERIALIZER = "value.serializer";
    public static final String ACKS = "acks";
    public static final String RETRIES = "retries";
    public static final String CLIENT_ID = "client.id";
    public static final String GROUP_ID = "group.id";
    public static final String POLL_DURATION = "poll.duration";
    public static final String ENABLE_AUTO_COMMIT = "enable.auto.commit";
    public static final String MAX_POLL_RECORDS = "max.poll.records";
    public static final String KEY_DESERIALIZER = "key.deserializer";
    public static final String VALUE_DESERIALIZER = "value.deserializer";
    public static final String AUTO_OFFSET_RESET = "auto.offset.reset";
    public static final String ALLOW_AUTO_CREATE_TOPICS = "allow.auto.create.topics";
    public static final String TEST_REQUEST_TOPIC = "test-request";
    public static final String TEST_RESPONSE_TOPIC = "test-response";

    // Couchdb
    public static final String COUCHDB_SERVER = "couchdb.server";

    // ScriptProcessor
    public static final String SCRIPTPROCESSOR_WORKING_DIRECTORY = "scriptprocessor.working.directory";

    // TestExecutor
    public static final String GIT_REPO_URL = "git.repo.url";
    public static final String GIT_CLONE_TO_DIRECTORY = "git.clone.to.directory";
    public static final String JSON_FILE_LOCATION = "json.file.location";

    // AWS S3
    public static final String AWS_DOWNLOAD_URL = "aws.download.url";
    public static final String AWS_BUCKET_NAME = "aws.bucket.name";
    public static final String AWS_ACCESS_KEY = "aws.data.1";
    public static final String AWS_SECRET_KEY = "aws.data.2";
}
