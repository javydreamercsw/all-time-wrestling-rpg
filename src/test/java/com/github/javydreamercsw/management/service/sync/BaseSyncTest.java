package com.github.javydreamercsw.management.service.sync;

import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;

public abstract class BaseSyncTest {
  /** Helper method to check if NOTION_TOKEN is available for conditional tests. */
  static boolean isNotionTokenAvailable() {
    return EnvironmentVariableUtil.getNotionToken() != null;
  }
}
