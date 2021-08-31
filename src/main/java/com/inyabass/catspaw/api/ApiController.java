package com.inyabass.catspaw.api;

import com.inyabass.catspaw.clients.KafkaConfig;
import com.inyabass.catspaw.clients.KafkaWriter;
import com.inyabass.catspaw.data.TestRequestModel;
import com.inyabass.catspaw.logging.Logger;
import com.inyabass.catspaw.util.Util;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.lang.invoke.MethodHandles;

@RestController
public class ApiController {

	private final static Logger logger = new Logger(MethodHandles.lookup().lookupClass());

	private String body = null;
	private TestRequestModel testRequestModel = null;
	private KafkaWriter kafkaWriter = new KafkaWriter();

	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping("/")
	public String post(@RequestBody String body) {
		logger.info("New Request in");
		if(body==null||body.equals("")) {
			logger.info("Empty or null body");
			throw new InvalidPayloadException("Empty or null body");
		}
		this.body = body;
		this.validateBody(this.body);
		this.processBody(this.body);
		return "{ \"status\": \"created\" }";
	}

	private void validateBody(String body) {
		try {
			this.testRequestModel = new TestRequestModel(body);
		} catch (Throwable t) {
			logger.info("Unable to parse JSON document body");
			throw new InvalidPayloadException("Unable to parse JSON document body");
		}
		String guid = null;
		try {
			guid = this.testRequestModel.getGuid();
		} catch (Throwable t) {
			guid = Util.getGuid();
			logger.info("[" + guid + "] assigning GUID " + guid);
			this.testRequestModel.addGuid(guid);
		}
		if(guid==null||guid.equals("")) {
			guid = Util.getGuid();
			logger.info("[" + guid + "] assigning GUID " + guid);
			this.testRequestModel.setGuid(guid);
		}
		String requestor = null;
		try {
			requestor = this.testRequestModel.getRequestor();
		} catch (Throwable t) {
			logger.info("[" + guid + "] requestor not found");
			throw new InvalidPayloadException("requestor not found");
		}
		if(requestor==null||requestor.equals("")) {
			logger.info("[" + guid + "] requestor null or blank");
			throw new InvalidPayloadException("requestor null or blank");
		}
		String timeRequested = null;
		try {
			timeRequested = this.testRequestModel.getTimeRequested();
		} catch (Throwable t) {
			logger.info("[" + guid + "] assigning timeRequested");
			timeRequested = Util.getTimeStampNow();
			this.testRequestModel.addTimeRequested(timeRequested);
		}
		if(timeRequested==null||timeRequested.equals("")) {
			logger.info("[" + guid + "] assigning timeRequested");
			timeRequested = Util.getTimeStampNow();
			this.testRequestModel.setTimeRequested(timeRequested);
		}
		if(!Util.isValidTimeStamp(timeRequested)) {
			logger.info("[" + guid + "] timeRequested invalid");
			throw new InvalidPayloadException("timeRequested invalid");
		}
		String status = null;
		try {
			status = this.testRequestModel.getStatus();
		} catch (Throwable t) {
			logger.info("[" + guid + "] status not found");
			throw new InvalidPayloadException("status not found");
		}
		if(status==null||!status.equals("new")) {
			logger.info("[" + guid + "] status must be 'new'");
			throw new InvalidPayloadException("status must be 'new'");
		}
	}

	public void processBody(String body) {

		try {
			this.kafkaWriter.write(KafkaConfig.TEST_REQUEST_TOPIC, this.testRequestModel.getGuid(), this.testRequestModel.export());
		} catch (Throwable t) {
			logger.info("Kafka write failed: " + t.getMessage());
			throw new InvalidPayloadException("Unable to write to Kafka");
		}
	}
}
