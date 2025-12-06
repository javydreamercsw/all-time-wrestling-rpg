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
