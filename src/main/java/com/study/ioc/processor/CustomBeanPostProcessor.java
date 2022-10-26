package com.study.ioc.processor;

import com.study.ioc.entity.Bean;

public class CustomBeanPostProcessor implements BeanPostProcessor{

    @Override
    public Object postProcessBeforeInitialization(Bean bean, String beanName) {
        bean.setId("BeforeInitialization");
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Bean bean, String beanName) {
        bean.setId("AfterInitialization");
        return bean;
    }
}
