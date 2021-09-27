package com.inyabass.catspaw.config;

import java.time.Duration;

public class ConfigProperties {

    // Couchdb
    public static final String COUCHDB_SERVER = "couchdb.server";
    public static final String COUCHDB_CONFIG_DB = "couchdb.config.db";
    public static final String COUCHDB_DATA_DB = "couchdb.data.db";

    // Kafka
    public static final String BOOTSTRAP_SERVERS = "bootstrap.servers";
    public static final String KEY_SERIALIZER = "key.serializer";
    public static final String VALUE_SERIALIZER = "value.serializer";
    public static final String ACKS = "acks";
    public static final String RETRIES = "retries";
    public static final String CLIENT_ID = "client.id";
    public static final String GROUP_ID = "group.id";
    public static final String TEST_REQUEST_LISTENER_GROUP_ID = "test.request.listener.group.id";
    public static final String TEST_RESPONSE_LISTENER_GROUP_ID = "test.response.listener.group.id";
    public static final String POLL_DURATION = "poll.duration";
    public static final String ENABLE_AUTO_COMMIT = "enable.auto.commit";
    public static final String MAX_POLL_RECORDS = "max.poll.records";
    public static final String KEY_DESERIALIZER = "key.deserializer";
    public static final String VALUE_DESERIALIZER = "value.deserializer";
    public static final String AUTO_OFFSET_RESET = "auto.offset.reset";
    public static final String ALLOW_AUTO_CREATE_TOPICS = "allow.auto.create.topics";
    public static final String TEST_REQUEST_TOPIC = "test-request";
    public static final String TEST_RESPONSE_TOPIC = "test-response";

    // ScriptProcessor
    public static final String SCRIPTPROCESSOR_WORKING_DIRECTORY = "scriptprocessor.working.directory";

    // TestExecutor
    public static final String GIT_REPO_URL = "git.repo.url";
    public static final String GIT_CLONE_TO_DIRECTORY = "git.clone.to.directory";
    public static final String CONFIG_DIRECTORY = "repo.config.directory";
    public static final String JSON_FILE_LOCATION = "json.file.location";
    public static final String TESTEXECUTOR_DEBUG = "testexecutor.debug";

    // TestResponser
    public static final String TESTRESPONDER_DEBUG = "testresponder.debug";

    // AWS S3
    public static final String AWS_BUCKET_NAME = "aws.bucket.name";
    public static final String AWS_REPORTING_BUCKET_NAME = "aws.reporting.bucket.name";
    public static final String AWS_REGION_NAME = "aws.region.name";
    public static final String AWS_ACCESS_KEY = "aws.data.1";
    public static final String AWS_SECRET_KEY = "aws.data.2";
}
