package com.study.ioc.processor;

import com.study.ioc.entity.BeanDefinition;

import java.util.List;

public interface BeanFactoryPostProcessor {

    void postProcessorBeanFactory(List<BeanDefinition> beanDefinitionList);
}
