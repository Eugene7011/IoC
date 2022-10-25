package com.study.entity;

import lombok.Getter;
import lombok.Setter;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class DefaultUserService implements UserService {

    private IMailService mailService;

    public void activateUsers() {
        System.out.println("Get users from db");

        List<User> users = new ArrayList<>(); // userDao.getAll();
        for (User user : users) {
            mailService.sendEmail(user, "You are active now");
        }
    }

}
