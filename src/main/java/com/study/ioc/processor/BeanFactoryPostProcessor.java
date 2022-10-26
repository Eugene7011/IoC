package com.study.ioc.processor;

import com.study.ioc.entity.BeanDefinition;

import java.util.List;
import java.util.Map;

public interface BeanFactoryPostProcessor {

    void postProcessorBeanFactory(Map<String, BeanDefinition> beanDefinitions);
}
