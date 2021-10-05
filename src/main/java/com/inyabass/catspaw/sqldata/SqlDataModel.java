package com.inyabass.catspaw.sqldata;

import com.inyabass.catspaw.clients.MySqlClient;
import com.inyabass.catspaw.logging.Logger;
import org.junit.Assert;

import java.lang.invoke.MethodHandles;
import java.security.InvalidParameterException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.util.*;

public class SqlDataModel {

    private final static Logger logger = new Logger(MethodHandles.lookup().lookupClass());

    private final static int STRING_TYPE = 12;
    private final static int INTEGER_TYPE = 4;
    private final static int DOUBLE_TYPE = 8;
    private final static int BOOLEAN_TYPE = -6;
    private final static int TIMESTAMP_TYPE = 93;

    private MySqlClient mySqlClient = null;
    private Map<String, Integer> fieldSpecs = null;
    private Map<String, Object> fields = null;
    private List<String> changedFields = null;
    private String table = null;
    private ResultSet resultSet = null;
    private int rowCount = 0;

    public static void main(String[] args) throws Throwable {
    }

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
        if(this.fields.containsKey(fieldName)) {
            if(this.fields.get(fieldName)==null) {
                return null;
            }
            return (String) this.fields.get(fieldName);
        } else {
            throw new InvalidParameterException("Field " + fieldName + " currently has no value");
        }
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
                if(this.fields.get(fieldName)!=null) {
                    String oldValue = (String) this.fields.get(fieldName);
                    if(value!=null) {
                        if (!value.equals(oldValue)) {
                            this.fields.replace(fieldName, value);
                            this.changed(fieldName);
                        }
                    } else {
                        this.fields.replace(fieldName, value);
                        this.changed(fieldName);
                    }
                } else {
                    if(value!=null) {
                        this.fields.replace(fieldName, value);
                        this.changed(fieldName);
                    }
                }
            } else {
                this.fields.put(fieldName, value);
                this.changed(fieldName);
            }
        } else {
            if(!this.fieldSpecs.containsKey(fieldName)) {
                throw new InvalidParameterException("Field " + fieldName + " is not present");
            }
            if(this.fieldSpecs.get(fieldName)!=SqlDataModel.STRING_TYPE) {
                throw new InvalidParameterException("Field " + fieldName + " is not defined as String");
            }
            if(this.fields.containsKey(fieldName)) {
                if(this.fields.get(fieldName)!=null) {
                    String oldValue = (String) this.fields.get(fieldName);
                    if(value!=null) {
                        if (!value.equals(oldValue)) {
                            this.fields.replace(fieldName, value);
                            this.changed(fieldName);
                        }
                    } else {
                        this.fields.replace(fieldName, value);
                        this.changed(fieldName);
                    }
                } else {
                    if(value!=null) {
                        this.fields.replace(fieldName, value);
                        this.changed(fieldName);
                    }
                }
            } else {
                this.fields.put(fieldName, value);
                this.changed(fieldName);
            }
        }
    }

    public Integer getInteger(String fieldName) throws Throwable {
        if(!this.fieldSpecs.containsKey(fieldName)) {
            throw new InvalidParameterException("Field " + fieldName + " is not present");
        }
        if(this.fieldSpecs.get(fieldName)!=SqlDataModel.INTEGER_TYPE) {
            throw new InvalidParameterException("Field " + fieldName + " is not defined as Integer");
        }
        if(this.fields.containsKey(fieldName)) {
            if(this.fields.get(fieldName)==null) {
                return null;
            }
            return (Integer) this.fields.get(fieldName);
        } else {
            throw new InvalidParameterException("Field " + fieldName + " currently has no value");
        }
    }

    public void setInteger(String fieldName, Integer value) throws Throwable {
        if(this.resultSet==null) {
            if(this.fieldSpecs.containsKey(fieldName)) {
                if(this.fieldSpecs.get(fieldName)!=SqlDataModel.INTEGER_TYPE) {
                    throw new InvalidParameterException("Field " + fieldName + " is not defined as Integer");
                }
            } else {
                this.fieldSpecs.put(fieldName, SqlDataModel.INTEGER_TYPE);
            }
            if(this.fields.containsKey(fieldName)) {
                if(this.fields.get(fieldName)!=null) {
                    Integer oldValue = (Integer) this.fields.get(fieldName);
                    if(value!=null) {
                        if (!value.equals(oldValue)) {
                            this.fields.replace(fieldName, value);
                            this.changed(fieldName);
                        }
                    } else {
                        this.fields.replace(fieldName, value);
                        this.changed(fieldName);
                    }
                } else {
                    if(value!=null) {
                        this.fields.replace(fieldName, value);
                        this.changed(fieldName);
                    }
                }
            } else {
                this.fields.put(fieldName, value);
                this.changed(fieldName);
            }
        } else {
            if(!this.fieldSpecs.containsKey(fieldName)) {
                throw new InvalidParameterException("Field " + fieldName + " is not present");
            }
            if(this.fieldSpecs.get(fieldName)!=SqlDataModel.INTEGER_TYPE) {
                throw new InvalidParameterException("Field " + fieldName + " is not defined as Integer");
            }
            if(this.fields.containsKey(fieldName)) {
                if(this.fields.get(fieldName)!=null) {
                    Integer oldValue = (Integer) this.fields.get(fieldName);
                    if(value!=null) {
                        if (!value.equals(oldValue)) {
                            this.fields.replace(fieldName, value);
                            this.changed(fieldName);
                        }
                    } else {
                        this.fields.replace(fieldName, value);
                        this.changed(fieldName);
                    }
                } else {
                    if(value!=null) {
                        this.fields.replace(fieldName, value);
                        this.changed(fieldName);
                    }
                }
            } else {
                this.fields.put(fieldName, value);
                this.changed(fieldName);
            }
        }
    }

    public Double getDouble(String fieldName) throws Throwable {
        if(!this.fieldSpecs.containsKey(fieldName)) {
            throw new InvalidParameterException("Field " + fieldName + " is not present");
        }
        if(this.fieldSpecs.get(fieldName)!=SqlDataModel.DOUBLE_TYPE) {
            throw new InvalidParameterException("Field " + fieldName + " is not defined as Double");
        }
        if(this.fields.containsKey(fieldName)) {
            if(this.fields.get(fieldName)==null) {
                return null;
            }
            return (Double) this.fields.get(fieldName);
        } else {
            throw new InvalidParameterException("Field " + fieldName + " currently has no value");
        }
    }

    public void setDouble(String fieldName, Double value) throws Throwable {
        if(this.resultSet==null) {
            if(this.fieldSpecs.containsKey(fieldName)) {
                if(this.fieldSpecs.get(fieldName)!=SqlDataModel.DOUBLE_TYPE) {
                    throw new InvalidParameterException("Field " + fieldName + " is not defined as Double");
                }
            } else {
                this.fieldSpecs.put(fieldName, SqlDataModel.DOUBLE_TYPE);
            }
            if(this.fields.containsKey(fieldName)) {
                if(this.fields.get(fieldName)!=null) {
                    Double oldValue = (Double) this.fields.get(fieldName);
                    if(value!=null) {
                        if (!value.equals(oldValue)) {
                            this.fields.replace(fieldName, value);
                            this.changed(fieldName);
                        }
                    } else {
                        this.fields.replace(fieldName, value);
                        this.changed(fieldName);
                    }
                } else {
                    if(value!=null) {
                        this.fields.replace(fieldName, value);
                        this.changed(fieldName);
                    }
                }
            } else {
                this.fields.put(fieldName, value);
                this.changed(fieldName);
            }
        } else {
            if(!this.fieldSpecs.containsKey(fieldName)) {
                throw new InvalidParameterException("Field " + fieldName + " is not present");
            }
            if(this.fieldSpecs.get(fieldName)!=SqlDataModel.DOUBLE_TYPE) {
                throw new InvalidParameterException("Field " + fieldName + " is not defined as Double");
            }
            if(this.fields.containsKey(fieldName)) {
                if(this.fields.get(fieldName)!=null) {
                    Double oldValue = (Double) this.fields.get(fieldName);
                    if(value!=null) {
                        if (!value.equals(oldValue)) {
                            this.fields.replace(fieldName, value);
                            this.changed(fieldName);
                        }
                    } else {
                        this.fields.replace(fieldName, value);
                        this.changed(fieldName);
                    }
                } else {
                    if(value!=null) {
                        this.fields.replace(fieldName, value);
                        this.changed(fieldName);
                    }
                }
            } else {
                this.fields.put(fieldName, value);
                this.changed(fieldName);
            }
        }
    }

    public Boolean getBoolean(String fieldName) throws Throwable {
        if(!this.fieldSpecs.containsKey(fieldName)) {
            throw new InvalidParameterException("Field " + fieldName + " is not present");
        }
        if(this.fieldSpecs.get(fieldName)!=SqlDataModel.BOOLEAN_TYPE) {
            throw new InvalidParameterException("Field " + fieldName + " is not defined as Boolean");
        }
        if(this.fields.containsKey(fieldName)) {
            if(this.fields.get(fieldName)==null) {
                return null;
            }
            if((Integer) this.fields.get(fieldName)==1) {
                return true;
            } else {
                return false;
            }
        } else {
            throw new InvalidParameterException("Field " + fieldName + " currently has no value");
        }
    }

    public void setBoolean(String fieldName, Boolean value) throws Throwable {
        Integer valueAsInteger = null;
        if(value!=null) {
            if(value) {
                valueAsInteger = 1;
            } else {
                valueAsInteger = 0;
            }
        }
        if(this.resultSet==null) {
            if(this.fieldSpecs.containsKey(fieldName)) {
                if(this.fieldSpecs.get(fieldName)!=SqlDataModel.BOOLEAN_TYPE) {
                    throw new InvalidParameterException("Field " + fieldName + " is not defined as Boolean");
                }
            } else {
                this.fieldSpecs.put(fieldName, SqlDataModel.BOOLEAN_TYPE);
            }
            if(this.fields.containsKey(fieldName)) {
                if(this.fields.get(fieldName)!=null) {
                    Integer oldValue = (Integer) this.fields.get(fieldName);
                    if(value!=null) {
                        if (!valueAsInteger.equals(oldValue)) {
                            this.fields.replace(fieldName, valueAsInteger);
                            this.changed(fieldName);
                        }
                    } else {
                        this.fields.replace(fieldName, valueAsInteger);
                        this.changed(fieldName);
                    }
                } else {
                    if(value!=null) {
                        this.fields.replace(fieldName, valueAsInteger);
                        this.changed(fieldName);
                    }
                }
            } else {
                this.fields.put(fieldName, valueAsInteger);
                this.changed(fieldName);
            }
        } else {
            if(!this.fieldSpecs.containsKey(fieldName)) {
                throw new InvalidParameterException("Field " + fieldName + " is not present");
            }
            if(this.fieldSpecs.get(fieldName)!=SqlDataModel.BOOLEAN_TYPE) {
                throw new InvalidParameterException("Field " + fieldName + " is not defined as Boolean");
            }
            if(this.fields.containsKey(fieldName)) {
                if(this.fields.get(fieldName)!=null) {
                    Integer oldValue = (Integer) this.fields.get(fieldName);
                    if(value!=null) {
                        if (!valueAsInteger.equals(oldValue)) {
                            this.fields.replace(fieldName, valueAsInteger);
                            this.changed(fieldName);
                        }
                    } else {
                        this.fields.replace(fieldName, valueAsInteger);
                        this.changed(fieldName);
                    }
                } else {
                    if(value!=null) {
                        this.fields.replace(fieldName, valueAsInteger);
                        this.changed(fieldName);
                    }
                }
            } else {
                this.fields.put(fieldName, valueAsInteger);
                this.changed(fieldName);
            }
        }
    }

    public Timestamp getTimeStamp(String fieldName) {
        if(!this.fieldSpecs.containsKey(fieldName)) {
            throw new InvalidParameterException("Field " + fieldName + " is not present");
        }
        if(this.fieldSpecs.get(fieldName)!=SqlDataModel.TIMESTAMP_TYPE) {
            throw new InvalidParameterException("Field " + fieldName + " is not defined as Timestamp");
        }
        if(this.fields.containsKey(fieldName)) {
            if(this.fields.get(fieldName)==null) {
                return null;
            }
            return (Timestamp) this.fields.get(fieldName);
        } else {
            throw new InvalidParameterException("Field " + fieldName + " currently has no value");
        }
    }

    public void setTimeStamp(String fieldName, Object value) throws Throwable {
        Timestamp valueAsTimestamp = null;
        if(value instanceof Timestamp) {
            valueAsTimestamp = (Timestamp) value;
        } else {
            if(value instanceof Long) {
                valueAsTimestamp = new Timestamp((Long) value);
            } else {
                throw new InvalidParameterException("Value was neither a Timestamp nor a Long");
            }
        }
        if(this.resultSet==null) {
            if(this.fieldSpecs.containsKey(fieldName)) {
                if(this.fieldSpecs.get(fieldName)!=SqlDataModel.TIMESTAMP_TYPE) {
                    throw new InvalidParameterException("Field " + fieldName + " is not defined as Timestamp");
                }
            } else {
                this.fieldSpecs.put(fieldName, SqlDataModel.TIMESTAMP_TYPE);
            }
            if(this.fields.containsKey(fieldName)) {
                if(this.fields.get(fieldName)!=null) {
                    Timestamp oldValue = (Timestamp) this.fields.get(fieldName);
                    if(value!=null) {
                        if (!value.equals(oldValue)) {
                            this.fields.replace(fieldName, valueAsTimestamp);
                            this.changed(fieldName);
                        }
                    } else {
                        this.fields.replace(fieldName, valueAsTimestamp);
                        this.changed(fieldName);
                    }
                } else {
                    if(value!=null) {
                        this.fields.replace(fieldName, valueAsTimestamp);
                        this.changed(fieldName);
                    }
                }
            } else {
                this.fields.put(fieldName, valueAsTimestamp);
                this.changed(fieldName);
            }
        } else {
            if(!this.fieldSpecs.containsKey(fieldName)) {
                throw new InvalidParameterException("Field " + fieldName + " is not present");
            }
            if(this.fieldSpecs.get(fieldName)!=SqlDataModel.TIMESTAMP_TYPE) {
                throw new InvalidParameterException("Field " + fieldName + " is not defined as Timestamp");
            }
            if(this.fields.containsKey(fieldName)) {
                if(this.fields.get(fieldName)!=null) {
                    Timestamp oldValue = (Timestamp) this.fields.get(fieldName);
                    if(value!=null) {
                        if (!value.equals(oldValue)) {
                            this.fields.replace(fieldName, valueAsTimestamp);
                            this.changed(fieldName);
                        }
                    } else {
                        this.fields.replace(fieldName, valueAsTimestamp);
                        this.changed(fieldName);
                    }
                } else {
                    if(value!=null) {
                        this.fields.replace(fieldName, valueAsTimestamp);
                        this.changed(fieldName);
                    }
                }
            } else {
                this.fields.put(fieldName, valueAsTimestamp);
                this.changed(fieldName);
            }
        }
    }

    public void select(String selectStatement) throws Throwable {
        this.resultSet = mySqlClient.execute(selectStatement);
        Assert.assertNotNull("No ResultSet Returned from SQL Select Statement", this.resultSet);
        if(this.resultSet.last()) {
            this.rowCount = this.resultSet.getRow();
        } else {
            this.rowCount = 0;
        }
        this.createFieldMap();
    }

    public void updateCurrent() throws Throwable {
        Assert.assertTrue("No Result Set - do a SELECT statement first", this.resultSet!=null);
        Assert.assertTrue("No Current Row - Empty ResultSet", this.rowCount>0);
        if(this.resultSet.isAfterLast()||this.resultSet.isBeforeFirst()) {
            throw new IllegalStateException("Invalid - was After Last or Before First");
        }
        Assert.assertTrue("No Current Row", this.resultSet.getRow()>0);
        if(this.changedFields.size()==0) {
            System.out.println("No changes");
            return;
        }
        String primaryKey = mySqlClient.getPrimaryKeyForTable(table);
        String primaryKeyValue = this.getString(primaryKey);
        String schema = this.mySqlClient.getSchema();
        String sql = "UPDATE `" + schema + "`.`" + table + "` SET ";
        for(int i = 0;i<this.changedFields.size();i++) {
            String fieldName = this.changedFields.get(i);
            if(i<this.changedFields.size() - 1) {
                sql += "`" + fieldName + "` = " + this.makeLiteralForType(this.fieldSpecs.get(fieldName), this.fields.get(fieldName)) + ", ";
            } else {
                sql += "`" + fieldName + "` = " + this.makeLiteralForType(this.fieldSpecs.get(fieldName), this.fields.get(fieldName));
            }
        }
        sql += ";";
        this.mySqlClient.executeNoResult(sql);
    }

    public void insertNew(String table) throws Throwable {
        String schema = this.mySqlClient.getSchema();
        Set<String> keys = this.fields.keySet();
        String sql = "INSERT INTO `" + schema + "`.`" + table + "` (";
        int i =0;
        for(String key: keys) {
           i++;
            if (i < keys.size()) {
                sql += "`" + key + "`, ";
            } else {
                sql += "`" + key + "`";
            }
        }
        sql += ") VALUES (";
        i = 0;
        for(String key: keys) {
            i++;
            if (i < keys.size()) {
                sql += this.makeLiteralForType(this.fieldSpecs.get(key), this.fields.get(key)) + ", ";
            } else {
                sql += this.makeLiteralForType(this.fieldSpecs.get(key), this.fields.get(key));
            }
        }
        sql += ");";
        this.mySqlClient.executeNoResult(sql);
    }

    private String makeLiteralForType(int type, Object value) {
        if(value==null) {
            return "null";
        }
        String literal = "";
        switch(type) {
            case STRING_TYPE: {
                literal = "'" + value + "'";
                break;
            }
            case TIMESTAMP_TYPE: {
                Timestamp timestamp = new Timestamp((Long) value);
                literal = "'" + timestamp.toString() + "'";
                break;
            }
            default: {
                literal = value.toString();
                break;
            }
        }
        return literal;
    }

    public void deleteCurrent() throws Throwable {
        Assert.assertTrue("No Result Set - do a SELECT statement first", this.resultSet!=null);
        Assert.assertTrue("No Current Row - Empty ResultSet", this.rowCount>0);
        if(this.resultSet.isAfterLast()||this.resultSet.isBeforeFirst()) {
            throw new IllegalStateException("Invalid - was After Last or Before First");
        }
        Assert.assertTrue("No Current Row", this.resultSet.getRow()>0);
        String primaryKey = mySqlClient.getPrimaryKeyForTable(table);
        String primaryKeyValue = this.getString(primaryKey);
        String schema = this.mySqlClient.getSchema();
        String sql = "DELETE FROM `" + schema + "`.`" + table + "` WHERE `" + primaryKey + "` = '" + primaryKeyValue + "';";
        this.mySqlClient.executeNoResult(sql);
    }

    public int getRowCount() throws Throwable {
        Assert.assertTrue("No Result Set - do a SELECT statement first", this.resultSet!=null);
        return this.rowCount;
    }

    public void moveFirst() throws Throwable {
        Assert.assertTrue("No Result Set - do a SELECT statement first", this.resultSet!=null);
        Assert.assertTrue("Query returned no results", this.rowCount>0);
        if(this.resultSet.first()) {
            this.mapFieldsFromTableRow();
        } else {
            Assert.fail("Unable to Move to First Row");
        }
    }

    public void moveLast() throws Throwable {
        Assert.assertTrue("No Result Set - do a SELECT statement first", this.resultSet!=null);
        Assert.assertTrue("Query returned no results", this.rowCount>0);
        if(this.resultSet.last()) {
            this.mapFieldsFromTableRow();
        } else {
            Assert.fail("Unable to Move to Last Row");
        }
    }

    public void moveNext() throws Throwable {
        Assert.assertTrue("No Result Set - do a SELECT statement first", this.resultSet!=null);
        Assert.assertTrue("Query returned no results", this.rowCount>0);
        if(this.resultSet.isAfterLast()||this.resultSet.isBeforeFirst()) {
            throw new IllegalStateException("Invalid - was After Last or Before First");
        }
        if(this.resultSet.isLast()) {
            throw new IllegalStateException("Cannot move to Next - Already on the Last Row");
        }
        if(this.resultSet.next()) {
            this.mapFieldsFromTableRow();
        } else {
            Assert.fail("Unable to Move to Next Row");
        }
    }

    public void movePrevious() throws Throwable {
        Assert.assertTrue("No Result Set - do a SELECT statement first", this.resultSet!=null);
        Assert.assertTrue("Query returned no results", this.rowCount>0);
        if(this.resultSet.isAfterLast()||this.resultSet.isBeforeFirst()) {
            throw new IllegalStateException("Invalid - was After Last or Before First");
        }
        if(this.resultSet.isFirst()) {
            throw new IllegalStateException("Cannot move to Previous - Already on the First Row");
        }
        if(this.resultSet.previous()) {
            this.mapFieldsFromTableRow();
        } else {
            Assert.fail("Unable to Move to Previous Row");
        }
    }

    public boolean EOF() throws Throwable {
        Assert.assertTrue("No Result Set - do a SELECT statement first", this.resultSet!=null);
        Assert.assertTrue("Query returned no results", this.rowCount>0);
        if(this.resultSet.isLast()) {
            return true;
        }
        return false;
    }

    public boolean BOF() throws Throwable {
        Assert.assertTrue("No Result Set - do a SELECT statement first", this.resultSet!=null);
        Assert.assertTrue("Query returned no results", this.rowCount>0);
        if(this.resultSet.isFirst()) {
            return true;
        }
        return false;
    }

    private void createFieldMap() throws Throwable {
        ResultSetMetaData resultSetMetaData = this.resultSet.getMetaData();
        int columns = resultSetMetaData.getColumnCount();
        if(columns==0) {
            throw new IllegalStateException("No Columns detected for query");
        }
        this.fieldSpecs = new HashMap<>();
        this.fields = new HashMap<>();
        this.changedFields = new ArrayList<>();
        this.table = resultSetMetaData.getTableName(1);
        for(int i = 1;i <= columns; i++) {
            Assert.assertTrue("Unsupported Type : " + resultSetMetaData.getColumnTypeName(i), this.isSupportedType(resultSetMetaData.getColumnType(i)));
           this.fieldSpecs.put(resultSetMetaData.getColumnName(i), resultSetMetaData.getColumnType(i));
        }
    }

    private boolean isSupportedType(int type) {
        switch(type) {
            case STRING_TYPE:
            case INTEGER_TYPE:
            case DOUBLE_TYPE:
            case BOOLEAN_TYPE:
            case TIMESTAMP_TYPE: {
                return true;
            }
        }
        return false;
    }

    private void mapFieldsFromTableRow() throws Throwable {
        Set<String> keys = this.fieldSpecs.keySet();
        this.fields = new HashMap<>();
        for(String key: keys) {
            this.fields.put(key, this.resultSet.getObject(key));
        }
        int i = 0;
    }
}
