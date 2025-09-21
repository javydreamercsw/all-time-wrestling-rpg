package com.github.javydreamercsw;

import com.github.javydreamercsw.management.DataInitializer;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  // TODO Configure your Testcontainers here.
  //  See https://docs.spring.io/spring-boot/reference/testing/testcontainers.html for details.

  @Bean
  public DataInitializer dataInitializer(SegmentTypeService segmentTypeService) {
    DataInitializer dataInitializer = new DataInitializer();
    dataInitializer.loadSegmentTypesFromFile(segmentTypeService);
    return dataInitializer;
  }
}
