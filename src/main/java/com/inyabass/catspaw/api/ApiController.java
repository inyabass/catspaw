package com.inyabass.catspaw.api;

import com.inyabass.catspaw.clients.KafkaWriter;
import com.inyabass.catspaw.config.ConfigProperties;
import com.inyabass.catspaw.data.JobStatusUpdateModel;
import com.inyabass.catspaw.data.TestRequestModel;
import com.inyabass.catspaw.listeners.ListenerHelper;
import com.inyabass.catspaw.logging.Logger;
import com.inyabass.catspaw.sqldata.SqlDataModel;
import com.inyabass.catspaw.util.Util;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.lang.invoke.MethodHandles;
import java.sql.Timestamp;
import java.util.Date;

@RestController
public class ApiController {

	private final static Logger logger = new Logger(MethodHandles.lookup().lookupClass());

	private String body = null;
	private TestRequestModel testRequestModel = null;
	private JobStatusUpdateModel jobStatusUpdateModel = null;
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

	@ResponseStatus(HttpStatus.OK)
	@PostMapping("/jobStatusUpdate")
	public String postJobStatusUpdate(@RequestBody String body) {
		logger.info("Status Update", "Starting");
		this.body = body;
		try {
			this.jobStatusUpdateModel = new JobStatusUpdateModel(body);
		} catch (Throwable t) {
			logger.info("Status", "Unable to parse JSON document body");
			throw new InvalidPayloadException("Unable to parse JSON document body");
		}
		try {
			this.guid = this.jobStatusUpdateModel.getGuid();
		} catch (Throwable t) {
			throw new InvalidPayloadException("Unable to get GUID from JSON");
		}
		if(this.guid==null||this.guid.equals("")) {
			logger.info(this.guid, "GUID Null or Blank");
			throw new InvalidPayloadException("GUID Null or Blank");
		}
		logger.info(this.guid, "Job Status Update");
		String status = null;
		try {
			status = this.jobStatusUpdateModel.getStatus();
		} catch (Throwable t) {
			logger.error(this.guid, "Unable to get status from JSON : " + t.getMessage());
			throw new InvalidPayloadException("Unable to get status from JSON");
		}
		if(status==null||status.equals("")) {
			logger.info(this.guid, "status Null or Blank");
			throw new InvalidPayloadException("status Null or Blank");
		}
		String statusMessage = null;
		try {
			statusMessage = this.jobStatusUpdateModel.getStatusMessage();
		} catch (Throwable t) {
			logger.error(this.guid, "Unable to get statusMessage from JSON : " + t.getMessage());
			throw new InvalidPayloadException("Unable to get statusMessage from JSON");
		}
		if(statusMessage==null||statusMessage.equals("")) {
			logger.info(this.guid, "statusMessage Null or Blank");
			throw new InvalidPayloadException("statusMessage Null or Blank");
		}
		logger.info(this.guid, "Updating status in cats.jobs");
		SqlDataModel sqlDataModel = null;
		try {
			sqlDataModel = new SqlDataModel();
		} catch (Throwable t) {
			logger.error(this.guid, "Unable to get SqlDataModel : " + t.getMessage());
			throw new InvalidPayloadException("Unable to get SqlDataModel : " + t.getMessage());
		}
		try {
			sqlDataModel.select("select * from cats.jobs where guid = '" + this.guid + "';");
		} catch (Throwable t) {
			logger.error(this.guid, "Unable to get data from cats.jobs : " + t.getMessage());
			throw new InvalidPayloadException("Unable to get data from cats.jobs : " + t.getMessage());
		}
		int rowCount = 0;
		try {
			rowCount = sqlDataModel.getRowCount();
		} catch (Throwable t) {
			logger.error(this.guid, "Unable to get rowcount from cats.jobs : " + t.getMessage());
			throw new InvalidPayloadException("Unable to get rowcount from cats.jobs : " + t.getMessage());
		}
		if(rowCount==0) {
			logger.error(this.guid, "Couldn't find GUID in cats.jobs");
			throw new InvalidPayloadException("Couldn't find GUID in cats.jobs");
		}
		try {
			sqlDataModel.moveFirst();
			sqlDataModel.setString("status", status);
			sqlDataModel.setString("statusMessage", statusMessage);
			sqlDataModel.updateCurrent();
		} catch (Throwable t) {
			logger.error(this.guid, "Unable to update cats.jobs : " + t.getMessage());
			throw new InvalidPayloadException("Unable to update cats.jobs : " + t.getMessage());
		}
		logger.info(this.guid, "Status Updated to '" + status + "' message '" + statusMessage + "'");
		return Util.buildSpringJsonResponse(HttpStatus.OK.value(), "Updated", this.guid);
	}

	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/job")
	public String getJobDetails(@RequestParam String jobNumber) {
		return "";
	}

	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/activeJobs")
	public String getActiveJobs() {
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
		String description = null;
		try {
			description = this.testRequestModel.getRequestor();
		} catch (Throwable t) {
			logger.info(this.guid, "description Not Found");
			throw new InvalidPayloadException("description Not Found");
		}
		if(description==null||description.equals("")) {
			logger.info(this.guid, "description Null or Blank");
			throw new InvalidPayloadException("description Null or Blank");
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
			Date date = Util.STANDARD_DATE_FORMAT.parse(this.testRequestModel.getTimeRequested());
			Timestamp timestamp = new Timestamp(date.getTime());
			sqlDataModel = new SqlDataModel();
			sqlDataModel.setString("guid", this.guid);
			sqlDataModel.setString("requestor", this.testRequestModel.getRequestor());
			sqlDataModel.setString("description", this.testRequestModel.getDescription());
			sqlDataModel.setTimeStamp("timeRequested", timestamp);
			sqlDataModel.setString("project", this.testRequestModel.getProject());
			sqlDataModel.setString("tagExpression", this.testRequestModel.getTagExpression());
			sqlDataModel.setString("branch", this.testRequestModel.getBranch());
			sqlDataModel.setString("configurationFile", this.testRequestModel.getConfigurationFile());
			sqlDataModel.setString("reports", this.testRequestModel.getReports());
			sqlDataModel.setString("emailTo", this.testRequestModel.getEmailTo());
			sqlDataModel.setString("status", TestRequestModel.STATUS_NEW);
			sqlDataModel.setString("statusMessage", "");
			sqlDataModel.insertNew("jobs");
			logger.info(this.guid, "Created cats.jobs record");
		} catch (Throwable throwable) {
			logger.error(this.guid, "Unable to create Job Record : " + throwable.getMessage());
			throwable.printStackTrace(System.out);
			return;
		}
	}
}
