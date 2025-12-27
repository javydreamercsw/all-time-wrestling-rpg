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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CustomPasswordValidatorTest {

  @Test
  void testValidPassword() {
    Assertions.assertTrue(CustomPasswordValidator.isValid("Password123!"));
  }

  @Test
  void testPasswordTooShort() {
    Assertions.assertFalse(CustomPasswordValidator.isValid("Pass1!"));
  }

  @Test
  void testPasswordMissingUppercase() {
    Assertions.assertFalse(CustomPasswordValidator.isValid("password123!"));
  }

  @Test
  void testPasswordMissingLowercase() {
    Assertions.assertFalse(CustomPasswordValidator.isValid("PASSWORD123!"));
  }

  @Test
  void testPasswordMissingDigit() {
    Assertions.assertFalse(CustomPasswordValidator.isValid("Password!!"));
  }

  @Test
  void testPasswordMissingSpecialCharacter() {
    Assertions.assertFalse(CustomPasswordValidator.isValid("Password123"));
  }

  @Test
  void testPasswordWithWhitespace() {
    Assertions.assertFalse(CustomPasswordValidator.isValid("Password 123!"));
  }
}
