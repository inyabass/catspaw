package com.inyabass.catspaw.listeners;

import com.inyabass.catspaw.clients.Couchdb;
import com.inyabass.catspaw.config.ConfigProperties;
import com.inyabass.catspaw.config.ConfigReader;
import com.inyabass.catspaw.data.TestRequestModel;
import com.inyabass.catspaw.logging.Logger;
import org.springframework.http.HttpStatus;

public class ListenerHelper {

    public static boolean putCouchdbCatsEntry(Logger logger, TestRequestModel testRequestModel) {
        Couchdb couchdb = null;
        String guid = testRequestModel.getGuid();
        try {
            couchdb = new Couchdb(ConfigReader.get(ConfigProperties.COUCHDB_SERVER));
        } catch (Throwable t) {
            logger.error(guid, "(PUT) Unable to create Couchdb client: " + t.getMessage());
            return false;
        }
        try {
            couchdb.put(guid, testRequestModel.export());
            if(couchdb.getHttpStatus()!=HttpStatus.CREATED.value()) {
                logger.error(guid, "Unable to PUT couchdb record: " + couchdb.getHttpStatus() + " " + couchdb.getHttpStatusMessage());
                return false;
            }
        } catch (Throwable t) {
            logger.error(guid, "Unable to PUT couchdb record: " + t.getMessage());
            return false;
        }
        return true;
    }

    public static boolean postCouchdbCatsEntry(Logger logger, TestRequestModel testRequestModel) {
        Couchdb couchdb = null;
        String guid = testRequestModel.getGuid();
        try {
            couchdb = new Couchdb(ConfigReader.get(ConfigProperties.COUCHDB_SERVER));
        } catch (Throwable t) {
            logger.error(guid, "(POST) Unable to create Couchdb client: " + t.getMessage());
            return false;
        }
        try {
            couchdb.post(guid, testRequestModel.export());
            if(couchdb.getHttpStatus()!=HttpStatus.CREATED.value()) {
                logger.error(guid, "Unable to POST couchdb record: " + couchdb.getHttpStatus() + " " + couchdb.getHttpStatusMessage());
                return false;
            }
        } catch (Throwable t) {
            return false;
        }
        return true;
    }

    public static boolean updateCouchdbCatsEntry(Logger logger, TestRequestModel testRequestModel) {
        Couchdb couchdb = null;
        String guid = testRequestModel.getGuid();
        try {
            couchdb = new Couchdb(ConfigReader.get(ConfigProperties.COUCHDB_SERVER));
        } catch (Throwable t) {
            logger.error(guid, "(GET) Unable to create Couchdb client: " + t.getMessage());
            return false;
        }
        try {
            couchdb.get(testRequestModel.getGuid());
            if(couchdb.getHttpStatus()!=HttpStatus.OK.value()) {
                logger.error("[" + guid + "] Unable to GET couchdb record: " + couchdb.getHttpStatus() + " " + couchdb.getHttpStatusMessage());
                return false;
            }
        } catch (Throwable t) {
            logger.error(guid, "Unable to GET couchdb record: " + t.getMessage());
            return false;
        }
        TestRequestModel existingTestRequestModel = null;
        try {
            existingTestRequestModel = new TestRequestModel(couchdb.getBodyAsJsonString());
        } catch (Throwable t) {
            logger.error(guid, "Unable to Parse JSON body of couchdb record: " + t.getMessage());
            return false;
        }
        String _id = null;
        try {
            _id = existingTestRequestModel.get_id();
        } catch (Throwable t) {
            logger.error(guid, "Unable to GET _id of couchdb record: " + t.getMessage());
            return false;
        }
        try {
            testRequestModel.get_id();
            testRequestModel.set_id(_id);
        } catch (Throwable t) {
            try {
                testRequestModel.add_id(_id);
            } catch (Throwable t2) {
                logger.error(guid, "Unable to add _id of couchdb record: " + t2.getMessage());
                return false;
            }
        }
        String _rev = null;
        try {
            _rev = existingTestRequestModel.get_rev();
        } catch (Throwable t) {
            logger.error(guid, "Unable to GET _rev of couchdb record: " + t.getMessage());
            return false;
        }
        try {
            testRequestModel.get_rev();
            testRequestModel.set_rev(_rev);
        } catch (Throwable t) {
            try {
                testRequestModel.add_rev(_rev);
            } catch (Throwable t2) {
                logger.error(guid, "Unable to add _rev of couchdb record: " + t2.getMessage());
                return false;
            }
        }
        return putCouchdbCatsEntry(logger, testRequestModel);
    }
}
