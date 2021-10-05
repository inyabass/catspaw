package com.inyabass.catspaw.clients;

import com.inyabass.catspaw.config.ConfigProperties;
import com.inyabass.catspaw.config.ConfigReader;
import org.junit.Assert;

import java.sql.*;

public class MySqlClient {

    private Connection connection = null;
    private String schema = null;

    public MySqlClient() throws Throwable {
        String mySqlServer = ConfigReader.get(ConfigProperties.MYSQL_SERVER);
        String mySqlPort = ConfigReader.get(ConfigProperties.MYSQL_PORT);
        String mySqlUsername = ConfigReader.get(ConfigProperties.MYSQL_USERNAME);
        String mySqlPassword = ConfigReader.get(ConfigProperties.MYSQL_PASSWORD);
        this.schema = ConfigReader.get(ConfigProperties.MYSQL_SCHEMA);
        String connectionString = "jdbc:mysql://" + mySqlServer + ":" + mySqlPort + "/";
        this.connection = DriverManager.getConnection(connectionString, mySqlUsername, mySqlPassword);
        this.connection.setSchema(this.schema);
    }

    public static void main(String[] args) throws Throwable {
        MySqlClient mySqlClient = new MySqlClient();
        int i = 0;
    }

    public ResultSet execute(String sqlStatment) throws Throwable {
        Statement statement = this.connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        statement.execute(sqlStatment);
        return statement.getResultSet();
    }

    public void executeNoResult(String sqlStatment) throws Throwable {
        Statement statement = this.connection.createStatement();
        statement.execute(sqlStatment);
    }

    public String getSchema() throws Throwable {
        return this.schema;
    }

    public String getPrimaryKeyForTable(String table) throws Throwable {
        DatabaseMetaData databaseMetaData = this.connection.getMetaData();
        String schema = this.connection.getSchema();
        ResultSet resultSet = databaseMetaData.getPrimaryKeys(null, schema, table);
        resultSet.first();
        String primaryKey = resultSet.getString("COLUMN_NAME");
        resultSet.close();
        return primaryKey;
    }
}
