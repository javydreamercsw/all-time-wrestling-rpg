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
package com.github.javydreamercsw.management.ui.view.wrestler;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerStats;
import com.github.javydreamercsw.base.image.ImageResolution;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.service.account.AccountService;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.StatusCard;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.campaign.WrestlerStatus;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.relationship.RelationshipType;
import com.github.javydreamercsw.management.domain.relationship.WrestlerRelationship;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerAbilityRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.campaign.AlignmentService;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.campaign.StatusCardService;
import com.github.javydreamercsw.management.service.campaign.WrestlerStatusService;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.injury.InjuryTypeService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.ranking.RankingService;
import com.github.javydreamercsw.management.service.relationship.WrestlerRelationshipService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerStatsService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.RouteParameters;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.domain.Page;

/**
 * Drives {@link WrestlerProfileView#beforeEnter} through the full {@code updateView()} render path
 * — name/details, stats with and without a stats row, status cards, relationships, highlights,
 * injuries, and the missing-wrestler/missing-parameter reroutes.
 */
class WrestlerProfileViewUpdateTest extends AbstractViewTest {

  @Mock private WrestlerService wrestlerService;
  @Mock private WrestlerStatsService wrestlerStatsService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private TitleService titleService;
  @Mock private RankingService rankingService;
  @Mock private SegmentService segmentService;
  @Mock private MultiWrestlerFeudService multiWrestlerFeudService;
  @Mock private RivalryService rivalryService;
  @Mock private SeasonService seasonService;
  @Mock private InjuryService injuryService;
  @Mock private InjuryTypeService injuryTypeService;
  @Mock private NpcService npcService;
  @Mock private AccountService accountService;
  @Mock private CampaignService campaignService;
  @Mock private ImageStorageService imageStorageService;
  @Mock private UniverseContextService universeContextService;
  @Mock private WrestlerRelationshipService relationshipService;
  @Mock private WrestlerStatusService wrestlerStatusService;
  @Mock private StatusCardService statusCardService;
  @Mock private WrestlerStateRepository wrestlerStateRepository;
  @Mock private WrestlerAbilityRepository wrestlerAbilityRepository;
  @Mock private AlignmentService alignmentService;
  @Mock private SecurityUtils securityUtils;

  private WrestlerProfileView view;
  private Wrestler wrestler;
  private WrestlerState state;
  private Universe universe;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setup() {
    lenient().when(seasonService.getAllSeasons(any())).thenReturn(Page.empty());
    lenient().when(securityUtils.hasAnyRole(any(), any())).thenReturn(false);
    lenient().when(securityUtils.isAdmin()).thenReturn(false);
    lenient().when(securityUtils.isBooker()).thenReturn(false);

    universe = new Universe();
    universe.setId(1L);
    universe.setName("Test Universe");

    wrestler = new Wrestler();
    wrestler.setId(5L);
    wrestler.setName("Test Wrestler");

    state = new WrestlerState();
    state.setId(9L);
    state.setFans(150L);

    lenient().when(wrestlerService.findByIdWithDetails(5L)).thenReturn(Optional.of(wrestler));
    lenient().when(wrestlerService.getOrCreateState(anyLong(), anyLong())).thenReturn(state);
    lenient()
        .when(wrestlerService.resolveWrestlerImage(any()))
        .thenReturn(new ImageResolution("test-url.png", true));

    view =
        new WrestlerProfileView(
            wrestlerService,
            wrestlerStatsService,
            wrestlerRepository,
            titleService,
            rankingService,
            segmentService,
            multiWrestlerFeudService,
            rivalryService,
            seasonService,
            injuryService,
            injuryTypeService,
            npcService,
            accountService,
            campaignService,
            imageStorageService,
            universeContextService,
            relationshipService,
            wrestlerStatusService,
            statusCardService,
            wrestlerStateRepository,
            wrestlerAbilityRepository,
            alignmentService);
    // securityUtils is field-injected (@Autowired), not constructor-injected, so the
    // mock has to be set reflectively before updateView() touches it.
    try {
      var field = WrestlerProfileView.class.getDeclaredField("securityUtils");
      field.setAccessible(true);
      field.set(view, securityUtils);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new IllegalStateException("Failed to inject securityUtils mock", e);
    }
    UI.getCurrent().add(view);
  }

  private void stubCommonLookups() {
    lenient().when(universeContextService.getCurrentUniverseId()).thenReturn(1L);
    lenient().when(universeContextService.getCurrentUniverse()).thenReturn(Optional.of(universe));
    lenient()
        .when(alignmentService.getOrCreateUniverseAlignment(any(), any()))
        .thenReturn(
            WrestlerAlignment.builder()
                .wrestler(wrestler)
                .universe(universe)
                .alignmentType(AlignmentType.FACE)
                .build());
    lenient().when(wrestlerStatsService.getWrestlerStats(5L, 1L)).thenReturn(Optional.empty());
    lenient().when(relationshipService.getRelationshipsForWrestler(5L)).thenReturn(List.of());
    lenient().when(titleService.findTitlesByChampion(any(), anyLong())).thenReturn(List.of());
    lenient().when(rankingService.getWrestlerTitleHistory(5L)).thenReturn(List.of());
    lenient().when(injuryService.getAllInjuriesForWrestler(5L, 1L)).thenReturn(List.of());
    lenient().when(wrestlerAbilityRepository.findByWrestlerId(5L)).thenReturn(List.of());
    lenient().when(multiWrestlerFeudService.getActiveFeudsForWrestler(5L)).thenReturn(List.of());
    lenient().when(rivalryService.getRivalriesForWrestler(5L)).thenReturn(List.of());
    lenient()
        .when(segmentService.getSegmentsByWrestlerParticipationAndSeason(any(), any(), any()))
        .thenReturn(Page.empty());
    lenient().when(segmentService.countSegmentsByWrestlerAndSeason(any(), any())).thenReturn(0L);
  }

  private void enterView() {
    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    when(event.getRouteParameters()).thenReturn(new RouteParameters("wrestlerId", "5"));
    view.beforeEnter(event);
  }

  /** Finds a component whose subtree text contains the given snippet. */
  private boolean anyTextContains(String snippet) {
    return flatten(view).anyMatch(text -> text.contains(snippet));
  }

  private Stream<String> flatten(Component root) {
    Stream<String> own =
        root.getElement().getText() == null
            ? Stream.empty()
            : Stream.of(root.getElement().getText());
    return Stream.concat(own, root.getChildren().flatMap(this::flatten));
  }

  @Test
  @DisplayName("beforeEnter with a valid wrestler renders name and fan details")
  void beforeEnterRendersProfile() {
    stubCommonLookups();
    enterView();

    H2 name = _get(view, H2.class);
    assertEquals("Test Wrestler", name.getText());
    assertTrue(anyTextContains("Fans: 150"), "Fan count should render in the details line");
  }

  @Test
  @DisplayName("Stats present renders wins/losses and win percentage")
  void statsRenderedWhenPresent() {
    stubCommonLookups();
    WrestlerStats stats = new WrestlerStats();
    stats.setWins(8);
    stats.setLosses(2);
    stats.setTitlesHeld(1);
    when(wrestlerStatsService.getWrestlerStats(5L, 1L)).thenReturn(Optional.of(stats));

    enterView();

    assertTrue(anyTextContains("Wins: 8"), "Wins paragraph should render");
    assertTrue(anyTextContains("Losses: 2"), "Losses paragraph should render");
    assertTrue(anyTextContains("Win Percentage: 80.00%"), "80% win rate should render");
  }

  @Test
  @DisplayName("Active status cards render name, level, and effect")
  void statusCardsRender() {
    stubCommonLookups();
    StatusCard card = new StatusCard();
    card.setLevel1Name("Hot Streak");
    card.setLevel1Effect("Momentum +1");
    card.setDescription("Riding high");
    card.setPositive(true);
    WrestlerStatus status = new WrestlerStatus();
    status.setLevel(1);
    status.setStatusCard(card);
    wrestler.getStatuses().add(status);

    enterView();

    assertTrue(anyTextContains("Hot Streak (Level 1)"), "Status card name should render");
    assertTrue(anyTextContains("Effect: Momentum +1"), "Status card effect should render");
  }

  @Test
  @DisplayName("Empty statuses show the empty-state message")
  void emptyStatusesShowPlaceholder() {
    stubCommonLookups();
    enterView();
    assertTrue(anyTextContains("No active status cards."));
  }

  @Test
  @DisplayName("Missing wrestler reroutes to the list view")
  void missingWrestlerReroutes() {
    when(wrestlerService.findByIdWithDetails(5L)).thenReturn(Optional.empty());
    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    when(event.getRouteParameters()).thenReturn(new RouteParameters("wrestlerId", "5"));

    view.beforeEnter(event);

    verify(event).rerouteTo(WrestlerListView.class);
  }

  @Test
  @DisplayName("Missing route parameter reroutes to the list view")
  void missingParameterReroutes() {
    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    when(event.getRouteParameters()).thenReturn(RouteParameters.empty());

    view.beforeEnter(event);

    verify(event).rerouteTo(WrestlerListView.class);
  }

  @Test
  @DisplayName("Relationships render with type, partner, level, and storyline flag")
  void relationshipsRender() {
    stubCommonLookups();
    Wrestler partner = new Wrestler();
    partner.setId(6L);
    partner.setName("Tag Partner");
    WrestlerRelationship rel = mock(WrestlerRelationship.class);
    lenient().when(rel.getType()).thenReturn(RelationshipType.BEST_FRIEND);
    lenient().when(rel.getPartner(wrestler)).thenReturn(partner);
    lenient().when(rel.getLevel()).thenReturn(3);
    lenient().when(rel.getIsStoryline()).thenReturn(true);
    when(relationshipService.getRelationshipsForWrestler(5L)).thenReturn(List.of(rel));

    enterView();

    assertTrue(anyTextContains("Tag Partner"), "Partner name should render");
    assertTrue(anyTextContains("Storyline"), "Storyline flag should render");
  }

  @Test
  @DisplayName("Career highlights render title names when present")
  void highlightsRender() {
    stubCommonLookups();
    Title title = new Title();
    title.setName("ATW Championship");
    when(titleService.findTitlesByChampion(any(), anyLong())).thenReturn(List.of(title));

    enterView();

    assertTrue(anyTextContains("ATW Championship"), "Highlight should include the title name");
  }

  @Test
  @DisplayName("Injuries render their display strings")
  void injuriesRender() {
    stubCommonLookups();
    Injury injury = mock(Injury.class);
    lenient().when(injury.getDisplayString()).thenReturn("Knee Injury (2 weeks)");
    when(injuryService.getAllInjuriesForWrestler(5L, 1L)).thenReturn(List.of(injury));

    enterView();

    assertTrue(anyTextContains("Knee Injury"), "Injury display string should render");
  }

  @Test
  @DisplayName("Booker role surfaces the Manage Statuses button")
  void bookerGetsManageButton() {
    stubCommonLookups();
    when(securityUtils.hasAnyRole(RoleName.ADMIN, RoleName.BOOKER)).thenReturn(true);

    enterView();

    assertTrue(anyTextContains("Manage Statuses"), "Manage Statuses should appear for bookers");
  }

  @Test
  @DisplayName("Biography renders the description, or the fallback when blank")
  void biographyRenders() {
    stubCommonLookups();
    wrestler.setDescription("A legendary brawler.");
    enterView();
    assertTrue(anyTextContains("A legendary brawler."));

    // Now blank: re-enter with a description-free wrestler.
    Wrestler blank = new Wrestler();
    blank.setId(5L);
    blank.setName("Test Wrestler");
    when(wrestlerService.findByIdWithDetails(5L)).thenReturn(Optional.of(blank));
    enterView();
    assertTrue(anyTextContains("No biography available."));
  }

  @Test
  @DisplayName("Manage Statuses dialog lists active statuses and Add/Flip assigns")
  void manageStatusesDialogAddFlow() {
    stubCommonLookups();
    when(securityUtils.hasAnyRole(RoleName.ADMIN, RoleName.BOOKER)).thenReturn(true);
    enterView();

    // Open the manage dialog. The header title lives in shadow DOM, so assert
    // on the content's Add/Flip button instead.
    _get(view, Button.class, spec -> spec.withText("Manage Statuses")).click();
    Dialog dialog = _get(UI.getCurrent(), Dialog.class);
    assertTrue(dialog.isOpened(), "Manage statuses dialog should open");
    assertNotNull(
        _get(UI.getCurrent(), Button.class, spec -> spec.withText("Add/Flip")),
        "Dialog content should offer the Add/Flip action");

    // Add a status card through the combo + Add/Flip button.
    StatusCard card = new StatusCard();
    card.setKey("hot_streak");
    card.setLevel1Name("Hot Streak");
    card.setLevel2Name("On Fire");
    com.vaadin.flow.component.combobox.ComboBox<StatusCard> combo =
        _get(
            UI.getCurrent(),
            com.vaadin.flow.component.combobox.ComboBox.class,
            spec -> spec.withLabel("Add Status Card"));
    combo.setValue(card);
    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Add/Flip")).click();

    verify(wrestlerStatusService).assignStatus(5L, "hot_streak");
  }

  @Test
  @DisplayName("Remove button in Manage Statuses dialog removes the status")
  void manageStatusesDialogRemoveFlow() {
    stubCommonLookups();
    when(securityUtils.hasAnyRole(RoleName.ADMIN, RoleName.BOOKER)).thenReturn(true);
    StatusCard card = new StatusCard();
    card.setKey("hot_streak");
    card.setLevel1Name("Hot Streak");
    card.setLevel2Name("On Fire");
    WrestlerStatus status = new WrestlerStatus();
    status.setLevel(2);
    status.setStatusCard(card);
    wrestler.getStatuses().add(status);
    lenient().when(statusCardService.findAll()).thenReturn(List.of(card));
    enterView();

    _get(view, Button.class, spec -> spec.withText("Manage Statuses")).click();
    Dialog dialog = _get(UI.getCurrent(), Dialog.class);
    assertTrue(dialog.isOpened(), "Manage statuses dialog should open");
    assertTrue(
        dialogText(dialog).contains("On Fire (L2)"),
        "Active status row should render in the dialog content");

    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Remove")).click();

    verify(wrestlerStatusService).removeStatus(5L, "hot_streak");
  }

  @Test
  @DisplayName("Reset Wear & Tear confirms and calls the service")
  void resetWearAndTearConfirmed() {
    stubCommonLookups();
    when(securityUtils.isAdmin()).thenReturn(true);
    enterView();

    _get(view, Button.class, spec -> spec.withText("Reset Wear & Tear")).click();
    com.vaadin.flow.component.confirmdialog.ConfirmDialog confirm =
        _get(UI.getCurrent(), com.vaadin.flow.component.confirmdialog.ConfirmDialog.class);
    assertTrue(confirm.isOpened(), "Confirm dialog should open");
    fireConfirmEvent(confirm);

    verify(wrestlerService).resetWearAndTear(5L, 1L);
  }

  @Test
  @DisplayName("State fans render via details and condition paragraph exists")
  void conditionParagraphRenders() {
    stubCommonLookups();
    state.setPhysicalCondition(45); // below the 50 threshold
    enterView();

    assertTrue(anyTextContains("Physical Condition"), "Condition meter should render");
  }

  /** Fires ConfirmDialog's confirm action via reflection (fireEvent is protected). */
  @SuppressWarnings("unchecked")
  private static void fireConfirmEvent(
      com.vaadin.flow.component.confirmdialog.ConfirmDialog dialog) {
    try {
      var event =
          new com.vaadin.flow.component.confirmdialog.ConfirmDialog.ConfirmEvent(dialog, true);
      var fireEvent =
          com.vaadin.flow.component.Component.class.getDeclaredMethod(
              "fireEvent", com.vaadin.flow.component.ComponentEvent.class);
      fireEvent.setAccessible(true);
      fireEvent.invoke(dialog, event);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to fire confirm event", e);
    }
  }

  private String dialogText(Dialog dialog) {
    StringBuilder sb = new StringBuilder();
    collectText(dialog, sb);
    return sb.toString();
  }

  private void collectText(Component c, StringBuilder sb) {
    String own = c.getElement().getText();
    if (own != null && !own.isBlank()) {
      sb.append(own).append('\n');
    }
    c.getChildren().forEach(child -> collectText(child, sb));
  }
}
