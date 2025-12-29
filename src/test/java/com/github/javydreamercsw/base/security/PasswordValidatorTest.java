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
package com.github.javydreamercsw.base.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.data.binder.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PasswordValidatorTest {

  private PasswordValidator validator;

  @BeforeEach
  void setUp() {
    validator = new PasswordValidator("Invalid password");
  }

  @Test
  void testValidPassword() {
    ValidationResult result = validator.apply("Valid123", null);
    assertFalse(result.isError());
  }

  @Test
  void testTooShort() {
    ValidationResult result = validator.apply("Short1", null);
    assertTrue(result.isError());
    assertEquals("Password must be at least 8 characters long", result.getErrorMessage());
  }

  @Test
  void testNoLetter() {
    ValidationResult result = validator.apply("12345678", null);
    assertTrue(result.isError());
    assertEquals("Password must contain at least one letter", result.getErrorMessage());
  }

  @Test
  void testNoNumber() {
    ValidationResult result = validator.apply("Password", null);
    assertTrue(result.isError());
    assertEquals("Password must contain at least one number", result.getErrorMessage());
  }

  @Test
  void testNullPassword() {
    ValidationResult result = validator.apply(null, null);
    assertFalse(result.isError());
  }

  @Test
  void testEmptyPassword() {
    ValidationResult result = validator.apply("", null);
    assertFalse(result.isError());
  }
}
