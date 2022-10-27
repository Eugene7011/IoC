package com.study.ioc.processor;

import com.study.ioc.entity.Bean;

public interface BeanPostProcessor {
    Bean postProcessBeforeInitialization(Bean bean, String beanName);
    Bean postProcessAfterInitialization(Bean bean, String beanName);
}
