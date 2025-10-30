package com.github.javydreamercsw;

import java.util.List;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  @Bean
  @ServiceConnection
  MySQLContainer<?> mysqlContainer() {
    return new MySQLContainer<>(DockerImageName.parse("mysql:latest"))
        .withUsername("test")
        .withPassword("test");
  }

  @Bean
  @ServiceConnection
  PostgreSQLContainer<?> postgresqlContainer() {
    return new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"))
        .withUsername("test")
        .withPassword("test");
  }

  @Bean
  @Primary
  public FlywayMigrationStrategy cleanMigrateStrategy(Environment environment) {
    return flyway -> {
      String[] activeProfiles = environment.getActiveProfiles();
      String flywayLocation = null;

      if (List.of(activeProfiles).contains("mysql")) {
        flywayLocation = "classpath:/db/migration/mysql";
      } else if (List.of(activeProfiles).contains("postgresql")) {
        flywayLocation = "classpath:/db/migration/postgresql";
      } else {
        // Default to H2 if no specific profile is active or for other profiles
        flywayLocation = "classpath:/db/migration/h2";
      }

      Flyway.configure()
          .dataSource(flyway.getConfiguration().getDataSource())
          .locations(flywayLocation)
          .load()
          .migrate();
    };
  }
}
