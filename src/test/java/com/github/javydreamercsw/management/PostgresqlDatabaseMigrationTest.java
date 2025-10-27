package com.github.javydreamercsw.management;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javydreamercsw.Application;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(classes = {Application.class})
@ActiveProfiles("postgresql")
@TestPropertySource(
    properties = {"notion.sync.enabled=false", "spring.flyway.repair-on-validate=true"})
public class PostgresqlDatabaseMigrationTest {

  @Container
  public static PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:13.3");

  @DynamicPropertySource
  static void registerPostgreSQLProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgresql::getJdbcUrl);
    registry.add("spring.datasource.username", postgresql::getUsername);
    registry.add("spring.datasource.password", postgresql::getPassword);
    registry.add("spring.flyway.locations", () -> "classpath:db/migration/postgresql");
  }

  @Test
  public void testPostgresqlMigration() throws SQLException {
    try (Connection conn =
            DriverManager.getConnection(
                postgresql.getJdbcUrl(), postgresql.getUsername(), postgresql.getPassword());
        Statement stmt = conn.createStatement()) {

      ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM faction");
      rs.next();
      assertEquals(0, rs.getInt(1));
    }
  }
}
