/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw;

import static com.github.javydreamercsw.base.domain.account.RoleName.ADMIN_ROLE;

import com.github.javydreamercsw.base.AccountInitializer;
import com.github.javydreamercsw.base.service.ranking.RankingService;
import com.github.javydreamercsw.management.DataInitializer;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Clock;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.github.javydreamercsw")
@Slf4j
public class Application extends SpringBootServletInitializer {

  static {
    // Enable InheritableThreadLocal to ensure background threads (like AI generation)
    // inherit the security context from the parent UI thread.
    SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
  }

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    return application.sources(Application.class);
  }

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  public Random random() {
    return new Random();
  }

  @Bean
  @Profile("!test")
  public CommandLineRunner initData(
      AccountInitializer accountInitializer, DataInitializer dataInitializer) {
    return args -> {
      log.info("Initializing data on startup...");
      // Create a system authentication context
      UsernamePasswordAuthenticationToken auth =
          new UsernamePasswordAuthenticationToken(
              "system",
              null,
              List.of(
                  new SimpleGrantedAuthority("ROLE_" + ADMIN_ROLE),
                  new SimpleGrantedAuthority("ADMIN")));
      SecurityContextHolder.getContext().setAuthentication(auth);
      try {
        accountInitializer.init();
        dataInitializer.init();
      } finally {
        // Clear the context
        SecurityContextHolder.clearContext();
      }
      log.info("Data initialization complete.");
    };
  }

  @Bean
  @Profile("test & !e2e")
  public CommandLineRunner recalculateRanking(
      RankingService rankingService, WrestlerRepository wrestlerRepository) {
    return args -> {
      log.info("Recalculating tiers on startup...");
      if (SecurityContextHolder.getContext().getAuthentication() == null
          || !SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
        log.warn(
            "Skipping tier recalculation on startup: No authenticated user in SecurityContext.");
        return;
      }
      rankingService.recalculateRanking(new java.util.ArrayList<>(wrestlerRepository.findAll()));
      log.info("Tier recalculation complete.");
    };
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
