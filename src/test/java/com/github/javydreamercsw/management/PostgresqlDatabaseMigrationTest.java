package com.github.javydreamercsw.management;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@ActiveProfiles("postgresql")
public class PostgresqlDatabaseMigrationTest {

    @Container
    public static PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:13.3");

    @Test
    public void testPostgresqlMigration() throws SQLException {
        try (Connection conn = DriverManager.getConnection(postgresql.getJdbcUrl(), postgresql.getUsername(), postgresql.getPassword());
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM faction");
            rs.next();
            assertEquals(0, rs.getInt(1));
        }
    }
}
