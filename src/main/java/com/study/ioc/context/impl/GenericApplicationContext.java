package com.study.ioc.context.impl;

import com.study.ioc.context.ApplicationContext;
import com.study.ioc.entity.Bean;
import com.study.ioc.entity.BeanDefinition;
import com.study.ioc.exception.BeanInstantiationException;
import com.study.ioc.exception.NoSuchBeanDefinitionException;
import com.study.ioc.exception.NoUniqueBeanOfTypeException;
import com.study.ioc.exception.ProcessPostConstructException;
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
        createBeans(beanDefinitions);
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

    public Map<String, Bean> createBeans(Map<String, BeanDefinition> beanDefinitionMap) throws InstantiationException, IllegalAccessException {
        try {
            for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
                Object object = Class.forName(entry.getValue().getClassName()).getDeclaredConstructor().newInstance();

                if (!(BeanFactoryPostProcessor.class).isAssignableFrom(object.getClass()) &&
                        !(BeanPostProcessor.class).isAssignableFrom(object.getClass())) {
                    Bean newBean = new Bean(entry.getValue().getId(), object);
                    beans.put(entry.getKey(), newBean);
                }
            }
        } catch (ClassNotFoundException e) {
            throw new BeanInstantiationException("BeanInstantiation failed", e);
        } catch (NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return beans;
    }

    public void injectValueDependencies(Map<String, BeanDefinition> beanDefinitions, Map<String, Bean> beans) {
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitions.entrySet()) {
            String key = entry.getKey();
            Bean bean = getBeans().get(key);
            Map<String, String> valueDependencies = entry.getValue().getValueDependencies();

            for (Map.Entry<String, String> entrySet : valueDependencies.entrySet()) {
                clarifyMethodAndInjectValue(bean, entrySet.getKey(), entrySet.getValue());
            }
        }
    }

    public void injectRefDependencies(Map<String, BeanDefinition> beanDefinitions, Map<String, Bean> beans) {
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitions.entrySet()) {
            String key = entry.getKey();
            Bean bean = beans.get(key);
            Map<String, String> refDependencies = entry.getValue().getRefDependencies();

            if (refDependencies.size() != 0) {
                refDependencies.forEach((fieldName, beanObject)
                        -> clarifyMethodAndInjectRefDependencies(bean, fieldName, beans.get(beanObject).getValue()));
            }
        }
    }

    @SneakyThrows
    public void injectValue(Object object, Method classMethod, String propertyValue) {
        List<Method> methods = Arrays.stream(object.getClass().getMethods()).toList();
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

    public void processBeanDefinitions(Map<String, BeanDefinition> beanDefinitions) {
        List<BeanDefinition> beanDefinitionList = new ArrayList<>();
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitions.entrySet()) {
            beanDefinitionList.add(entry.getValue());
        }

        for (BeanFactoryPostProcessor serviceFactoryBean : serviceFactoryBeans) {
            serviceFactoryBean.postProcessorBeanFactory(beanDefinitions);
        }
    }

    @SneakyThrows
    public void createAllServiceBeans(Map<String, BeanDefinition> beanDefinitions) {
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitions.entrySet()) {
            BeanDefinition entryValue = entry.getValue();
            Class<?> clazz = Class.forName(entryValue.getClassName());

            createFactoryPostProcessBeans(clazz);
            createPostProcessBeans(clazz, entry);
        }
    }

    public void callPostProcessAfterInitialization(Bean bean, BeanPostProcessor objectPostProcessor) {
        Object objectAfterProcess = objectPostProcessor.postProcessAfterInitialization(bean, bean.getId());
        bean.setValue(objectAfterProcess);
        beans.put(bean.getId(), bean);
    }

    public void callPostProcessBeforeInitialization(Bean bean, BeanPostProcessor objectPostProcessor) {
        Object objectBeforeProcess = objectPostProcessor.postProcessBeforeInitialization(bean, bean.getId());
        bean.setValue(objectBeforeProcess);
        beans.put(bean.getId(), bean);
    }

    @SneakyThrows
    public void callInitMethods() {

        for (Map.Entry<String, Bean> entry : beans.entrySet()) {
            Bean bean = entry.getValue();
            Class<?> clazz = bean.getValue().getClass();

            Method[] allMethods = clazz.getDeclaredMethods();
            List<Method> annotatedMethods = Arrays.stream(allMethods)
                    .filter(method -> method.getAnnotation(PostConstruct.class) != null)
                    .toList();

            for (Method method : annotatedMethods) {
                try {
                    method.setAccessible(true);
                    method.invoke(bean.getValue());
                } catch (IllegalAccessException e) {
                    throw new ProcessPostConstructException("Access to private fields denied", e);
                }
            }
        }
    }

    @SneakyThrows
    private void createPostProcessBeans(Class<?> clazz, Map.Entry<String, BeanDefinition> entry) {
        if ((BeanPostProcessor.class).isAssignableFrom(clazz)) {
            BeanDefinition entryValue = entry.getValue();
            BeanPostProcessor newPostProcessor =
                    (BeanPostProcessor) Class.forName(clazz.getName()).getConstructor().newInstance();
            Bean newBean = new Bean(entryValue.getId(), newPostProcessor);
            serviceBeans.put(entry.getKey(), newBean);
        }
    }

    @SneakyThrows
    private void clarifyMethodAndInjectValue(Bean bean, String keyValue, String value) {
        try {
            Method[] methods = bean.getValue().getClass().getMethods();

            String methodName = "set" + keyValue.substring(0, 1).toUpperCase() + keyValue.substring(1);
            Method methodClass = Arrays.stream(methods)
                    .filter(method -> method.getName().equals(methodName))
                    .findFirst().orElseThrow(Exception::new);
            Class<?>[] parameterTypes = methodClass.getParameterTypes();
            String methodClassName = parameterTypes[0].getName();

            if (methodClassName.equals("int")) {
                Method neededMethod = bean.getValue().getClass().getMethod(methodName, Integer.TYPE);
                injectValue(bean.getValue(), neededMethod, value);
                return;
            }
            Method neededMethod = bean.getValue().getClass().getMethod(methodName, String.class);
            injectValue(bean.getValue(), neededMethod, String.valueOf(value));
        } catch (InvocationTargetException e) {
            e.getCause().printStackTrace();
        }
    }

    @SneakyThrows
    private void createFactoryPostProcessBeans(Class<?> clazz) {
        if ((BeanFactoryPostProcessor.class).isAssignableFrom(clazz)) {
            BeanFactoryPostProcessor newFactoryPostProcessor =
                    (BeanFactoryPostProcessor) Class.forName(clazz.getName()).getDeclaredConstructor().newInstance();
            serviceFactoryBeans.add(newFactoryPostProcessor);
        }
    }

    @SneakyThrows
    private void clarifyMethodAndInjectRefDependencies(Bean bean, String fieldName, Object value) {
        Method[] methods = bean.getValue().getClass().getMethods();
        String methodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        Method methodClass = Arrays.stream(methods)
                .filter(method -> method.getName().equals(methodName))
                .findFirst().orElseThrow(Exception::new);
        methodClass.invoke(bean.getValue(), value);
    }

    private void postProcessBeans() {
        for (Map.Entry<String, Bean> serviceEntry : serviceBeans.entrySet()) {
            Bean serviceBean = serviceEntry.getValue();
            BeanPostProcessor objectPostProcessor = (BeanPostProcessor) serviceBean.getValue();

            for (Map.Entry<String, Bean> beanEntry : beans.entrySet()) {
                Bean bean = beanEntry.getValue();

                callPostProcessBeforeInitialization(bean, objectPostProcessor);
                callInitMethods();
                callPostProcessAfterInitialization(bean, objectPostProcessor);
            }
        }
    }

}