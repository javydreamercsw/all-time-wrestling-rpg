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
 * Assigns each unit test class to a shard by hashing its simple name — the unit-phase counterpart
 * of {@link E2EShardCondition}. Unit tests have no common base class to register an extension, so
 * this one is discovered via the JUnit Platform ServiceLoader (see {@code
 * META-INF/services/org.junit.jupiter.api.extension.Extension}) and only activates when
 * autodetection is enabled.
 *
 * <p>Activated by setting both {@code unit.shard.index} (1-based) and {@code unit.shard.total}
 * system properties together with {@code -Djunit.jupiter.extensions.autodetection.enabled=true},
 * e.g. in a sharded CI matrix. When either shard property is absent every class runs normally — no
 * change to local dev or single-job CI runs.
 */
public class UnitTestShardCondition implements ExecutionCondition {

  private static final ConditionEvaluationResult ENABLED_NO_SHARDING =
      ConditionEvaluationResult.enabled("Sharding not configured — running all tests");

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    // Only evaluate at class level; method-level defers to the class-level shard check.
    if (context.getTestMethod().isPresent()) {
      return ConditionEvaluationResult.enabled("Method-level: deferred to class-level shard check");
    }

    String indexProp = System.getProperty("unit.shard.index");
    String totalProp = System.getProperty("unit.shard.total");
    if (indexProp == null || totalProp == null) {
      return ENABLED_NO_SHARDING;
    }

    int shardIndex = Integer.parseInt(indexProp.trim()); // 1-based
    int shardTotal = Integer.parseInt(totalProp.trim());

    String className = context.getRequiredTestClass().getSimpleName();
    int assigned = (Math.abs(className.hashCode()) % shardTotal) + 1;

    if (assigned == shardIndex) {
      return ConditionEvaluationResult.enabled(
          className + " → unit shard " + assigned + " of " + shardTotal);
    }
    return ConditionEvaluationResult.disabled(
        className
            + " → unit shard "
            + assigned
            + " of "
            + shardTotal
            + " (skipping shard "
            + shardIndex
            + ")");
  }
}
