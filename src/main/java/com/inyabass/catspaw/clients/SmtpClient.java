package com.inyabass.catspaw.clients;

import com.inyabass.catspaw.logging.Logger;

import java.lang.invoke.MethodHandles;

public class SmtpClient {

    final static Logger logger = new Logger(MethodHandles.lookup().lookupClass());

    private String smtpServer = null;
    private String from = null;
    private int port = 25;

    public SmtpClient(String smtpServer, String from) {
        this.setSmtpServer(smtpServer);
        this.from = from;
    }

    public SmtpClient(String smtpServer, String from, int port) {
        this.setSmtpServer(smtpServer);
        this.from = from;
        this.setPort(port);
    }

    public static void main(String[] args) throws Throwable {
        SmtpClient smtpClient = new SmtpClient("localhost", "mark.wilkinson@dotmatics.com");
        smtpClient.sendSimpleEmail("inyabass@gmail.com", "Test message", "Hello world");
    }

    public void setSmtpServer(String smtpServer) {
        this.smtpServer = smtpServer;
    }

    public void sendSimpleEmail(String to, String subject, String body) throws Throwable {
    }

    public void setPort(int port) {
        this.port = port;
    }
}
