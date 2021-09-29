package com.inyabass.catspaw.clients;

import javax.mail.Session;

public class SmtpClient {

    private String smtpServer = null;
    private int port = 25;
    private Session session = null;

    public SmtpClient(String smtpServer) {
        this.setSmtpServer(smtpServer);
        this.session = this.createSession();
    }

    public SmtpClient(String smtpServer, int port) {
        this.setSmtpServer(smtpServer);
        this.setPort(port);
        this.session = this.createSession();
    }

    public void setSmtpServer(String smtpServer) {
        this.smtpServer = smtpServer;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Session createSession() {
        return null;
    }
}
