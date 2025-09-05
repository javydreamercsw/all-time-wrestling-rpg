package com.github.javydreamercsw.management.config;

import com.github.javydreamercsw.management.DataInitializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@TestConfiguration
@ComponentScan(
    excludeFilters =
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = DataInitializer.class))
public class TestConfig {}
