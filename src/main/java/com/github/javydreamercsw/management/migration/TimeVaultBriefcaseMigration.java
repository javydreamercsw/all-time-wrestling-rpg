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
package com.github.javydreamercsw.management.migration;

import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleOpportunityRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntryStatus;
import com.github.javydreamercsw.management.domain.tournament.TournamentRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.title.TitleOpportunityService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-time repair (ATW-8p72) for installs that represented the Money in the Bank equivalent with a
 * placeholder championship: a recurring "Time Vault" tournament linked to an unused "Time Vault"
 * title row. The proper mechanic is the briefcase ({@link
 * com.github.javydreamercsw.management.domain.title.TitleOpportunity}), so this migration:
 *
 * <ol>
 *   <li>flags every "Time Vault%" tournament as briefcase-deciding and detaches the placeholder
 *       title (deleting any segment_title join rows first — the completed first edition's final can
 *       still be pending adjudication, and leaving the title attached would award a fake
 *       championship the moment someone adjudicates it);
 *   <li>retires the placeholder title — deleted when it is inactive with no reigns, champions,
 *       challengers and no remaining references, otherwise left deactivated (never orphan FKs);
 *   <li>backfills a held briefcase for the winner of each COMPLETE "Time Vault%" tournament —
 *       idempotent via the service's exists-by-tournament guard.
 * </ol>
 */
@Component
@Order(20)
@RequiredArgsConstructor
@Slf4j
public class TimeVaultBriefcaseMigration implements DataMigration {

  /** Defensive name prefix — the Time Vault chain the placeholder shipped with. */
  static final String TOURNAMENT_NAME_PREFIX = "Time Vault";

  private final TournamentRepository tournamentRepository;
  private final TitleRepository titleRepository;
  private final TitleOpportunityRepository titleOpportunityRepository;
  private final SegmentRepository segmentRepository;
  private final TitleOpportunityService titleOpportunityService;

  @Override
  public String id() {
    return "time-vault-briefcase-repair";
  }

  @Override
  @Transactional
  public void migrate() {
    List<Tournament> vaults =
        tournamentRepository.findByNameStartingWithIgnoreCase(TOURNAMENT_NAME_PREFIX);
    if (vaults.isEmpty()) {
      log.info("No '{}' tournaments found — nothing to repair.", TOURNAMENT_NAME_PREFIX);
      return;
    }

    // 1 + 2: flag the chain, detach the placeholder, retire the title.
    Title placeholder = titleRepository.findByName(TOURNAMENT_NAME_PREFIX).orElse(null);
    for (Tournament vault : vaults) {
      if (!vault.isBriefcaseDeciding()) {
        vault.setBriefcaseDeciding(true);
      }
      if (vault.getLinkedTitle() != null) {
        segmentRepository
            .findByTitle(vault.getLinkedTitle())
            .forEach(
                segment -> {
                  segment.getTitles().remove(vault.getLinkedTitle());
                  log.info(
                      "Detached placeholder title '{}' from segment {} on the way to the briefcase"
                          + " repair",
                      vault.getLinkedTitle().getName(),
                      segment.getId());
                });
        vault.setLinkedTitle(null);
        tournamentRepository.save(vault);
      }
    }
    retirePlaceholder(placeholder);

    // 3: backfill a held briefcase for each COMPLETE edition's winner (idempotent).
    for (Tournament vault : vaults) {
      if (vault.getStatus() != TournamentStatus.COMPLETE) {
        continue;
      }
      Optional<Wrestler> winner =
          vault.getEntries().stream()
              .filter(e -> e.getStatus() == TournamentEntryStatus.WINNER)
              .map(e -> e.getWrestler())
              .findFirst();
      if (winner.isEmpty()) {
        log.warn(
            "Completed '{}' has no winner entry — briefcase backfill skipped", vault.getName());
        continue;
      }
      titleOpportunityService
          .grantFromTournament(vault, winner.get())
          .ifPresentOrElse(
              opportunity ->
                  log.info(
                      "Backfilled briefcase '{}' for {} from '{}'",
                      opportunity.getName(),
                      winner.get().getName(),
                      vault.getName()),
              () ->
                  log.info(
                      "Briefcase for '{}' already granted — backfill skipped (idempotent)",
                      vault.getName()));
    }
  }

  /**
   * Deletes the placeholder when it is provably unused (inactive, no reigns, no champions, no
   * challengers, no remaining tournament/segment references); otherwise leaves it deactivated. Any
   * TitleOpportunity referencing the title (a cash-in against it) also blocks deletion.
   *
   * <p>The detach step mutates in-memory entities; the reference queries below hit the database, so
   * the pending changes are flushed (and the placeholder re-read) first — otherwise a just detached
   * segment_title row still counts and the delete is skipped (seen on the sandbox run, 2026-10-08).
   */
  private void retirePlaceholder(Title placeholder) {
    if (placeholder == null) {
      return;
    }
    segmentRepository.flush();
    titleRepository.flush();
    Title current = titleRepository.findById(placeholder.getId()).orElse(null);
    if (current == null) {
      return; // already gone (e.g. orphanRemoval cascaded the delete)
    }
    List<String> blockers = new ArrayList<>();
    if (vaultsStillLinking(current)) {
      blockers.add("tournament link");
    }
    if (!segmentRepository.findByTitle(current).isEmpty()) {
      blockers.add("segment_title rows");
    }
    if (!titleOpportunityRepository.findByCashedAgainstTitleId(current.getId()).isEmpty()) {
      blockers.add("cashed-in opportunities");
    }
    if (Boolean.TRUE.equals(current.getIsActive())) {
      blockers.add("still active");
    }
    if (!current.getTitleReigns().isEmpty()) {
      blockers.add("reign history");
    }
    if (!current.getChampion().isEmpty()) {
      blockers.add("current champions");
    }
    if (!current.getChallengers().isEmpty()) {
      blockers.add("challengers");
    }
    if (!blockers.isEmpty()) {
      log.info(
          "Placeholder title '{}' not deleted — {} (left in place, inactive)",
          current.getName(),
          String.join(", ", blockers));
      return;
    }
    titleRepository.delete(current);
    log.info("Deleted unused placeholder title '{}'", current.getName());
  }

  private boolean vaultsStillLinking(Title placeholder) {
    return tournamentRepository.findByNameStartingWithIgnoreCase(TOURNAMENT_NAME_PREFIX).stream()
        .noneMatch(
            t ->
                t.getLinkedTitle() != null
                    && t.getLinkedTitle().getId().equals(placeholder.getId()));
  }
}
