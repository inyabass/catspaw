package com.inyabass.catspaw.clients;

import com.inyabass.catspaw.config.ConfigProperties;
import com.inyabass.catspaw.config.ConfigReader;

import java.sql.Connection;
import java.sql.DriverManager;

public class MySqlClient {

    private Connection connection = null;

    public MySqlClient() throws Throwable {
        String mySqlServer = ConfigReader.get(ConfigProperties.MYSQL_SERVER);
        String mySqlPort = ConfigReader.get(ConfigProperties.MYSQL_PORT);
        String mySqlUsername = ConfigReader.get(ConfigProperties.MYSQL_USERNAME);
        String mySqlPassword = ConfigReader.get(ConfigProperties.MYSQL_PASSWORD);
        String mySqlSchema = ConfigReader.get(ConfigProperties.MYSQL_SCHEMA);
        String connectionString = "jdbc:mysql://" + mySqlServer + ":" + mySqlPort + "/";
        this.connection = DriverManager.getConnection(connectionString, mySqlUsername, mySqlPassword);
        this.connection.setSchema(mySqlSchema);
    }

    public static void main(String[] args) throws Throwable {
        MySqlClient mySqlClient = new MySqlClient();
        int i = 0;
    }
}
