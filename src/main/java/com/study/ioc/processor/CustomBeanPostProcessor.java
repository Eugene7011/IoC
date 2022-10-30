package com.study.ioc.processor;

import com.study.ioc.entity.Bean;
import lombok.SneakyThrows;

public class CustomBeanPostProcessor implements BeanPostProcessor{

    @SneakyThrows
    @Override
    public Object postProcessBeforeInitialization(Bean bean, String beanName) {
        Object object = bean.getValue();
        if (bean.getValue().getClass().equals(TestClass.class)) {
            TestClass testClass;
            testClass = new TestClass();
            testClass.setText("BeforeInitialization");
            testClass.setId(001);
            object = testClass;
        }
        return object;
    }

    @Override
    public Object postProcessAfterInitialization(Bean bean, String beanName) {
        Object object = bean.getValue();
        if (bean.getValue().getClass().equals(TestClass.class)) {
            TestClass testClass;
            testClass = new TestClass();
            testClass.setText("AfterInitialization");
            testClass.setId(003);
            object = testClass;
        }
        return object;
    }
}
