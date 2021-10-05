package com.inyabass.catspaw.api;

import com.inyabass.catspaw.clients.KafkaWriter;
import com.inyabass.catspaw.config.ConfigProperties;
import com.inyabass.catspaw.data.TestRequestModel;
import com.inyabass.catspaw.logging.Logger;
import com.inyabass.catspaw.sqldata.SqlDataModel;
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
	private String guid = null;

	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping("/request")
	public String postRequest(@RequestBody String body) {
		logger.info("New Request", "New Request for " + ConfigProperties.TEST_REQUEST_TOPIC);
		this.body = body;
		this.validateTestRequestBody(this.body);
		this.processTestRequestBody(this.body);
		return Util.buildSpringJsonResponse(HttpStatus.CREATED.value(), "Created", this.guid);
	}

	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping("/jobstatusupdate")
	public String postJobStatusUpdate(@RequestBody String body) {
		logger.info("Status Update", "");
		this.body = body;
		this.validateTestRequestBody(this.body);
		logger.info(this.guid, "Job Status Update");
		return Util.buildSpringJsonResponse(HttpStatus.CREATED.value(), "Created", this.guid);
	}

	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/job")
	public String jobGetRequest(@RequestParam String jobNumber) {
		return "";
	}

	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/currentjobs")
	public String jobGetCurrentRequests() {
		return "";
	}

	private void validateTestRequestBody(String body) {
		try {
			this.testRequestModel = new TestRequestModel(body);
		} catch (Throwable t) {
			logger.info("New Request", "Unable to parse JSON document body");
			throw new InvalidPayloadException("Unable to parse JSON document body");
		}
		try {
			this.guid = this.testRequestModel.getGuid();
		} catch (Throwable t) {
			this.guid = Util.getGuid();
			logger.info(this.guid, "assigning GUID " + this.guid);
			this.testRequestModel.addGuid(this.guid);
		}
		if(this.guid==null||this.guid.equals("")) {
			logger.info(this.guid, "guid Null or Blank");
			throw new InvalidPayloadException("guid Null or Blank");
		}
		String requestor = null;
		try {
			requestor = this.testRequestModel.getRequestor();
		} catch (Throwable t) {
			logger.info(this.guid, "requestor Not Found");
			throw new InvalidPayloadException("requestor Not Found");
		}
		if(requestor==null||requestor.equals("")) {
			logger.info(this.guid, "requestor Null or Blank");
			throw new InvalidPayloadException("requestor Null or Blank");
		}
		String project = null;
		try {
			project = this.testRequestModel.getProject();
		} catch (Throwable t) {
			logger.info(this.guid, "project Not Found");
			throw new InvalidPayloadException("project Not Found");
		}
		if(project==null||project.equals("")) {
			logger.info(this.guid, "project Null or Blank");
			throw new InvalidPayloadException("project Null or Blank");
		}
		String timeRequested = null;
		try {
			timeRequested = this.testRequestModel.getTimeRequested();
		} catch (Throwable t) {
			timeRequested = Util.getStandardTimeStampNow();
			logger.info(this.guid, "assigning timeRequested " + timeRequested);
			this.testRequestModel.addTimeRequested(timeRequested);
		}
		if(timeRequested==null||timeRequested.equals("")) {
			logger.info(this.guid, "timeRequested Null or Blank");
			throw new InvalidPayloadException("timeRequested Null or Blank");
		}
		if(!Util.isValidTimeStamp(timeRequested)) {
			logger.info(this.guid, "Invalid timeRequested");
			throw new InvalidPayloadException("Invalid timeRequested");
		}
		String tagExpression = null;
		try {
			tagExpression = this.testRequestModel.getTagExpression();
		} catch (Throwable t) {
			logger.info(this.guid, "tagExpression Not Found");
			throw new InvalidPayloadException("tagExpression not Found");
		}
		if(tagExpression==null||tagExpression.equals("")) {
			logger.info(this.guid, "tagExpression Null or Blank");
			throw new InvalidPayloadException("tagExpression Null or Blank");
		}
		String reports = null;
		try {
			reports = this.testRequestModel.getReports();
		} catch (Throwable t) {
			logger.info(this.guid, "output.report.reports Not Found");
			throw new InvalidPayloadException("output.report.reports not Found");
		}
		if(reports==null||reports.equals("")) {
			logger.info(this.guid, "output.report.reports Null or Blank");
			throw new InvalidPayloadException("output.report.reports Null or Blank");
		}
	}

	public void processTestRequestBody(String body) {
		try {
			this.kafkaWriter.write(ConfigProperties.TEST_REQUEST_TOPIC, this.testRequestModel.getGuid(), this.testRequestModel.export());
		} catch (Throwable t) {
			logger.info(this.testRequestModel.getGuid(), "Kafka write failed: " + t.getMessage());
			throw new InvalidPayloadException("Unable to write to Kafka");
		}
		this.createJobRecord();
	}

	public void createJobRecord() {
		SqlDataModel sqlDataModel = null;
		try {
			sqlDataModel = new SqlDataModel();
			sqlDataModel.setString("guid", this.guid);
			sqlDataModel.setString("requestor", this.testRequestModel.getRequestor());
			sqlDataModel.setTimeStamp("timeRequested", Long.parseLong(this.testRequestModel.getTimeRequested()));
			sqlDataModel.setString("project", this.testRequestModel.getProject());
			sqlDataModel.setString("tagExpression", this.testRequestModel.getTagExpression());
			sqlDataModel.setString("branch", this.testRequestModel.getBranch());
			sqlDataModel.setString("configurationFile", this.testRequestModel.getConfigurationFile());
			sqlDataModel.setString("reports", this.testRequestModel.getReports());
			sqlDataModel.setString("emailTo", this.testRequestModel.getEmailTo());
			sqlDataModel.setString("status", "new");
			sqlDataModel.setString("statusMessage", "");
			sqlDataModel.insertNew("jobs");
		} catch (Throwable throwable) {
			logger.error(this.guid, "Unable to create Job Record : " + throwable.getMessage());
			return;
		}
	}
}
