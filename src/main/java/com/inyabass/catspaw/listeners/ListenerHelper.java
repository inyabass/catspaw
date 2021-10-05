package com.inyabass.catspaw.listeners;

import com.inyabass.catspaw.api.InvalidPayloadException;
import com.inyabass.catspaw.logging.Logger;
import com.inyabass.catspaw.sqldata.SqlDataModel;

public class ListenerHelper {

    public static void jobStatusUpdate(Logger logger, String guid, String status, String statusMessage) {
        logger.info(guid, "Updating status in cats.jobs");
        SqlDataModel sqlDataModel = null;
        try {
            sqlDataModel = new SqlDataModel();
        } catch (Throwable t) {
            logger.error(guid, "Unable to get SqlDataModel : " + t.getMessage());
            throw new InvalidPayloadException("Unable to get SqlDataModel : " + t.getMessage());
        }
        try {
            sqlDataModel.select("select * from cats.jobs where guid = '" + guid + "';");
        } catch (Throwable t) {
            logger.error(guid, "Unable to get data from cats.jobs : " + t.getMessage());
            throw new InvalidPayloadException("Unable to get data from cats.jobs : " + t.getMessage());
        }
        int rowCount = 0;
        try {
            rowCount = sqlDataModel.getRowCount();
        } catch (Throwable t) {
            logger.error(guid, "Unable to get rowcount from cats.jobs : " + t.getMessage());
            throw new InvalidPayloadException("Unable to get rowcount from cats.jobs : " + t.getMessage());
        }
        if(rowCount==0) {
            logger.error(guid, "Couldn't find GUID in cats.jobs");
            throw new InvalidPayloadException("Couldn't find GUID in cats.jobs");
        }
        try {
            sqlDataModel.moveFirst();
            sqlDataModel.setString("status", status);
            sqlDataModel.setString("statusMessage", statusMessage);
            sqlDataModel.updateCurrent();
        } catch (Throwable t) {
            logger.error(guid, "Unable to update cats.jobs : " + t.getMessage());
            throw new InvalidPayloadException("Unable to update cats.jobs : " + t.getMessage());
        }
        logger.info(guid, "Status Updated to '" + status + "' message '" + statusMessage + "'");
    }
}
