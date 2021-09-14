package com.inyabass.catspaw.api;

import com.inyabass.catspaw.clients.KafkaWriter;
import com.inyabass.catspaw.config.ConfigProperties;
import com.inyabass.catspaw.data.TestRequestModel;
import com.inyabass.catspaw.logging.Logger;
import com.inyabass.catspaw.util.Util;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
		logger.info("New Request", "New Request for " + ConfigProperties.TEST_REQUEST_TOPIC);
		this.body = body;
		this.validateBody(this.body);
		this.processBody(this.body);
		return Util.buildSpringJsonResponse(HttpStatus.CREATED.value(), "Created");
	}

	private void validateBody(String body) {
		try {
			this.testRequestModel = new TestRequestModel(body);
		} catch (Throwable t) {
			logger.info("New Request", "Unable to parse JSON document body");
			throw new InvalidPayloadException("Unable to parse JSON document body");
		}
		String guid = null;
		try {
			guid = this.testRequestModel.getGuid();
		} catch (Throwable t) {
			guid = Util.getGuid();
			logger.info(guid, "assigning GUID " + guid);
			this.testRequestModel.addGuid(guid);
		}
		if(guid==null||guid.equals("")) {
			logger.info(guid, "guid Null or Blank");
			throw new InvalidPayloadException("guid Null or Blank");
		}
		String requestor = null;
		try {
			requestor = this.testRequestModel.getRequestor();
		} catch (Throwable t) {
			logger.info(guid, "requestor Not Found");
			throw new InvalidPayloadException("requestor Not Found");
		}
		if(requestor==null||requestor.equals("")) {
			logger.info(guid, "requestor Null or Blank");
			throw new InvalidPayloadException("requestor Null or Blank");
		}
		String timeRequested = null;
		try {
			timeRequested = this.testRequestModel.getTimeRequested();
		} catch (Throwable t) {
			timeRequested = Util.getStandardTimeStampNow();
			logger.info(guid, "assigning timeRequested " + timeRequested);
			this.testRequestModel.addTimeRequested(timeRequested);
		}
		if(timeRequested==null||timeRequested.equals("")) {
			logger.info(guid, "timeRequested Null or Blank");
			throw new InvalidPayloadException("timeRequested Null or Blank");
		}
		if(!Util.isValidTimeStamp(timeRequested)) {
			logger.info(guid, "Invalid timeRequested");
			throw new InvalidPayloadException("Invalid timeRequested");
		}
		String status = null;
		try {
			status = this.testRequestModel.getStatus();
		} catch (Throwable t) {
			logger.info(guid, "status Not Found");
			throw new InvalidPayloadException("Status not Found");
		}
		if(status==null||!status.equals("new")) {
			logger.info(guid, "status Must Be 'new'");
			throw new InvalidPayloadException("Status Must Be 'new'");
		}
		String tagExpression = null;
		try {
			tagExpression = this.testRequestModel.getTagExpression();
		} catch (Throwable t) {
			logger.info(guid, "tagExpression Not Found");
			throw new InvalidPayloadException("tagExpression not Found");
		}
	}

	public void processBody(String body) {
		try {
			this.kafkaWriter.write(ConfigProperties.TEST_REQUEST_TOPIC, this.testRequestModel.getGuid(), this.testRequestModel.export());
		} catch (Throwable t) {
			logger.info(this.testRequestModel.getGuid(), "Kafka write failed: " + t.getMessage());
			throw new InvalidPayloadException("Unable to write to Kafka");
		}
	}
}
