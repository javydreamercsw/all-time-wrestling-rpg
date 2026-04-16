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
package com.github.javydreamercsw.base.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;
import org.junit.jupiter.api.Test;

class PasswordValidatorTest {

  private final PasswordValidator validator = new PasswordValidator("Invalid password");

  @Test
  void testApply() {
    ValueContext context = new ValueContext();

    assertThat(validator.apply(null, context)).isEqualTo(ValidationResult.ok());
    assertThat(validator.apply("", context)).isEqualTo(ValidationResult.ok());

    ValidationResult result = validator.apply("short", context);
    assertThat(result.isError()).isTrue();
    assertThat(result.getErrorMessage()).contains("at least 8 characters");

    result = validator.apply("nonumbers", context);
    assertThat(result.isError()).isTrue();
    assertThat(result.getErrorMessage()).contains("at least one number");

    result = validator.apply("12345678", context);
    assertThat(result.isError()).isTrue();
    assertThat(result.getErrorMessage()).contains("at least one letter");

    assertThat(validator.apply("password123", context)).isEqualTo(ValidationResult.ok());
  }
}
