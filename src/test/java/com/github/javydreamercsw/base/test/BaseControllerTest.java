package com.github.javydreamercsw.base.test;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {"spring.main.allow-bean-definition-overriding=true"})
public abstract class BaseControllerTest {}
