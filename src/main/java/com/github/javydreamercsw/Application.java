package com.github.javydreamercsw;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import java.time.Clock;
import java.util.Random;
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

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
