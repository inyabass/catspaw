package com.inyabass.catspaw.clients;

import com.inyabass.catspaw.logging.Logger;

import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Properties;

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
        SmtpClient smtpClient = new SmtpClient("localhost", "inyabass@gmail.com");
        smtpClient.sendSimpleEmail("mark.wilkinson@dotmatics.com", "Test message again", "Hello world");
    }

    public void setSmtpServer(String smtpServer) {
        this.smtpServer = smtpServer;
    }

    public void sendSimpleEmail(String to, String subject, String body) throws Throwable {
        String[] rawAddresses = to.split(",");
        if(rawAddresses.length==0) {
            logger.error("No To Email Addresses supplied");
            return;
        }
        InternetAddress[] internetAddresses = new InternetAddress[rawAddresses.length];
        for(int i = 0;i<rawAddresses.length;i++) {
            try {
                internetAddresses[i] = new InternetAddress(rawAddresses[i]);
            } catch (Throwable t) {
                logger.warn("Invalid email address found: " + rawAddresses[i]);
            }
        }
        if(internetAddresses.length==0) {
            logger.warn("No valid email addresses found - no message will be sent");
            return;
        }
        Properties properties = new Properties();
        properties.put("mail.smtp.host", this.smtpServer);
        properties.put("mail.smtp.port", String.valueOf(this.port));
        Session session = Session.getInstance(properties);
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(this.from));
        message.setRecipients(Message.RecipientType.TO, internetAddresses);
        message.setSubject(subject);
        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(body, "text/html");
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);

        message.setContent(multipart);

        Transport.send(message);
    }

    public void setPort(int port) {
        this.port = port;
    }
}
