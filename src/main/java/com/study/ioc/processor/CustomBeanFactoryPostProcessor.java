package com.study.ioc.processor;

import com.study.ioc.entity.BeanDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomBeanFactoryPostProcessor implements BeanFactoryPostProcessor{

    @Override
    public void postProcessorBeanFactory(List<BeanDefinition> beanDefinitionList) {
        for (BeanDefinition beanDefinition : beanDefinitionList) {
            if (beanDefinition.getId().equals(("mailServicePOP"))){
                Map<String, String> myMap = new HashMap<>() {{
                    put("port", "1000");
                    put("protocol", "TEST");
                }};
                beanDefinition.setValueDependencies(myMap);
            }
        }
    }
}
