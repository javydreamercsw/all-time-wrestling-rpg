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

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.validator.AbstractValidator;

/** A custom validator for password strength. */
public class PasswordValidator extends AbstractValidator<String> {

  public PasswordValidator(String errorMessage) {
    super(errorMessage);
  }

  @Override
  public ValidationResult apply(String value, ValueContext context) {
    if (value == null || value.isEmpty()) {
      return ValidationResult.ok(); // Let RequiredValidator handle empty values
    }
    if (value.length() < 8) {
      return ValidationResult.error("Password must be at least 8 characters long");
    }
    if (!value.matches(".*[a-zA-Z].*")) {
      return ValidationResult.error("Password must contain at least one letter");
    }
    if (!value.matches(".*\\d.*")) {
      return ValidationResult.error("Password must contain at least one number");
    }
    return ValidationResult.ok();
  }
}
