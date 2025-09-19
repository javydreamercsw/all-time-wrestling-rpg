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
    super(message, cause);
    this.statusCode = statusCode;
    this.statusText = statusText;
    this.provider = provider;
  }

  public AIServiceException(int statusCode, String statusText, String provider, String message) {
    super(message);
    this.statusCode = statusCode;
    this.statusText = statusText;
    this.provider = provider;
  }
}
