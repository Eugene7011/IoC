package com.study.ioc.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class BeanDefinition {
    private final String id;
    private String className;
    private Map<String, String> valueDependencies = new HashMap<>();
    private Map<String, String> refDependencies = new HashMap<>();

    public BeanDefinition(String id, String className) {
        this.id = id;
        this.className = className;
    }
}
