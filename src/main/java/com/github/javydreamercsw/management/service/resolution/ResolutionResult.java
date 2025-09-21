package com.github.javydreamercsw.management.service.resolution;

import org.jspecify.annotations.Nullable;

/**
 * Represents the result of a rivalry resolution attempt.
 *
 * @param <T> the type of entity involved in the rivalry (e.g., Rivalry, FactionRivalry)
 * @param resolved true if the rivalry was resolved, false otherwise
 * @param message A message describing the outcome of the resolution attempt
 * @param entity The rivalry entity that was the subject of the resolution attempt
 * @param roll1 The first roll in the resolution attempt
 * @param roll2 The second roll in the resolution attempt
 * @param totalRoll The sum of the rolls
 */
public record ResolutionResult<T>(
    boolean resolved, String message, @Nullable T entity, int roll1, int roll2, int totalRoll) {}
