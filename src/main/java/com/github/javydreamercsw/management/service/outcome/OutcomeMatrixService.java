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
package com.github.javydreamercsw.management.service.outcome;

import com.github.javydreamercsw.management.domain.drama.DramaEventSeverity;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrix;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixCategory;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixEntry;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixEntryRepository;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixRepository;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixResult;
import com.github.javydreamercsw.management.service.drama.DramaEventService;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class OutcomeMatrixService {

  private final OutcomeMatrixRepository matrixRepository;
  private final OutcomeMatrixEntryRepository entryRepository;
  private final RivalryService rivalryService;
  private final InjuryService injuryService;
  private final DramaEventService dramaEventService;
  private final WrestlerService wrestlerService;
  private final Random random;

  @Autowired
  public OutcomeMatrixService(
      final OutcomeMatrixRepository matrixRepository,
      final OutcomeMatrixEntryRepository entryRepository,
      final RivalryService rivalryService,
      final InjuryService injuryService,
      final DramaEventService dramaEventService,
      final WrestlerService wrestlerService,
      final Random random) {
    this.matrixRepository = matrixRepository;
    this.entryRepository = entryRepository;
    this.rivalryService = rivalryService;
    this.injuryService = injuryService;
    this.dramaEventService = dramaEventService;
    this.wrestlerService = wrestlerService;
    this.random = random;
  }

  // -------------------------------------------------------------------------
  // CRUD
  // -------------------------------------------------------------------------

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public OutcomeMatrix createMatrix(@NonNull final OutcomeMatrix matrix) {
    return matrixRepository.save(matrix);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public OutcomeMatrix updateMatrix(@NonNull final OutcomeMatrix matrix) {
    return matrixRepository.save(matrix);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public void deleteMatrix(@NonNull final Long matrixId) {
    matrixRepository.deleteById(matrixId);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<OutcomeMatrix> getAll() {
    return matrixRepository.findAll();
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<OutcomeMatrix> getById(@NonNull final Long matrixId) {
    return matrixRepository.findById(matrixId);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<OutcomeMatrix> getByCategory(@NonNull final OutcomeMatrixCategory category) {
    return matrixRepository.findByCategory(category);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<OutcomeMatrix> getByName(@NonNull final String name) {
    return matrixRepository.findByNameIgnoreCase(name);
  }

  public long count() {
    return matrixRepository.count();
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public OutcomeMatrixEntry addEntry(
      @NonNull final Long matrixId, @NonNull final OutcomeMatrixEntry entry) {
    OutcomeMatrix matrix =
        matrixRepository
            .findById(matrixId)
            .orElseThrow(() -> new EntityNotFoundException("OutcomeMatrix not found: " + matrixId));
    entry.setMatrix(matrix);
    return entryRepository.save(entry);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public OutcomeMatrixEntry updateEntry(@NonNull final OutcomeMatrixEntry entry) {
    return entryRepository.save(entry);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public void deleteEntry(@NonNull final Long entryId) {
    entryRepository.deleteById(entryId);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<OutcomeMatrixEntry> getEntries(@NonNull final Long matrixId) {
    OutcomeMatrix matrix =
        matrixRepository
            .findById(matrixId)
            .orElseThrow(() -> new EntityNotFoundException("OutcomeMatrix not found: " + matrixId));
    return entryRepository.findByMatrixOrderByDiceRollAsc(matrix);
  }

  // -------------------------------------------------------------------------
  // Roll resolution
  // -------------------------------------------------------------------------

  /**
   * Resolves a dice roll against the given matrix, substituting template variables. Does NOT apply
   * effects — call applyEffects() separately when side effects are desired.
   *
   * @param matrixId the chart to look up
   * @param diceRoll d66 value (11–66)
   * @param variables map of placeholder → replacement (e.g. "FAVORED" → "El Fuego")
   * @return resolved result with rendered text; redirect field populated when entry is a redirect
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<OutcomeMatrixResult> resolveRoll(
      @NonNull final Long matrixId,
      final int diceRoll,
      @NonNull final Map<String, String> variables) {

    OutcomeMatrix matrix =
        matrixRepository
            .findById(matrixId)
            .orElseThrow(() -> new EntityNotFoundException("OutcomeMatrix not found: " + matrixId));

    Optional<OutcomeMatrixEntry> entryOpt =
        entryRepository.findByMatrixAndDiceRoll(matrix, diceRoll);
    if (entryOpt.isEmpty()) {
      log.warn(
          "No entry for diceRoll={} in matrix '{}' (id={})", diceRoll, matrix.getName(), matrixId);
      return Optional.empty();
    }

    OutcomeMatrixEntry entry = entryOpt.get();
    String rendered = substituteVariables(entry.getTemplateText(), variables);

    OutcomeMatrix redirect = null;
    if (entry.isRedirect()) {
      redirect = entry.getRedirectToMatrix();
    }

    return Optional.of(new OutcomeMatrixResult(entry, rendered, redirect));
  }

  /**
   * Picks a random entry from a chart matching the given category and resolves template variables.
   * Useful for drama-event generation and post-match narrative where the specific roll is
   * irrelevant. Returns empty if no matrix or no entries exist for the category.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<OutcomeMatrixResult> resolveRandomRoll(
      @NonNull final OutcomeMatrixCategory category, @NonNull final Map<String, String> variables) {

    List<OutcomeMatrix> matrices = matrixRepository.findByCategory(category);
    if (matrices.isEmpty()) {
      log.warn("No outcome matrix found for category {}", category);
      return Optional.empty();
    }

    OutcomeMatrix matrix = matrices.get(random.nextInt(matrices.size()));
    List<OutcomeMatrixEntry> entries = entryRepository.findByMatrixOrderByDiceRollAsc(matrix);
    if (entries.isEmpty()) {
      log.warn("No entries in outcome matrix '{}' (id={})", matrix.getName(), matrix.getId());
      return Optional.empty();
    }

    OutcomeMatrixEntry entry = entries.get(random.nextInt(entries.size()));
    String rendered = substituteVariables(entry.getTemplateText(), variables);
    OutcomeMatrix redirect = entry.isRedirect() ? entry.getRedirectToMatrix() : null;
    return Optional.of(new OutcomeMatrixResult(entry, rendered, redirect));
  }

  /**
   * Applies mechanical effects from a resolved entry and records a DramaEvent for history/inbox.
   * Callers who have the rendered narrative text should use the overload that accepts it.
   */
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public void applyEffects(
      @NonNull final OutcomeMatrixEntry entry,
      @NonNull final Long primaryWrestlerId,
      final Long secondaryWrestlerId,
      @NonNull final Long universeId) {
    applyEffects(entry, primaryWrestlerId, secondaryWrestlerId, universeId, null);
  }

  /**
   * Applies mechanical effects from a resolved entry and records a DramaEvent for history/inbox.
   * The {@code renderedText} is stored as the DramaEvent description so the narrative is queryable
   * later. Redirect entries are skipped — the final resolved entry creates the record.
   */
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public void applyEffects(
      @NonNull final OutcomeMatrixEntry entry,
      @NonNull final Long primaryWrestlerId,
      final Long secondaryWrestlerId,
      @NonNull final Long universeId,
      final String renderedText) {

    boolean hasText = renderedText != null && !renderedText.isBlank();

    if (!entry.hasEffects() && !hasText) {
      return;
    }

    String reason =
        "From outcome chart: " + entry.getMatrix().getName() + " roll " + entry.getDiceRoll();

    if (entry.getHeatDelta() != null && secondaryWrestlerId != null) {
      rivalryService.addHeatBetweenWrestlers(
          primaryWrestlerId, secondaryWrestlerId, entry.getHeatDelta(), reason, universeId);
    }

    if (entry.getFanDelta() != null && entry.getFanDelta() != 0) {
      wrestlerService.awardFans(primaryWrestlerId, universeId, entry.getFanDelta());
    }

    if (entry.getGrudgeGradeDelta() != null && entry.getGrudgeGradeDelta() != 0) {
      log.debug(
          "grudgeGradeDelta={} noted for wrestler {} (no target entity yet)",
          entry.getGrudgeGradeDelta(),
          primaryWrestlerId);
    }

    if (entry.isInjuryCaused()) {
      injuryService.createInjuryFromBumps(primaryWrestlerId, universeId);
    }

    // Redirect entries defer DramaEvent creation to the final resolved entry.
    if (!entry.isRedirect()) {
      String description =
          hasText
              ? renderedText
              : "Outcome chart: " + entry.getMatrix().getName() + " roll " + entry.getDiceRoll();
      String title = entry.getMatrix().getName() + " — Roll " + entry.getDiceRoll();

      dramaEventService.createOutcomeMatrixEvent(
          primaryWrestlerId,
          secondaryWrestlerId,
          universeId,
          title,
          description,
          deriveSeverity(entry),
          entry.getHeatDelta(),
          entry.isInjuryCaused());
    }
  }

  // -------------------------------------------------------------------------
  // Internal helpers
  // -------------------------------------------------------------------------

  private DramaEventSeverity deriveSeverity(final OutcomeMatrixEntry entry) {
    if (entry.isInjuryCaused()) {
      return DramaEventSeverity.MAJOR;
    }
    if (entry.getHeatDelta() != null) {
      if (entry.getHeatDelta() > 3) {
        return DramaEventSeverity.NEGATIVE;
      }
      if (entry.getHeatDelta() > 0) {
        return DramaEventSeverity.NEUTRAL;
      }
      if (entry.getHeatDelta() < 0) {
        return DramaEventSeverity.POSITIVE;
      }
    }
    return DramaEventSeverity.NEUTRAL;
  }

  private String substituteVariables(final String template, final Map<String, String> variables) {
    String result = template;
    for (Map.Entry<String, String> var : variables.entrySet()) {
      result = result.replace(var.getKey(), var.getValue());
    }
    return result;
  }
}
