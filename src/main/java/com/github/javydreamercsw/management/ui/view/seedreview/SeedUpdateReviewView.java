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
package com.github.javydreamercsw.management.ui.view.seedreview;

import com.github.javydreamercsw.base.ui.service.NotificationService;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.service.seedreview.SeedUpdateDecisionService;
import com.github.javydreamercsw.management.sync.review.SeedFieldDecision;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.RolesAllowed;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Administrator's diff-approval tree for parked seed-data collisions (seed-update review feature,
 * PR 3, Task 10). Renders every PENDING {@link SeedUpdateDecision} grouped domain → entity → field;
 * the administrator chooses keep/use at every level and applies the batch, which lowers the review
 * gate and unlocks the app.
 *
 * <p>Layout mirrors the approved mock v3: a top bar (heading, "N entities · M fields awaiting
 * decision" summary, approve-everything shortcut behind a confirmation dialog), one filled {@link
 * Details} per domain containing one per entity, each entity holding field rows (fieldName +
 * current value + new value + a keep/use radio) and removal rows ("removed from source data" +
 * keep-as-custom/remove radio), and a footer with the single Apply button.
 *
 * <p>Sweep buttons mirror the hierarchy: domain-level "Keep all mine"/"Approve all new" set every
 * descendant row; entity-level "Keep mine"/"Use new" set that entity's rows. Approve-everything
 * merges the service's map (field conflicts only — removals are excluded by contract) into the
 * current choices; nothing is written until Apply, which stays the single write point.
 *
 * <p>Dependent removals are NOT pre-checked here: {@code hasDependents} is applied at apply time
 * inside the service (no applier beans exist until the follow-up PR). A refused removal surfaces as
 * an error notification with the service's message and the choices stay editable.
 */
@Route(SeedReviewGateView.REVIEW_ROUTE)
@PageTitle("Review Seed Updates | All Time Wrestling RPG")
@Component
@RolesAllowed("ADMIN")
@Lazy
@UIScope
@Slf4j
public class SeedUpdateReviewView extends VerticalLayout {

  /** Radio choice labels shared by the item label generator and the sweep-button semantics. */
  private static final String KEEP_MINE_LABEL = "Keep mine";

  private static final String USE_NEW_LABEL = "Use new";
  private static final String KEEP_AS_CUSTOM_LABEL = "Keep as custom";
  private static final String REMOVE_LABEL = "Remove";

  private static final String APPLY_BUTTON_ID = "apply-button";
  private static final String APPROVE_EVERYTHING_BUTTON_ID = "approve-everything-button";
  private static final String TREE_ID = "seed-review-tree";

  private final transient SeedUpdateDecisionService decisionService;
  private final transient NotificationService notificationService;

  /** Chosen outcome per pending decision id — the state the Apply button submits. */
  private final Map<Long, SeedFieldDecision> choices = new LinkedHashMap<>();

  /** Row layouts by decision id, for the decided visual state (dimming). */
  private final Map<Long, VerticalLayout> rowLayoutsByDecisionId = new LinkedHashMap<>();

  /** Entity section-title spans by entity key, for the roll-up visual state (weight + color). */
  private final Map<String, Span> entityTitlesByKey = new LinkedHashMap<>();

  /** Domain section-title spans by domain key, for the roll-up visual state (weight + color). */
  private final Map<String, Span> domainTitlesByKey = new LinkedHashMap<>();

  /** Live "X of Y decided" counter in the top bar. */
  private Span decidedCounter;

  /** Radios by decision id, for the sweep buttons to set in one move. */
  private final Map<Long, RadioButtonGroup<SeedFieldDecision>> radiosByDecisionId =
      new LinkedHashMap<>();

  /** Rows in the order the service returned them, so grouping is stable (detection order). */
  private final List<SeedUpdateDecision> pendingRows = new ArrayList<>();

  private final Button applyButton = new Button("Apply decisions & unlock the app");

  @Autowired
  public SeedUpdateReviewView(
      final SeedUpdateDecisionService decisionService,
      final NotificationService notificationService) {
    this.decisionService = decisionService;
    this.notificationService = notificationService;

    setSizeFull();
    setPadding(true);
    setSpacing(true);
    // This layout IS the review tree's root container — the id anchors the docs E2E tests.
    setId(TREE_ID);

    buildTree();
  }

  private void buildTree() {
    pendingRows.clear();
    pendingRows.addAll(decisionService.pendingGrouped());
    choices.clear();
    radiosByDecisionId.clear();
    rowLayoutsByDecisionId.clear();
    entityTitlesByKey.clear();
    domainTitlesByKey.clear();
    decidedCounter = null;

    if (pendingRows.isEmpty()) {
      // The gate should have kept this route unreachable while decisions are pending; landing here
      // with nothing to review means everything was resolved between the click and the render.
      add(new H2("Nothing to review"), new Span("All seed updates have already been decided."));
      return;
    }

    long entityCount =
        pendingRows.stream().map(SeedUpdateDecision::getEntityKey).distinct().count();
    long fieldCount =
        pendingRows.stream()
            .filter(
                row ->
                    SeedUpdateDecision.DECISION_TYPE_FIELD_CONFLICT.equals(row.getDecisionType()))
            .count();

    H2 heading = new H2("Review seed updates");
    heading.setId("seed-review-heading");
    Span summary =
        new Span(entityCount + " entities · " + fieldCount + " fields awaiting decision");
    summary.setId("seed-review-summary");
    // Live progress: how many of the pending rows already carry a choice. Decided rows dim in
    // place (they stay visible so progress through the list is readable).
    decidedCounter = new Span("0 of " + pendingRows.size() + " decided");
    decidedCounter.setId("seed-review-decided-counter");

    Button approveEverything = new Button("Approve Everything — use all new values");
    approveEverything.setId(APPROVE_EVERYTHING_BUTTON_ID);
    approveEverything.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    approveEverything.addClickListener(e -> confirmApproveEverything());

    HorizontalLayout topBar =
        new HorizontalLayout(heading, summary, decidedCounter, approveEverything);
    topBar.setAlignItems(Alignment.CENTER);
    topBar.setWidthFull();
    add(topBar);

    // Domain grouping over the detection-ordered rows: LinkedHashMap keeps first-seen order, the
    // inner TreeMap keeps entities alphabetical within a domain.
    Map<String, Map<String, List<SeedUpdateDecision>>> byDomain = new LinkedHashMap<>();
    pendingRows.stream()
        .collect(
            Collectors.groupingBy(
                SeedUpdateDecision::getDomainKey, LinkedHashMap::new, Collectors.toList()))
        .forEach(
            (domainKey, rows) -> {
              Map<String, List<SeedUpdateDecision>> byEntity = new TreeMap<>();
              rows.forEach(
                  row ->
                      byEntity
                          .computeIfAbsent(row.getEntityKey(), key -> new ArrayList<>())
                          .add(row));
              byDomain.put(domainKey, byEntity);
            });

    byDomain.forEach(this::addDomainSection);

    applyButton.setId(APPLY_BUTTON_ID);
    applyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    applyButton.setEnabled(false); // Disabled until every pending row has a choice.
    applyButton.addClickListener(e -> apply());
    add(applyButton);

    // Initial roll-up: at first load the whole tree is pending, so every title renders bold —
    // the tree announces the remaining work before the first click.
    refreshRollupState();
  }

  /** Builds one domain Details containing one entity Details per entity with pending rows. */
  private void addDomainSection(
      String domainKey, Map<String, List<SeedUpdateDecision>> rowsByEntity) {
    List<SeedUpdateDecision> domainRows =
        rowsByEntity.values().stream().flatMap(List::stream).toList();

    Button keepAllMine =
        new Button("Keep all mine", e -> sweep(domainRows, SeedFieldDecision.KEEP_MINE));
    keepAllMine.setId("domain-" + sanitize(domainKey) + "-keep-all-mine");

    Button approveAllNew =
        new Button("Approve all new", e -> sweep(domainRows, SeedFieldDecision.USE_NEW));
    approveAllNew.setId("domain-" + sanitize(domainKey) + "-approve-all-new");

    Span domainTitle = new Span(domainKey + " (" + rowsByEntity.size() + " entities)");
    domainTitle.setId("domain-section-" + sanitize(domainKey) + "-title");
    domainTitlesByKey.put(domainKey, domainTitle);

    Details domainSection =
        section(
            domainTitle,
            "domain-section-" + sanitize(domainKey),
            true,
            new HorizontalLayout(keepAllMine, approveAllNew));

    VerticalLayout entitiesLayout = new VerticalLayout();
    entitiesLayout.setPadding(false);
    entitiesLayout.setSpacing(false);
    rowsByEntity.forEach((entityKey, rows) -> entitiesLayout.add(buildEntitySection(rows)));
    domainSection.add(entitiesLayout);
    add(domainSection);
  }

  /** Builds one entity Details holding its field rows and/or its removal row. */
  private Details buildEntitySection(List<SeedUpdateDecision> rows) {
    SeedUpdateDecision first = rows.getFirst();
    String entityKey = first.getEntityKey();
    String sanitizedEntity = sanitize(entityKey);

    Button keepMine = new Button("Keep mine", e -> sweep(rows, SeedFieldDecision.KEEP_MINE));
    keepMine.setId("entity-" + sanitizedEntity + "-keep-mine");

    Button useNew = new Button("Use new", e -> sweep(rows, SeedFieldDecision.USE_NEW));
    useNew.setId("entity-" + sanitizedEntity + "-use-new");

    Span entityTitle = new Span(first.getEntityDisplayName());
    entityTitle.setId("entity-section-" + sanitizedEntity + "-title");
    entityTitlesByKey.put(entityKey, entityTitle);

    Details entitySection =
        section(
            entityTitle,
            "entity-section-" + sanitizedEntity,
            false,
            new HorizontalLayout(keepMine, useNew));

    VerticalLayout fieldRows = new VerticalLayout();
    fieldRows.setPadding(false);
    fieldRows.setSpacing(false);
    rows.forEach(row -> fieldRows.add(buildFieldRow(row)));
    entitySection.add(fieldRows);
    return entitySection;
  }

  /**
   * Builds one decision row: the field's current vs. new values and its keep/use radio — or, for a
   * removal, the "removed from source data" notice and the keep-as-custom/remove radio. No
   * dependent pre-check: the service refuses dependent removals at apply time (no applier beans
   * exist yet), and the helper text documents that contract.
   */
  private VerticalLayout buildFieldRow(SeedUpdateDecision row) {
    VerticalLayout rowLayout = new VerticalLayout();
    rowLayout.setPadding(false);
    rowLayout.setSpacing(false);
    rowLayoutsByDecisionId.put(row.getId(), rowLayout);

    boolean removal = SeedUpdateDecision.DECISION_TYPE_REMOVAL.equals(row.getDecisionType());

    List<SeedFieldDecision> options =
        removal
            ? List.of(SeedFieldDecision.KEEP_AS_CUSTOM, SeedFieldDecision.REMOVE)
            : List.of(SeedFieldDecision.KEEP_MINE, SeedFieldDecision.USE_NEW);

    RadioButtonGroup<SeedFieldDecision> radio = new RadioButtonGroup<>();
    radio.setItems(options);
    radio.setItemLabelGenerator(this::labelFor);
    radio.addValueChangeListener(
        e -> {
          // A null value is a deselect: the choice must LEAVE the map, not sit in it as a null
          // value (Map.copyOf in apply() rejects null values, and an entry here would let the
          // Apply button enable while the batch is still incomplete).
          if (e.getValue() == null) {
            choices.remove(row.getId());
          } else {
            choices.put(row.getId(), e.getValue());
          }
          markRowDecided(row.getId(), e.getValue());
          refreshRollupState();
          refreshApplyState();
        });
    String radioId = radioIdFor(row);
    radio.setId(radioId);
    radiosByDecisionId.put(row.getId(), radio);

    if (removal) {
      Span removalNotice =
          new Span("Removed from source data — this entity will be deleted if you choose Remove.");
      removalNotice.setId("removal-notice-" + sanitize(row.getEntityKey()));
      rowLayout.add(removalNotice);
      radio.setHelperText(
          "Removals whose data is still referenced elsewhere are refused at apply time — resolve"
              + " or remove the dependents first, then re-apply.");
    } else {
      Span diff =
          new Span(
              (row.getFieldName() == null ? "(field)" : row.getFieldName())
                  + ": current = "
                  + displayValue(row.getCurrentValue())
                  + " → new = "
                  + displayValue(row.getNewValue()));
      diff.setId("field-diff-" + radioId.substring("decision-".length()));
      rowLayout.add(diff);
    }
    rowLayout.add(radio);
    return rowLayout;
  }

  /**
   * Approve-everything shortcut: the service maps every pending field conflict to {@code USE_NEW}
   * (removals excluded by contract) and the map is merged into the current choices — the admin
   * still presses Apply to write anything.
   */
  private void confirmApproveEverything() {
    ConfirmDialog dialog = new ConfirmDialog();
    dialog.setHeader("Approve everything?");
    dialog.setText(
        "Every pending field conflict will be set to use the new value. Removals are NOT included"
            + " — they still need an explicit per-entity choice. Nothing is written until you"
            + " press Apply.");
    dialog.setCancelable(true);
    dialog.setCancelText("Go Back");
    dialog.setConfirmText("Use All New Values");
    dialog.addConfirmListener(
        e -> {
          Map<Long, SeedFieldDecision> approved = decisionService.approveEverythingExceptRemovals();
          approved.forEach(
              (id, decision) -> {
                choices.put(id, decision);
                RadioButtonGroup<SeedFieldDecision> radio = radiosByDecisionId.get(id);
                if (radio != null) {
                  radio.setValue(decision);
                }
              });
          refreshRollupState();
          refreshApplyState();
        });
    dialog.open();
  }

  /**
   * Sets every descendant FIELD row of a domain/entity sweep button to {@code decision} — radios
   * and the choice map together, so Apply always submits what the screen shows.
   *
   * <p>Removal rows are deliberately skipped: a bulk "keep mine"/"use new" has no valid mapping to
   * a removal's keep-as-custom/remove options (and silently choosing Remove there would be the one
   * thing this feature must never do in bulk). Removals need an explicit per-entity choice — same
   * contract as {@code approveEverythingExceptRemovals()}.
   */
  private void sweep(List<SeedUpdateDecision> rows, SeedFieldDecision decision) {
    rows.stream()
        .filter(
            row -> SeedUpdateDecision.DECISION_TYPE_FIELD_CONFLICT.equals(row.getDecisionType()))
        .forEach(
            row -> {
              RadioButtonGroup<SeedFieldDecision> radio = radiosByDecisionId.get(row.getId());
              if (radio != null) {
                radio.setValue(decision);
              }
              choices.put(row.getId(), decision);
            });
    refreshRollupState();
    refreshApplyState();
  }

  /**
   * Applies the whole batch. All-or-nothing in the service: on success the pending queue empties
   * (re-read to confirm) and the app unlocks; on failure the error message surfaces and the choices
   * stay editable. Applying is terminal — decisions that landed never come back.
   */
  private void apply() {
    try {
      decisionService.apply(Map.copyOf(choices));
    } catch (RuntimeException e) {
      log.warn("Seed decision apply failed", e);
      notificationService.showError("Apply failed: " + String.valueOf(e.getMessage()));
      return;
    }
    if (decisionService.pendingCount() == 0) {
      notificationService.showSuccess("Seed decisions applied — the app is unlocked.");
      // UI.getCurrent() (not getUI()): the gate is down after a successful apply, and this matches
      // the codebase's navigation-from-click-handler idiom (see ShowPlanningView).
      UI.getCurrent().navigate("");
    } else {
      // Unreachable with the current all-or-nothing service contract (every pending row was in
      // the submitted map); guarded so a future contract change cannot strand the admin silently.
      notificationService.showWarning(
          "Apply returned but decisions are still pending — review the tree and try again.");
    }
  }

  /** Enables the Apply button only when every pending row has a choice. */
  private void refreshApplyState() {
    Set<Long> pendingIds =
        pendingRows.stream()
            .map(SeedUpdateDecision::getId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    applyButton.setEnabled(choices.keySet().containsAll(pendingIds));
    if (decidedCounter != null) {
      decidedCounter.setText(choices.size() + " of " + pendingRows.size() + " decided");
    }
  }

  /**
   * Visual state for one field row, keyed by its decision: undecided rows stay plain; decided rows
   * dim to 70% and carry a decision-colored text tint plus a colored left stripe — green for
   * use-new, amber for keep-mine/keep-as-custom, red for remove (Lumo semantic tokens, readable in
   * both themes). Reversible: deselecting the radio restores the undecided visuals.
   */
  private void markRowDecided(Long decisionId, SeedFieldDecision decision) {
    VerticalLayout rowLayout = rowLayoutsByDecisionId.get(decisionId);
    if (rowLayout == null) {
      return;
    }
    if (decision == null) {
      rowLayout.getStyle().set("opacity", "1");
      rowLayout.getStyle().remove("color");
      rowLayout.getStyle().remove("border-left");
    } else {
      rowLayout.getStyle().set("opacity", "0.7");
      rowLayout.getStyle().set("color", colorFor(decision));
      rowLayout.getStyle().set("border-left", "2px solid " + colorFor(decision));
    }
  }

  /**
   * Recomputes the roll-up visual state of every entity and domain title from the current choices:
   * bold while nothing beneath is decided, italic while partially decided, and normal weight with a
   * decision color once fully decided — green (all use-new), amber (all keep-family), red (all
   * remove); mixed outcomes stay neutral. Weight announces the remaining work; color announces the
   * outcome.
   */
  private void refreshRollupState() {
    entityTitlesByKey.forEach(
        (entityKey, title) -> paintRollup(title, rowsOfEntity(entityKey), entityKey));
    domainTitlesByKey.forEach(
        (domainKey, title) ->
            paintRollup(
                title,
                pendingRows.stream().filter(row -> domainKey.equals(row.getDomainKey())).toList(),
                null));
  }

  /** Rows of one entity, looked up through the title maps' entity key. */
  private List<SeedUpdateDecision> rowsOfEntity(String entityKey) {
    return pendingRows.stream().filter(row -> entityKey.equals(row.getEntityKey())).toList();
  }

  /**
   * Paints one title span for the state of its rows: bold / no color (nothing decided), italic / no
   * color (partial), normal + uniform-outcome color, normal + no color (mixed). {@code entityKey}
   * limits the uniformity check to keep-family rows of one entity; domain-level checks pass {@code
   * null} to consider every row.
   */
  private void paintRollup(Span title, List<SeedUpdateDecision> rows, String entityKey) {
    List<SeedFieldDecision> decided =
        rows.stream().map(row -> choices.get(row.getId())).filter(Objects::nonNull).toList();

    if (decided.isEmpty()) {
      title.getElement().getStyle().remove("font-style");
      title.getElement().getStyle().set("font-weight", "bold");
      title.getElement().getStyle().remove("color");
      return;
    }
    if (decided.size() < rows.size()) {
      title.getElement().getStyle().remove("font-weight");
      title.getElement().getStyle().set("font-style", "italic");
      title.getElement().getStyle().remove("color");
      return;
    }
    title.getElement().getStyle().remove("font-weight");
    title.getElement().getStyle().remove("font-style");
    boolean uniform = decided.stream().allMatch(d -> d == decided.getFirst());
    title.getElement().getStyle().set("color", uniform ? colorFor(decided.getFirst()) : null);
  }

  /** Decision → Lumo semantic color token: green for use-new, amber for keeps, red for remove. */
  private String colorFor(SeedFieldDecision decision) {
    return switch (decision) {
      case USE_NEW -> "var(--lumo-success-color)";
      case KEEP_MINE, KEEP_AS_CUSTOM -> "var(--lumo-warning-color)";
      case REMOVE -> "var(--lumo-error-color)";
    };
  }

  /**
   * Builds the sanitized radio id: {@code decision-<domain>-<entity>-<field>} (or {@code
   * -removal}).
   */
  private String radioIdFor(SeedUpdateDecision row) {
    String base =
        "decision-" + sanitize(row.getDomainKey()) + "-" + sanitize(row.getEntityKey()) + "-";
    if (SeedUpdateDecision.DECISION_TYPE_REMOVAL.equals(row.getDecisionType())) {
      return base + "removal";
    }
    return base + sanitize(row.getFieldName() == null ? "field" : row.getFieldName());
  }

  /** Lowercase, every non-alphanumeric character collapsed to {@code -} — stable element ids. */
  private String sanitize(String value) {
    return value.toLowerCase().replaceAll("[^a-z0-9]+", "-");
  }

  private String labelFor(SeedFieldDecision decision) {
    return switch (decision) {
      case KEEP_MINE -> KEEP_MINE_LABEL;
      case USE_NEW -> USE_NEW_LABEL;
      case KEEP_AS_CUSTOM -> KEEP_AS_CUSTOM_LABEL;
      case REMOVE -> REMOVE_LABEL;
    };
  }

  /** Null-safe value display: removal/absent values render as a dash. */
  private String displayValue(String value) {
    return value == null || value.isBlank() ? "—" : value;
  }

  /**
   * Builds a filled collapsible section with the given title component, id, and content. The title
   * is a Component (not text) so the roll-up visual state can style it in place.
   */
  private Details section(
      final com.vaadin.flow.component.Component title,
      final String id,
      final boolean opened,
      final com.vaadin.flow.component.Component... content) {
    VerticalLayout layout = new VerticalLayout(content);
    layout.setPadding(false);
    layout.setSpacing(false);
    Details details = new Details(title, layout);
    details.setId(id);
    details.setOpened(opened);
    details.addThemeVariants(DetailsVariant.FILLED);
    details.setWidthFull();
    return details;
  }

  /** Exposed for tests: the current choice per pending decision id (what Apply would submit). */
  Map<Long, SeedFieldDecision> getChoices() {
    return Map.copyOf(choices);
  }
}
