package com.study.ioc.context.impl;

import com.study.ioc.context.ApplicationContext;
import com.study.ioc.entity.Bean;
import com.study.ioc.entity.BeanDefinition;
import com.study.ioc.exception.BeanInstantiationException;
import com.study.ioc.exception.NoSuchBeanDefinitionException;
import com.study.ioc.exception.NoUniqueBeanOfTypeException;
import com.study.ioc.reader.BeanDefinitionReader;
import com.study.ioc.reader.sax.XmlBeanDefinitionReader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class GenericApplicationContext implements ApplicationContext {

    private Map<String, Bean> beans = new HashMap<>();

    GenericApplicationContext() {
    }

    public GenericApplicationContext(String... paths) throws InstantiationException, IllegalAccessException {
        this(new XmlBeanDefinitionReader(paths));
    }

    public GenericApplicationContext(BeanDefinitionReader definitionReader) throws InstantiationException, IllegalAccessException {
        Map<String, BeanDefinition> beanDefinitions = definitionReader.getBeanDefinition();

        beans = createBeans(beanDefinitions);
        injectValueDependencies(beanDefinitions, beans);
        injectRefDependencies(beanDefinitions, beans);
    }

    @Override
    public Object getBean(String beanId) {
        return beans.get(beanId).getValue();
    }

    @Override
    public <T> T getBean(Class<T> clazz) {
        validateIfBeansUnique();
        for (Map.Entry<String, Bean> entry : beans.entrySet()) {
            Object value = entry.getValue().getValue();

            if (clazz.equals(value.getClass())) {
                return (T) value;
            }
        }
        return null;
    }

    @Override
    public <T> T getBean(String id, Class<T> clazz) {
        for (Map.Entry<String, Bean> entry : beans.entrySet()) {
            if (id.equals(entry.getKey())) {
                Class<?> aClass = entry.getValue().getValue().getClass();
                if (!clazz.equals(aClass)) {
                    throw new NoSuchBeanDefinitionException(id, clazz.getName(), aClass.getName());
                }
                return (T) entry.getValue().getValue();
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
                Object object = Class.forName(entry.getValue().getClassName()).newInstance();
                Bean newBean = new Bean(entry.getValue().getId(), object);
                beans.put(entry.getKey(), newBean);
            }
        } catch (ClassNotFoundException e) {
            throw new BeanInstantiationException("BeanInstantiation failed", new Throwable());
        }
        return beans;
    }

    void injectValueDependencies(Map<String, BeanDefinition> beanDefinitions, Map<String, Bean> beans) {
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitions.entrySet()) {
            String key = entry.getKey();
            Bean bean = beans.get(key);
            Map<String, String> valueDependencies = entry.getValue().getValueDependencies();
            valueDependencies.forEach((k, v) ->
            {
                try {
                    clarifyMethodAndInjectValue(bean, k, v);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            });

        }
    }

    void injectRefDependencies(Map<String, BeanDefinition> beanDefinitions, Map<String, Bean> beans) {
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitions.entrySet()) {
            String key = entry.getKey();
            Bean bean = beans.get(key);
            Map<String, String> refDependencies = entry.getValue().getRefDependencies();

            refDependencies.forEach((k, v) ->
            {
                try {
                    Object value = beans.get(v).getValue();
                    clarifyMethodAndInjectRefDependencies(bean, k, value);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void clarifyMethodAndInjectRefDependencies(Bean bean, String k, Object value) throws InvocationTargetException, IllegalAccessException {
        Method[] declaredMethods = bean.getValue().getClass().getDeclaredMethods();
        String methodName = "set" + k.substring(0, 1).toUpperCase() + k.substring(1);
        Method methodClass = Arrays.stream(declaredMethods)
                .filter(method -> method.getName().equals(methodName))
                .findFirst().get();
        methodClass.invoke(bean.getValue(), value);
    }

    void injectValue(Object object, Method classMethod, String propertyValue) throws ReflectiveOperationException {
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

    private void validateIfBeansUnique() {
        List<? extends Class<?>> classes = beans.values().stream().map(bean -> bean.getValue().getClass()).toList();
        HashSet<Object> beanHashSet = new HashSet<>(classes);
        if (classes.size() != beanHashSet.size()) {
            throw new NoUniqueBeanOfTypeException("There are duplicates in beans values");
        }
    }

    private void clarifyMethodAndInjectValue(Bean bean, String k, String v) throws ReflectiveOperationException {
        Method[] declaredMethods = bean.getValue().getClass().getDeclaredMethods();
        String methodName = "set" + k.substring(0, 1).toUpperCase() + k.substring(1);
        Method methodClass = Arrays.stream(declaredMethods)
                .filter(method -> method.getName().equals(methodName))
                .findFirst().get();
        Class<?>[] parameterTypes = methodClass.getParameterTypes();
        String methodClassName = parameterTypes[0].getName();
        if (methodClassName.equals("int")) {
            Method neededMethod = bean.getValue().getClass().getDeclaredMethod(methodName, Integer.TYPE);
            injectValue(bean.getValue(), neededMethod, v);
            return;
        }
        Method neededMethod = bean.getValue().getClass().getDeclaredMethod(methodName, String.class);
        injectValue(bean.getValue(), neededMethod, String.valueOf(v));
    }

}
