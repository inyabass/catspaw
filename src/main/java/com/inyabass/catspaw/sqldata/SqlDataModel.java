package com.inyabass.catspaw.sqldata;

import com.inyabass.catspaw.clients.MySqlClient;
import com.inyabass.catspaw.logging.Logger;
import org.junit.Assert;

import java.lang.invoke.MethodHandles;
import java.security.InvalidParameterException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SqlDataModel {

    private final static Logger logger = new Logger(MethodHandles.lookup().lookupClass());

    private final static int STRING_TYPE = 0;
    private final static int INTEGER_TYPE = 1;
    private final static int DOUBLE_TYPE = 2;
    private final static int BOOLEAN_TYPE = 3;
    private final static int TIMESTAMP_TYPE = 4;

    private MySqlClient mySqlClient = null;
    private Map<String, Integer> fieldSpecs = null;
    private Map<String, Object> fields = null;
    private List<String> changedFields = null;
    private String table = null;
    private ResultSet resultSet = null;
    private int rowCount = 0;

    public SqlDataModel() throws Throwable {
        this.clear();
        this.mySqlClient = new MySqlClient();
    }

    public void clear() {
        this.fieldSpecs = new HashMap<>();
        this.fields = new HashMap<>();
        this.changedFields = new ArrayList<>();
        this.table = null;
        this.resultSet = null;
        this.rowCount = 0;
    }

    private void changed(String fieldName) {
        if(!this.changedFields.contains(fieldName)) {
            this.changedFields.add(fieldName);
        }
    }

    public String getString(String fieldName) throws Throwable {
        if(!this.fieldSpecs.containsKey(fieldName)) {
            throw new InvalidParameterException("Field " + fieldName + " is not present");
        }
        if(this.fieldSpecs.get(fieldName)!=SqlDataModel.STRING_TYPE) {
            throw new InvalidParameterException("Field " + fieldName + " is not defined as String");
        }
        return (String) this.fields.get(fieldName);
    }

    public void setString(String fieldName, String value) throws Throwable {
        if(this.resultSet==null) {
            if(this.fieldSpecs.containsKey(fieldName)) {
               if(this.fieldSpecs.get(fieldName)!=SqlDataModel.STRING_TYPE) {
                   throw new InvalidParameterException("Field " + fieldName + " is not defined as String");
               }
            } else {
                this.fieldSpecs.put(fieldName, SqlDataModel.STRING_TYPE);
            }
            if(this.fields.containsKey(fieldName)) {
                String oldValue = (String) this.fields.get(fieldName);
                if(!value.equals(oldValue)) {
                    this.fields.replace(fieldName, value);
                    this.changed(fieldName);
                }
            } else {
                this.fields.put(fieldName, value);
            }
        } else {
            if(!this.fieldSpecs.containsKey(fieldName)) {
                throw new InvalidParameterException("Field " + fieldName + " is not present");
            }
            if(this.fieldSpecs.get(fieldName)!=SqlDataModel.STRING_TYPE) {
                throw new InvalidParameterException("Field " + fieldName + " is not defined as String");
            }
            String oldValue = (String) this.fields.get(fieldName);
            if(!value.equals(oldValue)) {
                this.fields.replace(fieldName, value);
                this.changed(fieldName);
            }
        }
    }

    public int getInteger(String fieldName) throws Throwable {
        return 0;
    }

    public void setInteger(String fieldName, int value) throws Throwable {

    }

    public double getDouble(String fieldName) throws Throwable {
        return 0.0;
    }

    public void setDouble(String fieldName, double value) throws Throwable {

    }

    public boolean getBoolean(String fieldName) throws Throwable {
        return false;
    }

    public void setBoolean(String fieldName, boolean value) throws Throwable {

    }

    public long getTimeStamp(String fieldName) {
        return 0;
    }

    public void setTimeStamp(String fieldName) {

    }

    public void select(String selectStatement) throws Throwable {

    }

    public void update() throws Throwable {

    }

    public void insert() throws Throwable {

    }

    public void delete() throws Throwable {

    }

    public int getRowCount() throws Throwable {
        Assert.assertTrue("No Result Set - do a SELECT statement first", this.resultSet!=null);
        return this.rowCount;
    }

    public void moveFirst() throws Throwable {
        Assert.assertTrue("No Result Set - do a SELECT statement first", this.resultSet!=null);
        Assert.assertTrue("Query returned no results", this.rowCount>0);
        this.resultSet.first();
        this.mapFieldsFromTableRow();
    }

    public void moveLast() throws Throwable {
        Assert.assertTrue("No Result Set - do a SELECT statement first", this.resultSet!=null);
        Assert.assertTrue("Query returned no results", this.rowCount>0);
        this.resultSet.last();
        this.mapFieldsFromTableRow();
    }

    public void moveNext() throws Throwable {
        Assert.assertTrue("No Result Set - do a SELECT statement first", this.resultSet!=null);
        Assert.assertTrue("Query returned no results", this.rowCount>0);
        if(this.resultSet.isAfterLast()||this.resultSet.isBeforeFirst()) {
            throw new IllegalStateException("Invalid - was After Last or Before First");
        }
        if(this.resultSet.isLast()) {
            throw new IllegalStateException("Already on the Last Row");
        }
        this.resultSet.next();
        this.mapFieldsFromTableRow();
    }

    public void movePrevious() throws Throwable {
        Assert.assertTrue("No Result Set - do a SELECT statement first", this.resultSet!=null);
        Assert.assertTrue("Query returned no results", this.rowCount>0);
        if(this.resultSet.isAfterLast()||this.resultSet.isBeforeFirst()) {
            throw new IllegalStateException("Invalid - was After Last or Before First");
        }
        if(this.resultSet.isFirst()) {
            throw new IllegalStateException("Already on the First Row");
        }
        this.resultSet.previous();
        this.mapFieldsFromTableRow();
    }

    private void mapFieldsFromTableRow() {

    }
}
