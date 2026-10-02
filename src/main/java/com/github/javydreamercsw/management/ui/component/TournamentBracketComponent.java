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
package com.github.javydreamercsw.management.ui.component;

import com.github.javydreamercsw.management.dto.campaign.TournamentDTO;
import com.github.javydreamercsw.management.service.tournament.TournamentFormat;
import com.github.javydreamercsw.management.service.tournament.TournamentFormat.RenderMode;
import com.github.javydreamercsw.management.ui.component.TournamentBracketModel.MatchModel;
import com.github.javydreamercsw.management.ui.component.TournamentBracketModel.MatchModel.ExtraEntrant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TournamentBracketComponent extends HorizontalLayout {

  /**
   * Campaign convenience constructor — wraps {@link TournamentDTO} in a {@link
   * TournamentDTOAdapter}.
   */
  public TournamentBracketComponent(final TournamentDTO tournament) {
    this(new TournamentDTOAdapter(tournament));
  }

  public TournamentBracketComponent(final TournamentBracketModel model) {
    addClassName("tournament-bracket");
    setSpacing(false);
    setAlignItems(Alignment.CENTER);
    setWidthFull();
    getStyle().set("overflow-x", "auto");

    if (model.getRenderMode() == RenderMode.ROUND_ROBIN_GRID) {
      buildRoundRobinGrid(model);
    } else {
      buildTree(model);
    }
  }

  // ── Tree (Single Elimination) ─────────────────────────────────────────────

  /** Zoom wrapper — the round columns' parent; scaled client-side to fit the container. */
  private Div zoomFrame;

  private Div zoomCanvas;

  private void buildTree(TournamentBracketModel model) {
    // Prefer the format's FULL bracket projection: every round renders — including lazily
    // generated future ones as "Winner of Match N" placeholder slots — instead of only the
    // rounds persisted so far.
    model
        .getProjection()
        .ifPresentOrElse(
            projection -> buildProjectedTree(model, projection), () -> buildPersistedTree(model));
    // Zoom applies to the canvas wrapper — only projected brackets (which route their columns
    // through the canvas) get the fit/100% controls; the DTO path stays plain.
    if (zoomCanvas != null) {
      addZoomControls();
    }
  }

  /** Lazy zoom wrapper: the frame constrains layout size, the canvas holds the scaled content. */
  private Div canvas() {
    if (zoomCanvas == null) {
      zoomFrame = new Div();
      zoomFrame.addClassName("bracket-frame");
      zoomFrame.getStyle().set("width", "100%");
      zoomFrame.getStyle().set("position", "relative");
      zoomCanvas = new Div();
      zoomCanvas.addClassName("bracket-canvas");
      zoomCanvas.getStyle().set("display", "flex");
      // Columns spread across the FULL width; each stretches to the canvas height (the tallest
      // column) so its matches can distribute vertically — the bracket uses the whole area
      // instead of huddling in the top-left corner.
      zoomCanvas.getStyle().set("align-items", "stretch");
      zoomCanvas.getStyle().set("justify-content", "space-evenly");
      zoomCanvas.getStyle().set("width", "100%");
      zoomCanvas.getStyle().set("transform-origin", "top left");
      zoomFrame.add(zoomCanvas);
      add(zoomFrame);
    }
    return zoomCanvas;
  }

  /**
   * Fit-to-width zoom (desktop) with a phone fallback. On viewports ≥ 700px the bracket scales down
   * uniformly to use the full container width ("Fit"); on phones the canvas stays at 100% and
   * scrolls horizontally — shrinking a 6-qualifier bracket to a 400px screen would make the cards
   * unreadable. "100%" restores natural size with scrolling on desktop too.
   */
  private void addZoomControls() {
    Span fitBtn = new Span("Fit");
    fitBtn.addClassNames(
        LumoUtility.FontSize.XSMALL,
        LumoUtility.TextColor.SECONDARY,
        LumoUtility.BorderRadius.SMALL);
    fitBtn.getStyle().set("cursor", "pointer");
    fitBtn.getStyle().set("padding", "2px 8px");
    fitBtn.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
    fitBtn.getElement().setAttribute("role", "button");
    fitBtn.getElement().setAttribute("aria-label", "Fit bracket to available width");

    Span fullBtn = new Span("100%");
    fullBtn.addClassNames(
        LumoUtility.FontSize.XSMALL,
        LumoUtility.TextColor.SECONDARY,
        LumoUtility.BorderRadius.SMALL);
    fullBtn.getStyle().set("cursor", "pointer");
    fullBtn.getStyle().set("padding", "2px 8px");
    fullBtn.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
    fullBtn.getElement().setAttribute("role", "button");
    fullBtn.getElement().setAttribute("aria-label", "Show bracket at natural size");

    Div toolbar = new Div(fitBtn, fullBtn);
    toolbar.getStyle().set("display", "flex");
    toolbar.getStyle().set("gap", "6px");
    toolbar.getStyle().set("justify-content", "flex-end");
    toolbar.addClassName("bracket-zoom-toolbar");

    fitBtn.getElement().addEventListener("click", e -> applyZoom("fit"));
    fullBtn.getElement().addEventListener("click", e -> applyZoom("full"));
    add(toolbar);
    // Initial state: auto-fit on desktop, natural size on phones (the client JS applies the
    // viewport check — a sub-700px viewport never scales). Container resizes re-fit so the
    // bracket keeps using the full width when panels open/close or the window moves screens.
    applyZoom("fit");
    zoomCanvas.getElement().executeJs(ZOOM_OBSERVER_SCRIPT);
  }

  /**
   * Registers the window resize re-fit. Must be an IIFE with {@code this} = the canvas element — a
   * bare arrow expression would evaluate to a function object the browser never calls. There is NO
   * width-change guard: ResizeObserver fires once on observe() (after layout), which doubles as the
   * deferred initial fit — the construct-time applyZoom runs before layout and no-ops.
   */
  static final String ZOOM_OBSERVER_SCRIPT =
      """
      ((el) => {
        if (window.__bracketFitObserver) return;
        const fit = () => {
          const frame = el.parentElement;
          if (!frame) return;
          el.style.transform = 'none';
          if (window.innerWidth < 700) {
            frame.style.height = 'auto';
            return;
          }
          const natural = el.scrollWidth;
          const available = frame.clientWidth;
          if (!natural || !available || available >= natural) {
            frame.style.height = 'auto';
            return;
          }
          const scale = available / natural;
          el.style.transform = 'scale(' + scale + ')';
          frame.style.height = el.scrollHeight * scale + 'px';
        };
        const observer = new ResizeObserver(() => fit());
        window.__bracketFitObserver = { observe: (target) => observer.observe(target) };
        // The initial observe() firing runs fit() unconditionally — the deferred first fit
        // (the construct-time run happened before layout and no-oped).
        window.__bracketFitObserver.observe(document.documentElement);
        window.__bracketFitObserver.observe(el);
        window.__bracketFitRefit = () => fit();
      })(this)
      """;

  /**
   * The Fit/100% zoom. Mode arrives as {@code $0} (Flow injects parameters positionally) — a bare
   * parameter name would be a ReferenceError killing the whole script; a bare arrow expression
   * would never fire. Both shipped once; TournamentBracketZoomTest guards them.
   */
  static final String ZOOM_SCRIPT =
      """
      ((el, mode) => {
        // Phone guard: sub-700px viewports stay at natural size with horizontal scroll —
        // shrinking a 6-qualifier bracket to a 400px screen makes cards unreadable.
        if (mode !== '100%' && window.innerWidth < 700) {
          el.style.transform = 'none';
          const f = el.parentElement;
          if (f) f.style.height = 'auto';
          return;
        }
        const frame = el.parentElement;
        if (mode === '100%') {
          el.style.transform = 'none';
          frame.style.height = 'auto';
          return;
        }
        // Reset first so measurement reflects the natural layout.
        el.style.transform = 'none';
        const natural = el.scrollWidth;
        const available = frame.clientWidth;
        if (!natural || !available || available >= natural) {
          el.style.transform = 'none';
          frame.style.height = 'auto';
          return;
        }
        const scale = available / natural;
        el.style.transform = 'scale(' + scale + ')';
        // A transformed child keeps its layout box — shrink the frame to the scaled
        // visual height so the page doesn't reserve dead space under the bracket.
        frame.style.height = el.scrollHeight * scale + 'px';
      })(this, $0)
      """;

  /** Applies a zoom mode: "fit" scales to container width (desktop), "100%" resets. */
  private void applyZoom(String mode) {
    if (zoomCanvas == null) {
      return;
    }
    zoomCanvas.getElement().executeJs(ZOOM_SCRIPT, mode);
  }

  /** Projected rendering: one column per projected round, placeholders for undecided slots. */
  private void buildProjectedTree(
      TournamentBracketModel model, TournamentFormat.BracketProjection projection) {
    Map<Integer, List<TournamentFormat.ProjectedMatch>> byRound =
        projection.matches().stream()
            .collect(
                Collectors.groupingBy(
                    TournamentFormat.ProjectedMatch::roundNumber,
                    LinkedHashMap::new,
                    Collectors.toList()));

    int roundCount = projection.roundNames().size();
    for (int round = 1; round < roundCount + 1; round++) {
      String title =
          round <= projection.roundNames().size()
              ? projection.roundNames().get(round - 1)
              : model.getRoundName(round);
      List<TournamentFormat.ProjectedMatch> projected = byRound.getOrDefault(round, List.of());
      addProjectedRound(model, round, title != null ? title : "Round " + round, projected);
    }
    addWinner(model, roundCount);
    addConnectorOverlay(projection);
  }

  /**
   * Client-side SVG overlay connecting source matches to the matches that consume their winners.
   * Server-side layout can't know card heights (a 6-entrant qualifier is ~3× taller than the
   * final), so a small script measures the rendered DOM — each card is tagged {@code
   * data-match-number}, each slot line that consumes a winner carries {@code bracket-source-<n>} —
   * and draws one elbow path per edge into an absolutely positioned SVG. A ResizeObserver redraws
   * on size changes. Edges come straight from the projection's {@code sourceMatchNumber} values;
   * nothing is guessed from layout.
   */
  private void addConnectorOverlay(TournamentFormat.BracketProjection projection) {
    List<int[]> edges = new ArrayList<>();
    for (TournamentFormat.ProjectedMatch match : projection.matches()) {
      for (TournamentFormat.ProjectedSlot slot : match.slots()) {
        if (slot.sourceMatchNumber() != null) {
          edges.add(new int[] {slot.sourceMatchNumber(), match.matchNumber()});
        }
      }
    }
    if (edges.isEmpty()) {
      return;
    }

    StringBuilder edgeList = new StringBuilder();
    for (int[] edge : edges) {
      if (edgeList.length() > 0) {
        edgeList.append(',');
      }
      edgeList.append(edge[0]).append(':').append(edge[1]);
    }

    // The zoom canvas is the positioning context; the host overlays it edge-to-edge so the
    // canvasBox-relative path coordinates land inside the host. Attaching to the canvas (not
    // the outer component) keeps the lines INSIDE the scaled content — they zoom with the cards.
    Div overlayHost = new Div();
    overlayHost.addClassNames("bracket-connector-host");
    overlayHost.getStyle().set("position", "absolute");
    overlayHost.getStyle().set("top", "0");
    overlayHost.getStyle().set("left", "0");
    overlayHost.getStyle().set("width", "100%");
    overlayHost.getStyle().set("height", "100%");
    overlayHost.getStyle().set("pointer-events", "none");

    overlayHost.getElement().executeJs(CONNECTOR_SCRIPT);
    overlayHost.getElement().setAttribute("data-bracket-edges", edgeList.toString());
    canvas().add(overlayHost);
  }

  /**
   * Defines {@code window.__drawBracketConnectors} (reads {@code data-bracket-edges} off the host,
   * measures card boxes, draws elbow paths), registers the resize redraw, and CALLS the draw
   * function — a script that only defines the function is the dead-code failure mode.
   */
  static final String CONNECTOR_SCRIPT =
      """
      window.__drawBracketConnectors = (host) => {
        const root = host.parentElement;
        if (!root) return;
        const edges = host.getAttribute('data-bracket-edges');
        if (!edges) return;
        const old = host.querySelector('svg.bracket-connectors');
        if (old) old.remove();
        const rootBox = root.getBoundingClientRect();
        if (rootBox.width === 0 || rootBox.height === 0) return;
        // The root is a scroll container: size the SVG to the scrollable content so paths
        // stay aligned when the bracket overflows horizontally.
        const w = Math.max(rootBox.width, root.scrollWidth);
        const h = Math.max(rootBox.height, root.scrollHeight);
        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.classList.add('bracket-connectors');
        svg.style.position = 'absolute';
        svg.style.left = '0';
        svg.style.top = '0';
        svg.style.width = w + 'px';
        svg.style.height = h + 'px';
        svg.style.pointerEvents = 'none';
        svg.style.opacity = '0.35';
        // --lumo-contrast flips shade/tint via light-dark() — black in light mode, white in
        // dark mode (the former non-existent contrast-color token always fell back to #000).
        const color = (getComputedStyle(root).getPropertyValue('--lumo-contrast') || '#000').trim();
        for (const pair of edges.split(',')) {
          const [src, dst] = pair.split(':').map(Number);
          const srcEl = root.querySelector('[data-match-number="' + src + '"]');
          const dstEl = root.querySelector('[data-match-number="' + dst + '"]');
          if (!srcEl || !dstEl) continue;
          const s = srcEl.getBoundingClientRect();
          const d = dstEl.getBoundingClientRect();
          const x1 = s.right - rootBox.left;
          const x2 = d.left - rootBox.left;
          const y1 = s.top - rootBox.top + s.height / 2;
          const y2 = d.top - rootBox.top + d.height / 2;
          const midX = x1 + (x2 - x1) / 2;
          const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
          path.setAttribute('d', 'M ' + x1 + ' ' + y1 + ' H ' + midX + ' V ' + y2 + ' H ' + x2);
          path.setAttribute('stroke', color);
          path.setAttribute('fill', 'none');
          path.setAttribute('stroke-width', '2.5');
          svg.appendChild(path);
        }
        host.appendChild(svg);
      };
      if (!window.__bracketResizeObserver) {
        window.__bracketResizeObserver = new ResizeObserver((entries) => {
          for (const entry of entries) {
            window.__drawBracketConnectors(entry.target);
          }
        });
      }
      window.__bracketHosts = window.__bracketHosts || new Set();
      if (!window.__bracketHosts.has(this)) {
        window.__bracketHosts.add(this);
        window.__bracketResizeObserver.observe(this);
      }
      window.__drawBracketConnectors(this);
      """;

  private void addProjectedRound(
      TournamentBracketModel model,
      int round,
      String title,
      List<TournamentFormat.ProjectedMatch> matches) {
    VerticalLayout roundCol = new VerticalLayout();
    roundCol.setPadding(false);
    roundCol.setSpacing(true);
    roundCol.setWidth("200px");
    roundCol.addClassNames(LumoUtility.Margin.Horizontal.SMALL);
    roundCol.getElement().setAttribute("data-bracket-round", String.valueOf(round));
    // Cards distribute evenly down the column (justify) so a short column's matches spread
    // vertically across the bracket's full height instead of stacking at the top.
    roundCol.getStyle().set("justify-content", "space-around");

    Span roundTitle = new Span(title);
    roundTitle.addClassNames(
        LumoUtility.FontSize.SMALL,
        LumoUtility.FontWeight.BOLD,
        LumoUtility.TextColor.SECONDARY,
        LumoUtility.TextAlignment.CENTER,
        LumoUtility.Width.FULL);
    roundCol.add(roundTitle);

    // Persisted matches at this round, in order — a booked match's card shows ITS OWN type·rule
    // (the segment's applied rule) instead of the round-level one.
    List<MatchModel> realMatches =
        model.getMatches().stream().filter(m -> m.getRound() == round).toList();

    int position = 0;
    for (TournamentFormat.ProjectedMatch match : matches) {
      MatchModel real = position < realMatches.size() ? realMatches.get(position) : null;
      roundCol.add(createProjectedMatchCard(model, match, real));
      position++;
    }

    canvas().add(roundCol);
  }

  /** A projected match card: real entrants, "Winner of Match N" placeholders, match number. */
  private Div createProjectedMatchCard(
      TournamentBracketModel model, TournamentFormat.ProjectedMatch match, MatchModel realMatch) {
    Div card = new Div();
    card.addClassNames(
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.SMALL,
        LumoUtility.Background.CONTRAST_5,
        LumoUtility.Padding.XSMALL,
        LumoUtility.FontSize.XSMALL);
    card.setWidth("180px");
    // Connector lines key on this: the overlay draws from each card to the cards that consume
    // its winner (data-bracket-edges on the overlay host lists the src:dst pairs).
    card.getElement().setAttribute("data-match-number", String.valueOf(match.matchNumber()));

    // Match number chip ("Match 3") — the placeholder references ("Winner of Match 3") key to it.
    Span number = new Span("Match " + match.matchNumber());
    number.addClassNames(LumoUtility.FontSize.XXSMALL, LumoUtility.TextColor.SECONDARY);
    card.add(number);

    for (TournamentFormat.ProjectedSlot slot : match.slots()) {
      card.add(createProjectedSlotLine(slot, match));
    }
    String label = realMatch != null ? realMatch.getTypeRuleLabel() : null;
    if (label == null || label.isBlank()) {
      label = model.getRoundTypeRuleLabel(match.roundNumber());
    }
    if (label != null && !label.isBlank()) {
      Span typeRule = new Span(label);
      typeRule.addClassNames(LumoUtility.FontSize.XXSMALL, LumoUtility.TextColor.TERTIARY);
      card.add(typeRule);
    }

    return card;
  }

  /** One slot line: a known entrant or a "Winner of Match N" placeholder. */
  private Div createProjectedSlotLine(
      TournamentFormat.ProjectedSlot slot, TournamentFormat.ProjectedMatch match) {
    Div line = new Div();
    line.addClassNames(
        LumoUtility.Display.FLEX,
        LumoUtility.JustifyContent.BETWEEN,
        LumoUtility.AlignItems.CENTER);
    line.getStyle().set("min-height", "20px");

    Span nameSpan = new Span();
    if (slot.sourceMatchNumber() != null && !slot.advancing()) {
      nameSpan.setText("Winner of Match " + slot.sourceMatchNumber());
      nameSpan.addClassNames(LumoUtility.TextColor.SECONDARY);
      nameSpan.getStyle().set("font-style", "italic");
    } else if (slot.advancing()) {
      // The source match already decided — show the advancing winner instead of the placeholder.
      nameSpan.setText(slot.entrantName() != null ? slot.entrantName() : "?");
      nameSpan.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.SUCCESS);
    } else {
      nameSpan.setText(slot.entrantName() != null ? slot.entrantName() : "?");
      if (slot.entrantId() != null
          && match.decidedWinnerId() != null
          && slot.entrantId().equals(match.decidedWinnerId())) {
        nameSpan.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.SUCCESS);
      } else if (match.decidedWinnerId() != null && slot.entrantId() != null) {
        // Everyone who didn't win a DECIDED match lost — the Free-for-All bug where only the
        // slot-1 entrant rendered struck through.
        nameSpan.addClassNames(LumoUtility.TextColor.DISABLED);
        nameSpan.getStyle().set("text-decoration", "line-through");
      }
    }

    line.add(nameSpan);
    return line;
  }

  private void buildPersistedTree(TournamentBracketModel model) {
    int totalRounds = model.getTotalRounds();
    if (totalRounds <= 0) {
      totalRounds = 4;
    }

    for (int i = 1; i < totalRounds + 1; i++) {
      addTreeRound(model, i, roundLabel(model, i, totalRounds), totalRounds);
    }
    addWinner(model, totalRounds);
  }

  private void addTreeRound(
      TournamentBracketModel model, int round, String title, int totalRounds) {
    VerticalLayout roundCol = new VerticalLayout();
    roundCol.setPadding(false);
    roundCol.setSpacing(true);
    roundCol.setWidth("200px");
    roundCol.addClassNames(LumoUtility.Margin.Horizontal.SMALL);

    Span roundTitle = new Span(title);
    roundTitle.addClassNames(
        LumoUtility.FontSize.SMALL,
        LumoUtility.FontWeight.BOLD,
        LumoUtility.TextColor.SECONDARY,
        LumoUtility.TextAlignment.CENTER,
        LumoUtility.Width.FULL);
    roundCol.add(roundTitle);

    List<MatchModel> matches =
        model.getMatches().stream().filter(m -> m.getRound() == round).collect(Collectors.toList());

    for (MatchModel match : matches) {
      roundCol.add(createMatchCard(match));
      if (round < totalRounds) {
        Div spacer = new Div();
        spacer.setHeight("10px");
        roundCol.add(spacer);
      }
    }

    add(roundCol);
  }

  /**
   * The champion box renders ONLY when the tournament is actually complete: a decided match in the
   * last *rendered* round is not a champion while later rounds still generate lazily (the sandbox
   * bug — a won qualifier crowned a champion on a two-rounds-left bracket).
   */
  private void addWinner(TournamentBracketModel model, int totalRounds) {
    if (!model.isComplete()) {
      return;
    }
    MatchModel finals =
        model.getMatches().stream()
            .filter(m -> m.getRound() == totalRounds)
            .findFirst()
            .orElse(null);

    if (finals != null && finals.getWinnerId() != null) {
      VerticalLayout winnerCol = new VerticalLayout();
      winnerCol.setPadding(false);
      winnerCol.setAlignItems(Alignment.CENTER);
      winnerCol.setJustifyContentMode(JustifyContentMode.CENTER);

      Span title = new Span("CHAMPION");
      title.addClassNames(LumoUtility.TextColor.SUCCESS, LumoUtility.FontWeight.BOLD);

      String winnerName =
          finals.getWinnerId().equals(finals.getWrestler1Id())
              ? finals.getWrestler1Name()
              : finals.getWrestler2Name();

      Div winnerBox = new Div();
      winnerBox.setText(winnerName);
      winnerBox.addClassNames(
          LumoUtility.Padding.MEDIUM,
          LumoUtility.Background.PRIMARY,
          LumoUtility.TextColor.PRIMARY_CONTRAST,
          LumoUtility.BorderRadius.MEDIUM,
          LumoUtility.FontWeight.BOLD);

      winnerCol.add(title, winnerBox);
      canvas().add(winnerCol);
    }
  }

  // ── Round Robin grid ──────────────────────────────────────────────────────

  private void buildRoundRobinGrid(TournamentBracketModel model) {
    VerticalLayout grid = new VerticalLayout();
    grid.setPadding(false);
    grid.setSpacing(true);
    grid.setWidthFull();

    // Group matches by round, preserving round order
    Map<Integer, List<MatchModel>> byRound =
        model.getMatches().stream()
            .sorted(Comparator.comparingInt(MatchModel::getRound))
            .collect(
                Collectors.groupingBy(
                    MatchModel::getRound, LinkedHashMap::new, Collectors.toList()));

    for (Map.Entry<Integer, List<MatchModel>> entry : byRound.entrySet()) {
      Span roundHeader = new Span("Round " + entry.getKey());
      roundHeader.addClassNames(
          LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
      grid.add(roundHeader);

      HorizontalLayout row = new HorizontalLayout();
      row.setSpacing(true);
      row.setAlignItems(Alignment.CENTER);
      for (MatchModel match : entry.getValue()) {
        row.add(createMatchCard(match));
      }
      grid.add(row);
    }

    add(grid);
  }

  // ── Shared match card ─────────────────────────────────────────────────────

  private Div createMatchCard(final MatchModel match) {
    Div card = new Div();
    card.addClassNames(
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.SMALL,
        LumoUtility.Background.CONTRAST_5,
        LumoUtility.Padding.XSMALL,
        LumoUtility.FontSize.XSMALL);
    card.setWidth("180px");

    card.add(createWrestlerLine(match.getWrestler1Name(), match.getWrestler1Id(), match));
    card.add(createWrestlerLine(match.getWrestler2Name(), match.getWrestler2Id(), match));
    // Multi-entrant matches (ATW-oloa): qualifiers/finals beyond two wrestlers render as
    // additional lines styled by the same winner/loser rule as the classic slots.
    for (ExtraEntrant extra : match.getExtraEntrants()) {
      card.add(createWrestlerLine(extra.name(), extra.wrestlerId(), match));
    }

    // Format context (entity path): "Free-for-All · No DQ" on every qualifier card.
    String label = match.getTypeRuleLabel();
    if (label != null && !label.isBlank()) {
      Span typeRule = new Span(label);
      typeRule.addClassNames(LumoUtility.FontSize.XXSMALL, LumoUtility.TextColor.TERTIARY);
      card.add(typeRule);
    }

    if (match.isPlayerMatch()) {
      card.getStyle().set("border-color", "var(--lumo-primary-color)");
      card.getStyle().set("border-width", "2px");
    }

    return card;
  }

  private Div createWrestlerLine(final String name, final Long id, final MatchModel match) {
    Div line = new Div();
    line.addClassNames(
        LumoUtility.Display.FLEX,
        LumoUtility.JustifyContent.BETWEEN,
        LumoUtility.AlignItems.CENTER);
    line.getStyle().set("min-height", "20px");

    Span nameSpan = new Span(name != null ? name : "?");
    if (id != null && match.getWinnerId() != null && id.equals(match.getWinnerId())) {
      nameSpan.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.SUCCESS);
    } else if (match.getWinnerId() != null && id != null) {
      nameSpan.addClassNames(LumoUtility.TextColor.DISABLED);
      nameSpan.getStyle().set("text-decoration", "line-through");
    }

    line.add(nameSpan);
    return line;
  }

  /**
   * Prefer the persisted round name ("Qualifiers", "Final") — lazy generation makes positional
   * guessing wrong ("Finals" on a bracket whose only round is the qualifiers). Falls back to the
   * positional label for models without real names (campaign DTO path, which pre-computes its
   * bracket size).
   */
  private static String roundLabel(TournamentBracketModel model, int round, int totalRounds) {
    String persisted = model.getRoundName(round);
    if (persisted != null && !persisted.isBlank()) {
      return persisted;
    }
    int remaining = totalRounds - round;
    return switch (remaining) {
      case 0 -> "Finals";
      case 1 -> "Semi-Finals";
      case 2 -> "Quarter-Finals";
      default -> "Round " + round;
    };
  }
}
