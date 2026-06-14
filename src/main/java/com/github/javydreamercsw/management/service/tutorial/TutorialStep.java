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

  /**
   * Controls how the player completes this step.
   *
   * <ul>
   *   <li>{@code NAVIGATE} — show a "Go to X" button; player navigates away, returns, then
   *       validates.
   *   <li>{@code INLINE} — render a Vaadin component directly inside the tutorial view; no
   *       navigation required (e.g. wrestler picker on Step 1).
   * </ul>
   */
  enum InteractionMode {
    NAVIGATE,
    INLINE
  }

  /** 1-based step number within the tutorial. */
  int getStepNumber();

  /** Short headline shown as the step title. */
  String getTitle();

  /** Full instructions telling the player exactly what to do. */
  String getInstructions();

  /** Shown near the Validate button so the player knows what will be checked. */
  String getValidationHint();

  /** Returns how this step expects the player to interact. Defaults to {@code NAVIGATE}. */
  default InteractionMode getInteractionMode() {
    return InteractionMode.NAVIGATE;
  }

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
   * When this step uses an INLINE wrestler picker, returns the names of wrestlers the player is
   * allowed to choose. An empty list means all active wrestlers are eligible. The default
   * implementation returns an empty list (no restriction).
   */
  default java.util.List<String> getAllowedWrestlerNames() {
    return java.util.List.of();
  }

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
