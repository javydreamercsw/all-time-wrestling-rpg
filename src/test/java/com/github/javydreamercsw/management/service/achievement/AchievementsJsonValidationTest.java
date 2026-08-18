/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.achievement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.domain.account.Achievement;
import java.io.InputStream;
import java.util.List;
import org.codehaus.groovy.control.CompilationFailedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Validates the achievements.json file on every build:
 *
 * <ul>
 *   <li>File parses as valid JSON into {@link Achievement} objects
 *   <li>Every entry has the required fields: key, name, description, xpValue, category
 *   <li>Every {@code unlockCondition} (when present) compiles as syntactically valid Groovy
 * </ul>
 *
 * A compile failure here means a typo was introduced in a Groovy expression that would silently
 * return {@code false} in production rather than unlocking the achievement.
 */
class AchievementsJsonValidationTest {

  private static List<Achievement> achievements;
  private static final groovy.lang.GroovyClassLoader groovyClassLoader =
      new groovy.lang.GroovyClassLoader();

  @BeforeAll
  static void loadAchievementsJson() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    try (InputStream is =
        AchievementsJsonValidationTest.class
            .getClassLoader()
            .getResourceAsStream("achievements.json")) {
      assertThat(is).as("achievements.json must exist on the classpath").isNotNull();
      achievements = mapper.readValue(is, new TypeReference<>() {});
    }
  }

  @Test
  void achievementsJsonParsesSuccessfully() {
    assertThat(achievements).isNotEmpty();
  }

  @Test
  void everyAchievementHasRequiredFields() {
    for (Achievement a : achievements) {
      assertThat(a.getKey()).as("key must not be blank in achievement: %s", a).isNotBlank();
      assertThat(a.getName()).as("name must not be blank for key: %s", a.getKey()).isNotBlank();
      assertThat(a.getDescription())
          .as("description must not be blank for key: %s", a.getKey())
          .isNotBlank();
      assertThat(a.getXpValue()).as("xpValue must not be null for key: %s", a.getKey()).isNotNull();
      assertThat(a.getCategory())
          .as("category must not be null for key: %s", a.getKey())
          .isNotNull();
    }
  }

  @Test
  void everyUnlockConditionCompiles() {
    List<String> failures =
        achievements.stream()
            .filter(a -> a.getUnlockCondition() != null && !a.getUnlockCondition().isBlank())
            .filter(a -> !compilesCleanly(a.getUnlockCondition()))
            .map(Achievement::getKey)
            .toList();

    if (!failures.isEmpty()) {
      fail(
          "The following achievements have unlockCondition scripts that fail to compile: "
              + failures);
    }
  }

  private boolean compilesCleanly(final String script) {
    try {
      // parseClass() throws CompilationFailedException on syntax errors.
      // AchievementScriptService.evaluate() swallows compile errors as false,
      // so we go directly to the class loader to distinguish syntax failures
      // from scripts that legitimately evaluate to false.
      groovyClassLoader.parseClass(script);
      return true;
    } catch (CompilationFailedException e) {
      return false;
    }
  }
}
