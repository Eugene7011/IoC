package com.study.entity;

import javax.annotation.PostConstruct;

public class MailService implements IMailService {
    private String protocol;
    private int port;

    @PostConstruct
    private void init() {
        this.port = port;
        this.protocol = protocol;
        // make some initialization
        // fill cache
    }

    @Override
    public void sendEmail(User user, String message) {
        System.out.println("sending email with message: " + message);
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public int getPort() {
        return port;
    }
}
