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
package com.github.javydreamercsw;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Assigns each E2E test class to a shard by hashing its simple name, enabling parallel GitHub
 * Actions matrix jobs without maintaining explicit class lists.
 *
 * <p>Activated by setting both {@code e2e.shard.index} (1-based) and {@code e2e.shard.total} system
 * properties, e.g. {@code -De2e.shard.index=1 -De2e.shard.total=2}. When either property is absent
 * all tests run normally — no change to local dev or single-job CI runs.
 */
public class E2EShardCondition implements ExecutionCondition {

  private static final ConditionEvaluationResult ENABLED_NO_SHARDING =
      ConditionEvaluationResult.enabled("Sharding not configured — running all tests");

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    // Only evaluate at class level; method-level defers to the class-level result.
    if (context.getTestMethod().isPresent()) {
      return ConditionEvaluationResult.enabled("Method-level: deferred to class-level shard check");
    }

    String indexProp = System.getProperty("e2e.shard.index");
    String totalProp = System.getProperty("e2e.shard.total");
    if (indexProp == null || totalProp == null) {
      return ENABLED_NO_SHARDING;
    }

    int shardIndex = Integer.parseInt(indexProp.trim()); // 1-based
    int shardTotal = Integer.parseInt(totalProp.trim());

    String className = context.getRequiredTestClass().getSimpleName();
    int assigned = (Math.abs(className.hashCode()) % shardTotal) + 1;

    if (assigned == shardIndex) {
      return ConditionEvaluationResult.enabled(
          className + " → shard " + assigned + " of " + shardTotal);
    }
    return ConditionEvaluationResult.disabled(
        className
            + " → shard "
            + assigned
            + " of "
            + shardTotal
            + " (skipping shard "
            + shardIndex
            + ")");
  }
}
