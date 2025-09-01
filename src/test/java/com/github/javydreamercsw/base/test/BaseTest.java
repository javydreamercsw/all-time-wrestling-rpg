package com.github.javydreamercsw.base.test;

import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;

public abstract class BaseTest {
  /** Helper method to check if NOTION_TOKEN is available for conditional tests. */
  public static boolean isNotionTokenAvailable() {
    return EnvironmentVariableUtil.getNotionToken() != null;
  }
}
