package com.github.javydreamercsw;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import java.time.Clock;
import java.util.Random;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@Theme("default")
public class Application implements AppShellConfigurator {

  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone(); // You can also use Clock.systemUTC()
  }

  @Bean
  public Random random() {
    return new Random();
  }

  // Added Flyway programmatic migration bean
  @Bean
  public CommandLineRunner flywayCommandLineRunner(DataSource dataSource) {
    return args -> {
      Flyway flyway =
          Flyway.configure()
              .dataSource(dataSource)
              .locations("classpath:db/migration")
              .baselineOnMigrate(true) // Allow Flyway to take over existing schemas
              .load();
      flyway.repair();
      flyway.migrate();
    };
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
