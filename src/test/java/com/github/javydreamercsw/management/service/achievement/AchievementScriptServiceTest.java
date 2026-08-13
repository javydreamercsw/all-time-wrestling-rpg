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

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AchievementScriptServiceTest {

  private final AchievementScriptService service = new AchievementScriptService();

  @Test
  @DisplayName("Groovy script returning true unlocks the achievement")
  void testScriptReturningTrue() {
    assertThat(service.evaluate("true", Map.of())).isTrue();
  }

  @Test
  @DisplayName("Groovy script returning false does not unlock the achievement")
  void testScriptReturningFalse() {
    assertThat(service.evaluate("false", Map.of())).isFalse();
  }

  @Test
  @DisplayName("Script can read bound context variables")
  void testScriptCanAccessContext() {
    assertThat(
            service.evaluate("wrestlers.size() >= 2", Map.of("wrestlers", java.util.List.of(1, 2))))
        .isTrue();
    assertThat(service.evaluate("wrestlers.size() >= 2", Map.of("wrestlers", java.util.List.of(1))))
        .isFalse();
  }

  @Test
  @DisplayName("Script that throws returns false and does not propagate")
  void testScriptExceptionReturnsFalse() {
    assertThat(service.evaluate("throw new RuntimeException('boom')", Map.of())).isFalse();
  }

  @Test
  @DisplayName("Null or blank script is treated as not-unlocked")
  void testNullOrBlankScript() {
    assertThat(service.evaluate(null, Map.of())).isFalse();
    assertThat(service.evaluate("", Map.of())).isFalse();
    assertThat(service.evaluate("   ", Map.of())).isFalse();
  }

  @Test
  @DisplayName("Non-boolean script result is treated as not-unlocked")
  void testNonBooleanResultReturnsFalse() {
    assertThat(service.evaluate("'not a boolean'", Map.of())).isFalse();
  }

  @Test
  @DisplayName("Repeated evaluation of the same script reuses the compiled class")
  void testRepeatedEvaluationIsConsistent() {
    String script = "wrestlers.size() >= 2";
    for (int i = 0; i < 3; i++) {
      assertThat(service.evaluate(script, Map.of("wrestlers", java.util.List.of(1, 2)))).isTrue();
    }
  }
}
