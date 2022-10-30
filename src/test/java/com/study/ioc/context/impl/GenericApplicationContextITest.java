package com.study.ioc.context.impl;

import com.study.entity.MailService;
import com.study.entity.UserService;
import com.study.ioc.processor.TestClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GenericApplicationContextITest {


    @Test
    public void integrationTest() throws InstantiationException, IllegalAccessException {
        GenericApplicationContext genericApplicationContextTest = new GenericApplicationContext("context.xml");

        UserService userService = (UserService) genericApplicationContextTest.getBean("userService");
        assertNotNull(userService);

        MailService mailServiceIMAP = (MailService) genericApplicationContextTest.getBean("mailServiceIMAP");
        assertNotNull(mailServiceIMAP);

        TestClass testClass = (TestClass) genericApplicationContextTest.getBean("testClass");
        assertNotNull(testClass);
        assertEquals(003, testClass.getId());
        assertEquals("AfterInitialization", testClass.getText());
    }
}
