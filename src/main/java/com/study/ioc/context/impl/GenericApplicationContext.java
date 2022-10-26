package com.study.ioc.context.impl;

import com.study.ioc.context.ApplicationContext;
import com.study.ioc.entity.Bean;
import com.study.ioc.entity.BeanDefinition;
import com.study.ioc.exception.BeanInstantiationException;
import com.study.ioc.exception.NoSuchBeanDefinitionException;
import com.study.ioc.exception.NoUniqueBeanOfTypeException;
import com.study.ioc.processor.BeanFactoryPostProcessor;
import com.study.ioc.processor.BeanPostProcessor;
import com.study.ioc.reader.BeanDefinitionReader;
import com.study.ioc.reader.sax.XmlBeanDefinitionReader;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import javax.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Setter
@Getter
public class GenericApplicationContext implements ApplicationContext {

    private Map<String, Bean> beans = new HashMap<>();
    private List<BeanFactoryPostProcessor> serviceFactoryBeans = new ArrayList<>();
    private Map<String, Bean> serviceBeans = new HashMap<>();

    GenericApplicationContext() {
    }

    public GenericApplicationContext(String... paths) throws InstantiationException, IllegalAccessException {
        this(new XmlBeanDefinitionReader(paths));
    }

    public GenericApplicationContext(BeanDefinitionReader definitionReader) throws InstantiationException, IllegalAccessException {
        Map<String, BeanDefinition> beanDefinitions = definitionReader.getBeanDefinition();

        createAllServiceBeans(beanDefinitions);
        processBeanDefinitions(beanDefinitions);
        beans = createBeans(beanDefinitions);
        injectValueDependencies(beanDefinitions, beans);
        injectRefDependencies(beanDefinitions, beans);
        postProcessBeans();
    }

    @Override
    public Object getBean(String beanId) {
        return beans.get(beanId).getValue();
    }

    @Override
    public <T> T getBean(Class<T> clazz) {
        T beanValue = null;
        for (Map.Entry<String, Bean> entry : beans.entrySet()) {
            Bean bean = entry.getValue();
            if (clazz.isAssignableFrom(bean.getValue().getClass())) {
                if (beanValue != null) {
                    throw new NoUniqueBeanOfTypeException("No unique bean of type :" + clazz.getName());
                }
                beanValue = clazz.cast(bean.getValue());
            }
        }
        return beanValue;
    }

    @Override
    public <T> T getBean(String id, Class<T> clazz) {
        for (Map.Entry<String, Bean> entry : beans.entrySet()) {
            if (id.equals(entry.getKey())) {
                Object value = entry.getValue().getValue();
                if (!clazz.isAssignableFrom(value.getClass())) {
                    throw new NoSuchBeanDefinitionException(id, clazz.getName(), value.getClass().getName());
                }
                return clazz.cast(value);
            }
        }
        return null;
    }

    @Override
    public List<String> getBeanNames() {
        return beans.keySet().stream().toList();
    }

    Map<String, Bean> createBeans(Map<String, BeanDefinition> beanDefinitionMap) throws InstantiationException, IllegalAccessException {
        try {
            for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
                Object object = Class.forName(entry.getValue().getClassName()).getDeclaredConstructor().newInstance();
                Bean newBean = new Bean(entry.getValue().getId(), object);
                beans.put(entry.getKey(), newBean);
            }
        } catch (ClassNotFoundException e) {
            throw new BeanInstantiationException("BeanInstantiation failed", e);
        } catch (NoSuchMethodException | InvocationTargetException e){
            e.printStackTrace();
        }
        return beans;
    }

    void injectValueDependencies(Map<String, BeanDefinition> beanDefinitions, Map<String, Bean> beans) {
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitions.entrySet()) {
            String key = entry.getKey();
            Bean bean = beans.get(key);
            Map<String, String> valueDependencies = entry.getValue().getValueDependencies();
            valueDependencies.forEach((keyValue, value) -> clarifyMethodAndInjectValue(bean, keyValue, value));
        }
    }

    void injectRefDependencies(Map<String, BeanDefinition> beanDefinitions, Map<String, Bean> beans) {
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitions.entrySet()) {
            String key = entry.getKey();
            Bean bean = beans.get(key);
            Map<String, String> refDependencies = entry.getValue().getRefDependencies();

            refDependencies.forEach((fieldName, beanObject) -> clarifyMethodAndInjectRefDependencies(bean, fieldName, beans.get(beanObject).getValue()));
        }
    }

    @SneakyThrows
    void injectValue(Object object, Method classMethod, String propertyValue) {
        List<Method> methods = Arrays.stream(object.getClass().getDeclaredMethods()).toList();
        Class<?>[] parameterTypes = classMethod.getParameterTypes();
        if (parameterTypes.length != 1 || !methods.contains(classMethod)) {
            throw new IllegalArgumentException();
        }
        String methodClassName = parameterTypes[0].getName();

        if (methodClassName.equalsIgnoreCase("int")) {
            int intValueOfProperty = Integer.parseInt(propertyValue);
            classMethod.invoke(object, intValueOfProperty);
            return;
        }
        classMethod.invoke(object, propertyValue);
    }

    void setBeans(Map<String, Bean> beans) {
        this.beans = beans;
    }

    @SneakyThrows
    private void clarifyMethodAndInjectValue(Bean bean, String keyValue, String value) {
        Method[] declaredMethods = bean.getValue().getClass().getDeclaredMethods();
        String methodName = "set" + keyValue.substring(0, 1).toUpperCase() + keyValue.substring(1);
        Method methodClass = Arrays.stream(declaredMethods)
                .filter(method -> method.getName().equals(methodName))
                .findFirst().get();
        Class<?>[] parameterTypes = methodClass.getParameterTypes();
        String methodClassName = parameterTypes[0].getName();
        if (methodClassName.equals("int")) {
            Method neededMethod = bean.getValue().getClass().getDeclaredMethod(methodName, Integer.TYPE);
            injectValue(bean.getValue(), neededMethod, value);
            return;
        }
        Method neededMethod = bean.getValue().getClass().getDeclaredMethod(methodName, String.class);
        injectValue(bean.getValue(), neededMethod, String.valueOf(value));
    }

    @SneakyThrows
    private void clarifyMethodAndInjectRefDependencies(Bean bean, String fieldName, Object value) {
        Method[] declaredMethods = bean.getValue().getClass().getDeclaredMethods();
        String methodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        Method methodClass = Arrays.stream(declaredMethods)
                .filter(method -> method.getName().equals(methodName))
                .findFirst().get();
        methodClass.invoke(bean.getValue(), value);
    }

    @SneakyThrows
    void createAllServiceBeans(Map<String, BeanDefinition> beanDefinitions) {
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitions.entrySet()) {
            BeanDefinition entryValue = entry.getValue();
            Class<?> clazz = Class.forName(entryValue.getClassName());

            if ((BeanFactoryPostProcessor.class).isAssignableFrom(clazz)){
                BeanFactoryPostProcessor newFactoryPostProcessor = (BeanFactoryPostProcessor) Class.forName(clazz.getName()).getDeclaredConstructor().newInstance();
                serviceFactoryBeans.add(newFactoryPostProcessor);
            }

            if ((BeanPostProcessor.class).isAssignableFrom(clazz) ){
                BeanPostProcessor newPostProcessor = (BeanPostProcessor) Class.forName(clazz.getName()).getDeclaredConstructor().newInstance();
                Bean newBean = new Bean(entryValue.getId(), newPostProcessor);
                serviceBeans.put(entry.getKey(), newBean);
            }
        }
    }

    void processBeanDefinitions(Map<String, BeanDefinition> beanDefinitions) {
        List<BeanDefinition> beanDefinitionList = new ArrayList<>();
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitions.entrySet()) {
            beanDefinitionList.add(entry.getValue());
        }

        for (BeanFactoryPostProcessor serviceFactoryBean : serviceFactoryBeans) {
            serviceFactoryBean.postProcessorBeanFactory(beanDefinitionList, beanDefinitions);
        }
    }

    private void postProcessBeans() {
        for (Map.Entry<String, Bean> entry : serviceBeans.entrySet()) {
            Bean bean = entry.getValue();
            BeanPostProcessor beanPostProcessor = (BeanPostProcessor) bean.getValue();

            beanPostProcessor.postProcessBeforeInitialization(bean, entry.getKey());
            callInitMethods();
            beanPostProcessor.postProcessAfterInitialization(bean, entry.getKey());
        }
    }

    @SneakyThrows
    private void callInitMethods() {

        for (Map.Entry<String, Bean> entry : serviceBeans.entrySet()) {
            Bean bean = entry.getValue();
            Class<?> clazz = bean.getValue().getClass();

            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (method.getAnnotation(PostConstruct.class) != null) {
                    method.invoke(bean.getValue());
                }
            }
        }
    }


}