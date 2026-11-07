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
package com.github.javydreamercsw.management.sync;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository;
import com.github.javydreamercsw.management.domain.show.template.RecurrenceType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateSegmentAssignment;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRepository;
import com.github.javydreamercsw.management.dto.ShowTemplateDTO;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.sync.review.SeedCollisionDetector;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedReviewDetectionSupport;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(60)
public class ShowTemplateSync implements DataSyncContributor {

  /** Seed domain key (must match {@code SeedDomains.STAMPING_DOMAINS}). */
  public static final String DOMAIN_KEY = "show_template";

  private final SeedSyncSupport syncSupport;
  private final ShowTemplateService showTemplateService;
  private final ShowTemplateRepository showTemplateRepository;
  private final ShowTypeRepository showTypeRepository;
  private final CommentaryTeamRepository commentaryTeamRepository;
  private final SegmentTypeService segmentTypeService;
  private final SegmentRuleService segmentRuleService;
  private final TournamentRepository tournamentRepository;
  private final ObjectMapper objectMapper;
  private final SeedUpdateReviewService seedReviewService;
  private final SeedProvenanceManifest provenanceManifest;

  @Autowired
  public ShowTemplateSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final ShowTemplateService showTemplateService,
      final ShowTemplateRepository showTemplateRepository,
      final ShowTypeRepository showTypeRepository,
      final CommentaryTeamRepository commentaryTeamRepository,
      final SegmentTypeService segmentTypeService,
      final SegmentRuleService segmentRuleService,
      final TournamentRepository tournamentRepository,
      final ObjectMapper objectMapper,
      final SeedUpdateReviewService seedReviewService,
      final SeedProvenanceManifest provenanceManifest) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.showTemplateService = showTemplateService;
    this.showTemplateRepository = showTemplateRepository;
    this.showTypeRepository = showTypeRepository;
    this.commentaryTeamRepository = commentaryTeamRepository;
    this.segmentTypeService = segmentTypeService;
    this.segmentRuleService = segmentRuleService;
    this.tournamentRepository = tournamentRepository;
    this.objectMapper = objectMapper;
    this.seedReviewService = seedReviewService;
    this.provenanceManifest = provenanceManifest;
  }

  @Override
  public void sync() {
    long existingCount = showTemplateService.count();
    if (existingCount > 0) {
      log.debug(
          "Show templates table already contains {} templates - skipping file import",
          existingCount);
      return;
    }

    try {
      List<ShowTemplateDTO> dtos = loadCatalog();
      for (ShowTemplateDTO dto : dtos) {
        ShowTemplate template =
            showTemplateService.createOrUpdateTemplate(
                dto.getName(),
                dto.getDescription(),
                dto.getShowTypeName(),
                null,
                dto.getCommentaryTeamName(),
                dto.getExpectedMatches(),
                dto.getExpectedPromos(),
                dto.getDurationDays(),
                dto.getRecurrenceType() != null
                    ? RecurrenceType.valueOf(dto.getRecurrenceType())
                    : null,
                dto.getDayOfWeek() != null ? DayOfWeek.valueOf(dto.getDayOfWeek()) : null,
                dto.getDayOfMonth(),
                dto.getWeekOfMonth(),
                dto.getMonth() != null ? Month.valueOf(dto.getMonth()) : null,
                dto.getGenderConstraint() != null
                    ? Gender.valueOf(dto.getGenderConstraint())
                    : null,
                dto.getRequiredExpansions());
        if (template != null) {
          seedAssignments(template, dto);
          // Stamp the freshly seeded row so verify mode's stored-side check and the
          // version-change detection pass can match it against the manifest. The service's save
          // leaves the stamp unset; the instance createOrUpdateTemplate returned already carries
          // every seed-owned field, so stamping it directly and re-saving reuses the identity
          // the first write committed (assignments live on their own rows and were persisted by
          // seedAssignments' own save).
          template.setSeedFingerprint(templateFingerprint(template));
          // Direct repository save (not showTemplateService.save): the stamp write has no
          // security context concerns beyond the sync's own run-as-admin and must not trigger the
          // service's snapshot propagation (syncFutureShowsWithTemplate) — nothing user-visible
          // exists yet at seed time, and the propagation's nested reads force the detached
          // template instance back through merge on a boot where the persistence context for the
          // outer call has already closed.
          showTemplateRepository.save(template);
          log.debug(
              "Loaded show template: {} (Type: {})", template.getName(), dto.getShowTypeName());
        } else {
          log.warn(
              "Failed to load show template: {} - show type not found: {}",
              dto.getName(),
              dto.getShowTypeName());
        }
      }
    } catch (IOException e) {
      log.error("Error loading show templates from file", e);
    }
  }

  /** Parse the seed catalog. Protected so tests can substitute their own definitions. */
  protected List<ShowTemplateDTO> loadCatalog() throws IOException {
    ClassPathResource resource = new ClassPathResource("show_templates.json");
    if (resource.exists()) {
      log.debug("Loading show templates from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        return objectMapper.readValue(is, new TypeReference<>() {});
      }
    }
    log.warn("Show templates file not found: {}", resource.getPath());
    return List.of();
  }

  /**
   * Version-change detection pass (seed-update review feature): on a boot where the app version
   * changed (or undecided decisions exist), existing show templates are classified field-by-field
   * through {@link SeedCollisionDetector} instead of being reconciled directly — tier-1 fields
   * update silently and re-stamp, tier-2 fields park as PENDING decisions, tier-3 fields stay
   * untouched. Runs only in PROTECT mode with existing rows — this domain's normal path skips the
   * whole import on a non-empty table (no verify-mode reconcile exists here), so the pass gives
   * upgrade boots their only chance to classify existing templates. FULL mode's load already
   * applies the seed everywhere; an empty DB has nothing to collide.
   *
   * <p>Callers invoke this once per boot after the domain passes and only when {@link
   * SeedUpdateReviewService#shouldRunDetection()} is true; {@code markDetected()} is NOT called
   * here (the orchestrator stamps the version after all domains finish).
   */
  void detectVersionChangeCollisions() {
    if (syncSupport.mode() == SeedSyncMode.FULL || showTemplateService.count() == 0) {
      return; // FULL mode applies the seed everywhere; an empty DB has nothing to collide
    }
    long start = System.currentTimeMillis();
    List<ShowTemplateDTO> dtos;
    try {
      dtos = loadCatalog();
    } catch (IOException e) {
      log.error("Error loading show templates for seed-review detection", e);
      return;
    }
    if (dtos.isEmpty()) {
      return;
    }
    List<ShowTemplate> toSave = new ArrayList<>();
    for (ShowTemplateDTO dto : dtos) {
      ShowTemplate template = showTemplateRepository.findByName(dto.getName()).orElse(null);
      if (template == null) {
        continue; // booker-created rows have no seed match; missing rows are the FULL load's job
      }
      String newFingerprint =
          SeedReviewDetectionSupport.incomingFingerprint(
              provenanceManifest, DOMAIN_KEY, dto.getName(), seedValueFingerprint(dto));
      boolean rowHasStoredStamp =
          SeedReviewDetectionSupport.hasStamp(template.getSeedFingerprint());
      List<SeedCollisionDetector.FieldObservation> observations = fieldObservations(template, dto);
      SeedReviewDetectionSupport.DetectionResult result =
          SeedReviewDetectionSupport.detectRow(
              DOMAIN_KEY,
              entityKey(dto.getName()),
              dto.getName(),
              rowHasStoredStamp,
              template.getSeedFingerprint(),
              observations,
              this::templateValueListFingerprint,
              newFingerprint,
              seedReviewService);
      String stamp =
          SeedReviewDetectionSupport.stampForResolvedRow(result.provenanceExact(), newFingerprint);
      if (!result.silentUpdateFields().isEmpty()) {
        // Tier-1 write set: ONLY the silent-update fields — a tier-3-preserved admin edit on
        // the same row must never be touched (per-field granularity, spec "Tier 2 granularity").
        applySilentSeedFields(template, dto, result.silentUpdateFields());
        toSave.add(template);
      }
      // Stamp-only rows must persist the re-stamp too: capture the change BEFORE setting it —
      // comparing after the set reads the NEW value and the save is silently skipped forever
      // (the row never converges).
      if (stamp != null && !stamp.equals(template.getSeedFingerprint())) {
        template.setSeedFingerprint(stamp);
        if (!toSave.contains(template)) {
          toSave.add(template);
        }
      }
    }
    if (!toSave.isEmpty()) {
      for (ShowTemplate template : toSave) {
        // Direct repository save (same choice as the FULL path's stamp write): the detection pass
        // only needs the tier-1 field updates and the stamp persisted. The service's save() also
        // runs snapshot propagation (syncFutureShowsWithTemplate) and re-merges the detached
        // instance inside its own REQUIRES_NEW boundary — mechanics that abort the whole pass
        // with a 0-row optimistic-lock update on a real upgrade boot (observed on the FULL path
        // in test-context boots). No user-visible show exists to propagate at seed time.
        showTemplateRepository.save(template);
      }
    }
    log.info(
        "Seed review detection (show template) complete ({} ms).",
        System.currentTimeMillis() - start);
  }

  /** The detection-time per-field observations for one template row against its seed DTO. */
  private List<SeedCollisionDetector.FieldObservation> fieldObservations(
      ShowTemplate template, ShowTemplateDTO dto) {
    List<SeedCollisionDetector.FieldObservation> obs = new ArrayList<>();
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "description", null, template.getDescription(), dto.getDescription()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "showTypeName",
            null,
            template.getShowType() == null ? null : template.getShowType().getName(),
            dto.getShowTypeName()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "commentaryTeamName",
            null,
            template.getCommentaryTeam() == null ? null : template.getCommentaryTeam().getName(),
            dto.getCommentaryTeamName()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "expectedMatches",
            null,
            template.getExpectedMatches() == null ? null : template.getExpectedMatches().toString(),
            dto.getExpectedMatches() == null ? null : dto.getExpectedMatches().toString()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "expectedPromos",
            null,
            template.getExpectedPromos() == null ? null : template.getExpectedPromos().toString(),
            dto.getExpectedPromos() == null ? null : dto.getExpectedPromos().toString()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "durationDays",
            null,
            template.getDurationDays() == null ? null : template.getDurationDays().toString(),
            dto.getDurationDays() == null ? null : dto.getDurationDays().toString()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "recurrenceType",
            null,
            template.getRecurrenceType() == null ? null : template.getRecurrenceType().name(),
            dto.getRecurrenceType()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "dayOfWeek",
            null,
            template.getDayOfWeek() == null ? null : template.getDayOfWeek().name(),
            dto.getDayOfWeek()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "dayOfMonth",
            null,
            template.getDayOfMonth() == null ? null : template.getDayOfMonth().toString(),
            dto.getDayOfMonth() == null ? null : dto.getDayOfMonth().toString()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "weekOfMonth",
            null,
            template.getWeekOfMonth() == null ? null : template.getWeekOfMonth().toString(),
            dto.getWeekOfMonth() == null ? null : dto.getWeekOfMonth().toString()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "month",
            null,
            template.getMonth() == null ? null : template.getMonth().name(),
            dto.getMonth()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "genderConstraint",
            null,
            template.getGenderConstraint() == null ? null : template.getGenderConstraint().name(),
            dto.getGenderConstraint()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "requiredExpansions",
            null,
            String.join("\n", template.getRequiredExpansions().stream().sorted().toList()),
            String.join("\n", dto.getRequiredExpansions().stream().sorted().toList())));
    return obs;
  }

  /** Fingerprint the incoming seed values would produce, in the canonical field order. */
  private String seedValueFingerprint(ShowTemplateDTO dto) {
    return syncSupport.fingerprint(
        dto.getDescription(),
        dto.getShowTypeName(),
        dto.getCommentaryTeamName(),
        dto.getExpectedMatches(),
        dto.getExpectedPromos(),
        dto.getDurationDays(),
        dto.getRecurrenceType(),
        dto.getDayOfWeek(),
        dto.getDayOfMonth(),
        dto.getWeekOfMonth(),
        dto.getMonth(),
        dto.getGenderConstraint(),
        String.join("\n", dto.getRequiredExpansions().stream().sorted().toList()));
  }

  /**
   * Re-hashes a field-value list (the {@link #fieldObservations} order) back into a row fingerprint
   * — the hash-function half of the clean-row/single-edit provenance reconstruction. Serialization
   * mirrors {@link #templateFingerprint(ShowTemplate)} exactly.
   */
  private String templateValueListFingerprint(List<String> values) {
    // Raw pass-through: fingerprint serializes each value via toString, so the observed string
    // forms hash identically to the row fingerprint's typed values (see WrestlerSync's shape).
    return syncSupport.fingerprint(values.toArray());
  }

  private static String entityKey(String name) {
    return entityKeyFor(name);
  }

  /**
   * The canonical entity key for a show template name — shared with {@code ShowTemplateSeedApplier}
   * so apply-side resolution derives keys exactly as detection minted them.
   */
  static String entityKeyFor(String name) {
    return "show_template:" + name.toLowerCase().replace(' ', '-');
  }

  /**
   * Canonical seed-owned field values for a show template row, in fingerprint order: description,
   * showTypeName, commentaryTeamName, expectedMatches, expectedPromos, durationDays,
   * recurrenceType, dayOfWeek, dayOfMonth, weekOfMonth, month, genderConstraint, requiredExpansions
   * (sorted, newline-joined). The name is the lookup key and excluded; assignment rows are their
   * own domain-level structure and excluded (the assignment seed write only runs when the table is
   * empty — template content is the row's reviewable surface). Public for the seed-provenance
   * manifest generator, which re-hashes fresh seed rows through the owning sync's own method.
   */
  public String templateFingerprint(ShowTemplate template) {
    return syncSupport.fingerprint(
        template.getDescription(),
        template.getShowType() == null ? null : template.getShowType().getName(),
        template.getCommentaryTeam() == null ? null : template.getCommentaryTeam().getName(),
        template.getExpectedMatches(),
        template.getExpectedPromos(),
        template.getDurationDays(),
        template.getRecurrenceType() == null ? null : template.getRecurrenceType().name(),
        template.getDayOfWeek() == null ? null : template.getDayOfWeek().name(),
        template.getDayOfMonth(),
        template.getWeekOfMonth(),
        template.getMonth() == null ? null : template.getMonth().name(),
        template.getGenderConstraint() == null ? null : template.getGenderConstraint().name(),
        String.join("\n", template.getRequiredExpansions().stream().sorted().toList()));
  }

  /**
   * Writes ONLY the named silent-update fields from the seed DTO onto the row — the tier-1 write
   * set of the version-change pass. Field names match {@link #fieldObservations} entries; the write
   * semantics mirror {@code createOrUpdateTemplate}'s write set. The show type reference is
   * resolved by name through the same lookup the seed write uses (unknown names log and skip).
   */
  private void applySilentSeedFields(
      ShowTemplate template, ShowTemplateDTO dto, List<String> silentFields) {
    for (String field : silentFields) {
      switch (field) {
        case "description" -> template.setDescription(dto.getDescription());
        case "showTypeName" ->
            showTypeRepository
                .findByName(dto.getShowTypeName())
                .ifPresentOrElse(
                    template::setShowType,
                    () ->
                        log.warn(
                            "Show type '{}' not found — show type reference not changed on"
                                + " template '{}'",
                            dto.getShowTypeName(),
                            dto.getName()));
        case "commentaryTeamName" -> {
          String name = dto.getCommentaryTeamName();
          if (name == null || name.isBlank()) {
            template.setCommentaryTeam(null);
          } else {
            commentaryTeamRepository.findAll().stream()
                .filter(t -> t.getName().equals(name))
                .findFirst()
                .ifPresentOrElse(
                    template::setCommentaryTeam,
                    () ->
                        log.warn(
                            "Commentary team '{}' not found — team reference not changed on"
                                + " template '{}'",
                            name,
                            dto.getName()));
          }
        }
        case "expectedMatches" -> template.setExpectedMatches(dto.getExpectedMatches());
        case "expectedPromos" -> template.setExpectedPromos(dto.getExpectedPromos());
        case "durationDays" ->
            template.setDurationDays(dto.getDurationDays() != null ? dto.getDurationDays() : 1);
        case "recurrenceType" ->
            template.setRecurrenceType(
                dto.getRecurrenceType() != null
                    ? RecurrenceType.valueOf(dto.getRecurrenceType())
                    : RecurrenceType.NONE);
        case "dayOfWeek" ->
            template.setDayOfWeek(
                dto.getDayOfWeek() != null ? DayOfWeek.valueOf(dto.getDayOfWeek()) : null);
        case "dayOfMonth" -> template.setDayOfMonth(dto.getDayOfMonth());
        case "weekOfMonth" -> template.setWeekOfMonth(dto.getWeekOfMonth());
        case "month" ->
            template.setMonth(dto.getMonth() != null ? Month.valueOf(dto.getMonth()) : null);
        case "genderConstraint" ->
            template.setGenderConstraint(
                dto.getGenderConstraint() != null
                    ? Gender.valueOf(dto.getGenderConstraint())
                    : null);
        case "requiredExpansions" -> {
          template.getRequiredExpansions().clear();
          template.getRequiredExpansions().addAll(dto.getRequiredExpansions());
        }
        default ->
            throw new IllegalStateException(
                "Unknown silent-update show template field '" + field + "'");
      }
    }
  }

  /**
   * Seed the template's assignment rows from the DTO (ATW-cpuu). Row targets resolve by NAME —
   * SegmentRuleSync (@Order 30) and SegmentTypeSync (@Order 40) ran earlier, so names resolve;
   * unknown names are skipped with a warning rather than failing the whole template. Deliberately
   * only touches templates with no rows yet: on re-seed the booker's own edits win.
   */
  private void seedAssignments(ShowTemplate template, ShowTemplateDTO dto) {
    if (dto.getAssignments() == null || dto.getAssignments().isEmpty()) {
      return;
    }
    List<ShowTemplateSegmentAssignment> rows = new ArrayList<>();
    for (ShowTemplateDTO.AssignmentDTO assignmentDto : dto.getAssignments()) {
      ShowTemplateSegmentAssignment row = new ShowTemplateSegmentAssignment();
      if (assignmentDto.getSegmentTypeName() != null) {
        segmentTypeService
            .findByName(assignmentDto.getSegmentTypeName())
            .ifPresentOrElse(
                row::setSegmentType,
                () ->
                    log.warn(
                        "Segment type '{}' not found — skipping assignment row on template '{}'",
                        assignmentDto.getSegmentTypeName(),
                        dto.getName()));
      }
      if (assignmentDto.getSegmentRuleName() != null) {
        segmentRuleService
            .findByName(assignmentDto.getSegmentRuleName())
            .ifPresentOrElse(
                row::setSegmentRule,
                () ->
                    log.warn(
                        "Segment rule '{}' not found — skipping assignment row on template '{}'",
                        assignmentDto.getSegmentRuleName(),
                        dto.getName()));
      }
      if (assignmentDto.getTournamentCode() != null) {
        // TournamentSync (@Order 55) ran before this sync, so catalog codes resolve.
        tournamentRepository
            .findByCode(assignmentDto.getTournamentCode())
            .ifPresentOrElse(
                row::setTournament,
                () ->
                    log.warn(
                        "Tournament code '{}' not found — skipping assignment row on template"
                            + " '{}'",
                        assignmentDto.getTournamentCode(),
                        dto.getName()));
      }
      if (assignmentDto.getSpecName() != null) {
        // Full spec row (ATW-etws): the booking path creates the tournament on first use. The
        // final rule and allowed pool resolve by name like type/rule targets.
        if (assignmentDto.getTournamentCode() != null) {
          log.warn(
              "Assignment row on template '{}' sets both tournamentCode and a spec — keeping the"
                  + " tournament reference, ignoring the spec",
              dto.getName());
        } else {
          row.setSpecName(assignmentDto.getSpecName());
          row.setSpecFormatId(assignmentDto.getSpecFormatId());
          row.setSpecEntrantCount(assignmentDto.getSpecEntrantCount());
          if (assignmentDto.getSpecFinalRuleName() != null) {
            segmentRuleService
                .findByName(assignmentDto.getSpecFinalRuleName())
                .ifPresentOrElse(
                    row::setSpecFinalRule,
                    () ->
                        log.warn(
                            "Final rule '{}' not found — spec final rule skipped on template"
                                + " '{}'",
                            assignmentDto.getSpecFinalRuleName(),
                            dto.getName()));
          }
          for (String ruleName : assignmentDto.getAllowedRuleNames()) {
            segmentRuleService
                .findByName(ruleName)
                .ifPresentOrElse(
                    row.getSpecAllowedRules()::add,
                    () ->
                        log.warn(
                            "Allowed rule '{}' not found — skipped from spec pool on template"
                                + " '{}'",
                            ruleName,
                            dto.getName()));
          }
        }
      }
      row.setMode(
          assignmentDto.getMode() != null
              ? ShowTemplateSegmentAssignment.AssignmentMode.valueOf(assignmentDto.getMode())
              : ShowTemplateSegmentAssignment.AssignmentMode.ENCOURAGED);
      if (!row.isValid()) {
        log.warn(
            "Assignment row on template '{}' targets nothing recognizable (type: '{}', rule:"
                + " '{}') — skipping",
            dto.getName(),
            assignmentDto.getSegmentTypeName(),
            assignmentDto.getSegmentRuleName());
        continue;
      }
      rows.add(row);
    }
    if (!rows.isEmpty()) {
      showTemplateService.syncSegmentAssignments(template.getId(), rows);
      log.debug("Seeded {} assignment row(s) on template '{}'", rows.size(), template.getName());
    }
  }
}
