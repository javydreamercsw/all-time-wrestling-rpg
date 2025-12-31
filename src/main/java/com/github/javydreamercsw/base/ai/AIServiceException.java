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
package com.github.javydreamercsw.base.ai;

import lombok.Getter;

/** Custom exception for AI service errors. */
@Getter
public class AIServiceException extends RuntimeException {

  private final int statusCode;
  private final String statusText;
  private final String provider;

  public AIServiceException(
      int statusCode, String statusText, String provider, String message, Throwable cause) {
    super(String.format("[%s] %s", provider, message), cause);
    this.statusCode = statusCode;
    this.statusText = statusText;
    this.provider = provider;
  }

  public AIServiceException(int statusCode, String statusText, String provider, String message) {
    super(String.format("[%s] %s", provider, message));
    this.statusCode = statusCode;
    this.statusText = statusText;
    this.provider = provider;
  }
}
