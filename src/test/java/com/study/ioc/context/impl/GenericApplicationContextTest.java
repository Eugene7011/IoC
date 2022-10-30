package com.study.ioc.context.impl;

import com.study.entity.DefaultUserService;
import com.study.entity.MailService;
import com.study.entity.UserService;
import com.study.ioc.entity.Bean;
import com.study.ioc.entity.BeanDefinition;
import com.study.ioc.exception.BeanInstantiationException;
import com.study.ioc.exception.NoSuchBeanDefinitionException;
import com.study.ioc.exception.NoUniqueBeanOfTypeException;
import com.study.ioc.processor.BeanFactoryPostProcessor;
import com.study.ioc.processor.BeanPostProcessor;
import com.study.ioc.processor.CustomBeanFactoryPostProcessor;
import com.study.ioc.processor.CustomBeanPostProcessor;
import com.study.ioc.processor.TestClass;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class GenericApplicationContextTest {

    private GenericApplicationContext genericApplicationContext;

    @Before
    public void before() {
        genericApplicationContext = new GenericApplicationContext();
    }

    @Test
    public void testCreateBeans() throws InstantiationException, IllegalAccessException {
        Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();
        BeanDefinition beanDefinitionMailService = new BeanDefinition("mailServicePOP", "com.study.entity.MailService");
        beanDefinitionMap.put("mailServicePOP", beanDefinitionMailService);
        BeanDefinition beanDefinitionUserService = new BeanDefinition("userService", "com.study.entity.DefaultUserService");
        beanDefinitionMap.put("userService", beanDefinitionUserService);

        Map<String, Bean> beanMap = genericApplicationContext.createBeans(beanDefinitionMap);

        Bean actualMailBean = beanMap.get("mailServicePOP");
        assertNotNull(actualMailBean);
        assertEquals("mailServicePOP", actualMailBean.getId());
        assertEquals(MailService.class, actualMailBean.getValue().getClass());

        Bean actualUserBean = beanMap.get("userService");
        assertNotNull(actualUserBean);
        assertEquals("userService", actualUserBean.getId());
        assertEquals(DefaultUserService.class, actualUserBean.getValue().getClass());
    }


    @Test(expected = BeanInstantiationException.class)
    public void testCreateBeansWithWrongClass() throws InstantiationException, IllegalAccessException {
        Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();
        BeanDefinition errorBeanDefinition = new BeanDefinition("mailServicePOP", "com.study.entity.TestClass");
        beanDefinitionMap.put("mailServicePOP", errorBeanDefinition);
        Map<String, Bean> beanMap = genericApplicationContext.createBeans(beanDefinitionMap);
    }

    @Test
    public void testGetBeanById() {
        Map<String, Bean> beanMap = new HashMap<>();
        DefaultUserService beanValue1 = new DefaultUserService();
        DefaultUserService beanValue2 = new DefaultUserService();
        beanMap.put("bean1", new Bean("bean1", beanValue1));
        beanMap.put("bean2", new Bean("bean2", beanValue2));
        genericApplicationContext.setBeans(beanMap);
        DefaultUserService actualBeanValue1 = (DefaultUserService) genericApplicationContext.getBean("bean1");
        DefaultUserService actualBeanValue2 = (DefaultUserService) genericApplicationContext.getBean("bean2");
        assertNotNull(actualBeanValue1);
        assertNotNull(actualBeanValue2);
        assertEquals(beanValue1, actualBeanValue1);
        assertEquals(beanValue2, actualBeanValue2);
    }

    @Test
    public void testGetBeanByClazz() {
        Map<String, Bean> beanMap = new HashMap<>();
        DefaultUserService beanValue1 = new DefaultUserService();
        MailService beanValue2 = new MailService();
        beanMap.put("bean1", new Bean("bean1", beanValue1));
        beanMap.put("bean2", new Bean("bean2", beanValue2));
        genericApplicationContext.setBeans(beanMap);
        DefaultUserService actualBeanValue1 = (DefaultUserService) genericApplicationContext.getBean(UserService.class);
        MailService actualBeanValue2 = genericApplicationContext.getBean(MailService.class);
        assertNotNull(actualBeanValue1);
        assertNotNull(actualBeanValue2);
        assertEquals(beanValue1, actualBeanValue1);
        assertEquals(beanValue2, actualBeanValue2);
    }

    @Test(expected = NoUniqueBeanOfTypeException.class)
    public void testGetBeanByClazzNoUniqueBean() {
        Map<String, Bean> beanMap = new HashMap<>();
        beanMap.put("bean1", new Bean("bean1", new DefaultUserService()));
        beanMap.put("bean2", new Bean("bean2", new DefaultUserService()));
        genericApplicationContext.setBeans(beanMap);
        genericApplicationContext.getBean(DefaultUserService.class);
    }

    @Test
    public void testGetBeanByIdAndClazz() {
        Map<String, Bean> beanMap = new HashMap<>();
        DefaultUserService beanValue1 = new DefaultUserService();
        DefaultUserService beanValue2 = new DefaultUserService();
        beanMap.put("bean1", new Bean("bean1", beanValue1));
        beanMap.put("bean2", new Bean("bean2", beanValue2));
        genericApplicationContext.setBeans(beanMap);
        DefaultUserService actualBeanValue1 = genericApplicationContext.getBean("bean1", DefaultUserService.class);
        DefaultUserService actualBeanValue2 = genericApplicationContext.getBean("bean2", DefaultUserService.class);
        assertNotNull(actualBeanValue1);
        assertNotNull(actualBeanValue2);
        assertEquals(beanValue1, actualBeanValue1);
        assertEquals(beanValue2, actualBeanValue2);
    }


    @Test(expected = NoSuchBeanDefinitionException.class)
    public void testGetBeanByIdAndClazzNoSuchBean() {
        Map<String, Bean> beanMap = new HashMap<>();
        DefaultUserService beanValue = new DefaultUserService();
        beanMap.put("bean1", new Bean("bean1", beanValue));
        genericApplicationContext.setBeans(beanMap);
        genericApplicationContext.getBean("bean1", MailService.class);

    }

    @Test
    public void getBeanNames() {
        Map<String, Bean> beanMap = new HashMap<>();
        beanMap.put("bean3", new Bean("bean3", new DefaultUserService()));
        beanMap.put("bean4", new Bean("bean4", new DefaultUserService()));
        beanMap.put("bean5", new Bean("bean5", new DefaultUserService()));
        genericApplicationContext.setBeans(beanMap);
        List<String> actualBeansNames = genericApplicationContext.getBeanNames();
        List<String> expectedBeansNames = Arrays.asList("bean3", "bean4", "bean5");
        assertTrue(actualBeansNames.containsAll(expectedBeansNames));
        assertTrue(expectedBeansNames.containsAll(actualBeansNames));
    }

    @Test
    public void testInjectValueDependencies() throws InstantiationException, IllegalAccessException {
        Map<String, Bean> beanMap = new HashMap<>();
        Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();

        MailService mailServicePOP = new MailService();
        beanMap.put("mailServicePOP", new Bean("mailServicePOP", mailServicePOP));
        MailService mailServiceIMAP = new MailService();
        beanMap.put("mailServiceIMAP", new Bean("mailServiceIMAP", mailServiceIMAP));

        //  setPort(110) and setProtocol("POP3") via valueDependencies
        BeanDefinition popServiceBeanDefinition = new BeanDefinition("mailServicePOP", "com.study.entity.MailService");
        Map<String, String> popServiceValueDependencies = new HashMap<>();
        popServiceValueDependencies.put("port", "110");
        popServiceValueDependencies.put("protocol", "POP3");
        popServiceBeanDefinition.setValueDependencies(popServiceValueDependencies);
        beanDefinitionMap.put("mailServicePOP", popServiceBeanDefinition);

        //  setPort(143) and setProtocol("IMAP") via valueDependencies
        BeanDefinition imapServiceBeanDefinition = new BeanDefinition("mailServiceIMAP", "com.study.entity.MailService");
        Map<String, String> imapServiceValueDependencies = new HashMap<>();
        imapServiceValueDependencies.put("port", "143");
        imapServiceValueDependencies.put("protocol", "IMAP");
        imapServiceBeanDefinition.setValueDependencies(imapServiceValueDependencies);
        beanDefinitionMap.put("mailServiceIMAP", imapServiceBeanDefinition);

        genericApplicationContext.createAllServiceBeans(beanDefinitionMap);
        genericApplicationContext.processBeanDefinitions(beanDefinitionMap);
        genericApplicationContext.createBeans(beanDefinitionMap);
        genericApplicationContext.injectValueDependencies(beanDefinitionMap, beanMap);

        MailService mailServicePOPFinal = (MailService) genericApplicationContext.getBean("mailServicePOP");
        MailService mailServiceIMAPFinal = (MailService) genericApplicationContext.getBean("mailServiceIMAP");
        assertEquals(110, mailServicePOPFinal.getPort());
        assertEquals("POP3", mailServicePOPFinal.getProtocol());
        assertEquals(143, mailServiceIMAPFinal.getPort());
        assertEquals("IMAP", mailServiceIMAPFinal.getProtocol());
    }

    @Test
    public void testInjectRefDependencies() {
        Map<String, Bean> beanMap = new HashMap<>();
        Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();
        int port = 110;

        MailService mailServicePOP = new MailService();
        mailServicePOP.setPort(port);
        mailServicePOP.setProtocol("POP3");
        beanMap.put("mailServicePOP", new Bean("mailServicePOP", mailServicePOP));

        DefaultUserService userService = new DefaultUserService();
        beanMap.put("userService", new Bean("userService", userService));

        //  setMailService(mailServicePOP) via refDependencies
        BeanDefinition userServiceBeanDefinition = new BeanDefinition("userService", "com.study.entity.DefaultUserService");
        Map<String, String> userServiceRefDependencies = new HashMap<>();
        userServiceRefDependencies.put("mailService", "mailServicePOP");
        userServiceBeanDefinition.setRefDependencies(userServiceRefDependencies);
        beanDefinitionMap.put("userService", userServiceBeanDefinition);

        genericApplicationContext.injectRefDependencies(beanDefinitionMap, beanMap);
        assertNotNull(userService.getMailService());
        assertEquals(110, ((MailService) userService.getMailService()).getPort());
        assertEquals("POP3", ((MailService) userService.getMailService()).getProtocol());
    }

    @Test
    public void testInjectValue() throws ReflectiveOperationException {
        MailService mailService = new MailService();
        Method setPortMethod = MailService.class.getDeclaredMethod("setPort", Integer.TYPE);
        genericApplicationContext.injectValue(mailService, setPortMethod, "465");
        int actualPort = mailService.getPort();
        assertEquals(465, actualPort);
    }

    @Test
    public void processBeanDefinitions() throws InstantiationException, IllegalAccessException {
        Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();
        BeanDefinition beanDefinitionMailService = new BeanDefinition("mailServicePOP", "com.study.entity.MailService");
        beanDefinitionMap.put("mailServicePOP", beanDefinitionMailService);
        BeanDefinition beanDefinitionUserService = new BeanDefinition("userService", "com.study.entity.DefaultUserService");
        beanDefinitionMap.put("userService", beanDefinitionUserService);
        BeanDefinition beanDefinitionFactoryPostProcessor =
                new BeanDefinition("customBeanFactoryPostProcessor", "com.study.ioc.processor.CustomBeanFactoryPostProcessor");
        beanDefinitionMap.put("customBeanFactoryPostProcessor", beanDefinitionFactoryPostProcessor);

        genericApplicationContext.createAllServiceBeans(beanDefinitionMap);
        genericApplicationContext.processBeanDefinitions(beanDefinitionMap);
        Map<String, Bean> beanMap = genericApplicationContext.createBeans(beanDefinitionMap);
        genericApplicationContext.injectValueDependencies(beanDefinitionMap, beanMap);

        Bean actualMailBean = beanMap.get("mailServicePOP");
        assertNotNull(actualMailBean);
        assertEquals("mailServicePOP", actualMailBean.getId());
        assertEquals(MailService.class, actualMailBean.getValue().getClass());
        MailService mailService = (MailService) actualMailBean.getValue();

        assertEquals(1000, mailService.getPort());
        assertEquals("TEST", mailService.getProtocol());
    }

    @Test
    public void createAllServiceBeans() {
        Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();
        BeanDefinition beanDefinitionUserService = new BeanDefinition("userService", "com.study.entity.DefaultUserService");
        beanDefinitionMap.put("userService", beanDefinitionUserService);
        BeanDefinition beanDefinitionFactoryPostProcessor =
                new BeanDefinition("customBeanFactoryPostProcessor", "com.study.ioc.processor.CustomBeanFactoryPostProcessor");
        beanDefinitionMap.put("customBeanFactoryPostProcessor", beanDefinitionFactoryPostProcessor);
        BeanDefinition beanDefinitionPostProcessor =
                new BeanDefinition("customBeanPostProcessor", "com.study.ioc.processor.CustomBeanPostProcessor");
        beanDefinitionMap.put("customBeanPostProcessor", beanDefinitionPostProcessor);

        genericApplicationContext.createAllServiceBeans(beanDefinitionMap);

        Map<String, Bean> serviceBeans = genericApplicationContext.getServiceBeans();
        List<BeanFactoryPostProcessor> serviceFactoryBeans = genericApplicationContext.getServiceFactoryBeans();

        assertNotNull(serviceBeans);
        assertNotNull(serviceFactoryBeans);

        assertEquals(CustomBeanFactoryPostProcessor.class, serviceFactoryBeans.get(0).getClass());
        Class<?> customBeanPostProcessor = serviceBeans.get("customBeanPostProcessor").getValue().getClass();
        assertEquals(CustomBeanPostProcessor.class, customBeanPostProcessor);
    }

    @Test
    public void createBeansNotSaveServiceBeans() throws InstantiationException, IllegalAccessException {
        Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();
        BeanDefinition beanDefinitionUserService = new BeanDefinition("userService", "com.study.entity.DefaultUserService");
        beanDefinitionMap.put("userService", beanDefinitionUserService);
        BeanDefinition beanDefinitionFactoryPostProcessor =
                new BeanDefinition("customBeanFactoryPostProcessor", "com.study.ioc.processor.CustomBeanFactoryPostProcessor");
        beanDefinitionMap.put("customBeanFactoryPostProcessor", beanDefinitionFactoryPostProcessor);

        Map<String, Bean> beans = genericApplicationContext.createBeans(beanDefinitionMap);

        assertNotNull(beans);
        assertEquals(1, beans.size());
        assertNull(beans.get("customBeanFactoryPostProcessor"));
    }

    @Test
    public void processBeans() throws InstantiationException, IllegalAccessException {
        Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();
        BeanDefinition beanDefinitionUserService = new BeanDefinition("userService", "com.study.entity.DefaultUserService");
        beanDefinitionMap.put("userService", beanDefinitionUserService);

        BeanDefinition beanDefinitionMailService = new BeanDefinition("mailServicePOP", "com.study.entity.MailService");
        beanDefinitionMap.put("mailServicePOP", beanDefinitionMailService);

        BeanDefinition beanDefinitionPostProcessor =
                new BeanDefinition("customBeanPostProcessor", "com.study.ioc.processor.CustomBeanPostProcessor");
        beanDefinitionMap.put("customBeanPostProcessor", beanDefinitionPostProcessor);

        BeanDefinition beanDefinitionTestClass = new BeanDefinition("testClass", "com.study.ioc.processor.TestClass");
        beanDefinitionMap.put("testClass", beanDefinitionTestClass);

        genericApplicationContext.createAllServiceBeans(beanDefinitionMap);
        genericApplicationContext.processBeanDefinitions(beanDefinitionMap);
        Map<String, Bean> beans = genericApplicationContext.createBeans(beanDefinitionMap);
        genericApplicationContext.injectValueDependencies(beanDefinitionMap, beans);
        genericApplicationContext.injectRefDependencies(beanDefinitionMap, beans);

        Bean customBeanPostProcessor = genericApplicationContext.getServiceBeans().get("customBeanPostProcessor");
        BeanPostProcessor beanPostProcessor = (BeanPostProcessor) customBeanPostProcessor.getValue();
        Bean userServiceBean = beans.get("userService");
        assertEquals("userService", userServiceBean.getId());

        Bean testClassBeanAfterProcess = beans.get("testClass");
        genericApplicationContext.callPostProcessBeforeInitialization(userServiceBean, beanPostProcessor);
        genericApplicationContext.callPostProcessBeforeInitialization(testClassBeanAfterProcess, beanPostProcessor);
        TestClass testClass = (TestClass) testClassBeanAfterProcess.getValue();
        assertEquals("BeforeInitialization", testClass.getText());
        assertEquals(001, testClass.getId());

        Bean mailServiceBean = beans.get("mailServicePOP");
        MailService mailService = (MailService) mailServiceBean.getValue();
        assertEquals(0, mailService.getPort());
        assertNull(mailService.getProtocol());

        genericApplicationContext.callInitMethods();
        Bean mailServiceBeanAfterInitMethod = beans.get("mailServicePOP");
        MailService mailServiceAfterInitMethod = (MailService) mailServiceBeanAfterInitMethod.getValue();
        assertEquals(1000, mailServiceAfterInitMethod.getPort());
        assertEquals("TEST", mailServiceAfterInitMethod.getProtocol());

        genericApplicationContext.callPostProcessAfterInitialization(userServiceBean, beanPostProcessor);
        genericApplicationContext.callPostProcessAfterInitialization(testClassBeanAfterProcess, beanPostProcessor);
        Bean testClassBeanAfterProcessor = beans.get("testClass");
        TestClass testClassFinal = (TestClass) testClassBeanAfterProcessor.getValue();
        assertEquals("AfterInitialization", testClassFinal.getText());
        assertEquals(003, testClassFinal.getId());
    }


}
