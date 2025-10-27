package com.github.javydreamercsw.management;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javydreamercsw.Application;
import com.github.javydreamercsw.management.service.sync.NotionSyncService;
import com.github.javydreamercsw.management.service.sync.parallel.ParallelSyncOrchestrator;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(classes = {Application.class})
@ActiveProfiles("mysql")
@TestPropertySource(properties = "notion.sync.enabled=false")
public class MySQLDatabaseMigrationTest {

  @MockBean NotionSyncService notionSyncService;
  @MockBean ParallelSyncOrchestrator parallelSyncOrchestrator;

  @Container public static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.33");

  @DynamicPropertySource
  static void registerMySQLProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
    registry.add("spring.flyway.locations", () -> "classpath:db/migration/mysql");
  }

  @Test
  public void testMysqlMigration() throws SQLException {
    try (Connection conn =
            DriverManager.getConnection(
                mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
        Statement stmt = conn.createStatement()) {

      ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM faction");
      rs.next();
      assertEquals(0, rs.getInt(1));
    }
  }
}
