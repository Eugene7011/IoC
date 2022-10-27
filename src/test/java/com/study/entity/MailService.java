package com.study.entity;

import lombok.Getter;
import lombok.Setter;

import javax.annotation.PostConstruct;

@Getter
@Setter
public class MailService implements IMailService {
    private String protocol;
    private int port;

    @PostConstruct
    private void init() {
        this.port = port + 1000;
        this.protocol = "TEST";

        // make some initialization
        // fill cache
    }

    @Override
    public void sendEmail(User user, String message) {
        System.out.println("sending email with message: " + message);
    }

}
