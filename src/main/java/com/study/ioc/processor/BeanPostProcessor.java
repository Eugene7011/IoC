package com.study.ioc.processor;

import com.study.ioc.entity.Bean;

public interface BeanPostProcessor {
    Object postProcessBeforeInitialization(Bean bean, String beanName);
    Object postProcessAfterInitialization(Bean bean, String beanName);
}
