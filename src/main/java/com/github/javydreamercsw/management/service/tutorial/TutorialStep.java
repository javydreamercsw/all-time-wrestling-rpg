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
package com.github.javydreamercsw.management.service.tutorial;

import com.github.javydreamercsw.base.domain.account.Account;

/** Defines one step in a mode-specific player tutorial. */
public interface TutorialStep {

  /** 1-based step number within the tutorial. */
  int getStepNumber();

  /** Short headline shown as the step title. */
  String getTitle();

  /** Full instructions telling the player exactly what to do. */
  String getInstructions();

  /** Shown near the Validate button so the player knows what will be checked. */
  String getValidationHint();

  /** Vaadin route the "Go to X" button navigates to (e.g. {@code "player"}). */
  String getTargetRoute();

  /** Human label for the "Go to X" button (e.g. {@code "Player Dashboard"}). */
  String getTargetViewLabel();

  /**
   * Optional path to a tutorial screenshot image served under {@code /images/tutorial/}. Returns
   * {@code null} when no image is available — the view hides the image slot gracefully.
   */
  String getImagePath();

  /**
   * Validates that the player completed the step's required action.
   *
   * @return {@code null} on success; a human-readable error message when the action is not yet
   *     done.
   */
  String validate(Account account);

  /**
   * Called before the step is displayed, executed under admin security context (via {@code
   * GeneralSecurityUtils.runAsAdmin}). Implementations may call services requiring elevated
   * authority — for example, seeding a default campaign or league so the player has something to
   * join. <strong>Must be idempotent</strong>: it is called every time the step renders, including
   * after navigating backwards.
   */
  default void beforeStep(Account account) {}

  /**
   * Called after {@link #validate} returns {@code null} (step succeeded) and before the step index
   * is advanced. Executed under the same admin security context as {@link #beforeStep}.
   */
  default void afterStep(Account account) {}
}
