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
package com.github.javydreamercsw.management.ui.view.campaign;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.Difficulty;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.segment.rule.BumpAddition;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterMode;
import com.github.javydreamercsw.management.dto.campaign.StaticEncounterDTO;
import com.github.javydreamercsw.management.service.campaign.CampaignChapterService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowFacade;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.ui.view.campaign.CampaignCardExportView.CustomKind;
import com.github.javydreamercsw.management.ui.view.campaign.CampaignCardExportView.ExportCategory;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import com.github.mvysny.kaributesting.v10.mock.MockedUI;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Instance-level tests for {@link CampaignCardExportView}: toolbar wiring, category switching and
 * every custom-content render branch, using mocked services and Karibu's mocked Vaadin UI.
 */
class CampaignCardExportViewUiTest {

  private CampaignChapterService chapterService;
  private SegmentTypeService segmentTypeService;
  private SegmentRuleService segmentRuleService;
  private NpcService npcService;
  private TitleService titleService;

  private CampaignChapterDTO chapter;

  @BeforeEach
  void setUp() {
    MockVaadin.setup(new Routes(), MockedUI::new);

    segmentTypeService = mock(SegmentTypeService.class);
    segmentRuleService = mock(SegmentRuleService.class);
    npcService = mock(NpcService.class);
    titleService = mock(TitleService.class);
    chapterService = mock(CampaignChapterService.class);

    // All service lookups empty by default; individual tests re-stub what they render.
    when(segmentTypeService.findAllByExpansionCode("CUSTOM")).thenReturn(List.of());
    when(segmentRuleService.findAllByExpansionCode("CUSTOM")).thenReturn(List.of());
    when(npcService.findAllUnfiltered()).thenReturn(List.of());
    when(titleService.findAllByExpansionCode("CUSTOM")).thenReturn(List.of());

    chapter = chapterWithTitle("Test Campaign");
    when(chapterService.getAllChapters()).thenReturn(List.of(chapter));
  }

  @AfterEach
  void tearDown() {
    MockVaadin.tearDown();
  }

  private CampaignCardExportView createView() {
    ShowFacade showFacade = mock(ShowFacade.class);
    when(showFacade.getSegmentTypeService()).thenReturn(segmentTypeService);
    when(showFacade.getSegmentRuleService()).thenReturn(segmentRuleService);
    when(showFacade.getNpcService()).thenReturn(npcService);

    CampaignCardExportView view =
        new CampaignCardExportView(chapterService, showFacade, titleService);
    UI.getCurrent().add(view);
    return view;
  }

  /** A minimal chapter that renders a cover card plus one encounter card. */
  private static CampaignChapterDTO chapterWithTitle(String title) {
    StaticEncounterDTO.StaticChoiceDTO fullMetaChoice =
        StaticEncounterDTO.StaticChoiceDTO.builder()
            .label("Challenge them")
            .text("You step into the light.")
            .vpReward(2)
            .alignmentShift(1)
            .nextPhase(com.github.javydreamercsw.management.domain.campaign.CampaignPhase.MATCH)
            .segmentRules(List.of("No DQ"))
            .build();
    StaticEncounterDTO.StaticChoiceDTO bareChoice =
        StaticEncounterDTO.StaticChoiceDTO.builder().label("Walk away").build();
    StaticEncounterDTO encounter =
        StaticEncounterDTO.builder()
            .id("enc-1")
            .title("Opening Bell")
            .narrativeText("The arena erupts.")
            .requiredWrestlerName("Docs Wrestler")
            .choices(List.of(fullMetaChoice, bareChoice))
            .build();

    return CampaignChapterDTO.builder()
        .id("test_campaign")
        .title(title)
        .introText("A test chapter intro.")
        .difficulty(Difficulty.MEDIUM)
        .allowedWrestlerNames(List.of("Docs Wrestler"))
        .requiredExpansions(List.of("CUSTOM"))
        .mode(CampaignChapterMode.STATIC_ONLY)
        .staticEncounters(new java.util.ArrayList<>(List.of(encounter)))
        .build();
  }

  @SuppressWarnings("unchecked")
  private static ComboBox<Object> categoryCombo(CampaignCardExportView view) {
    return (ComboBox<Object>) _get(view, ComboBox.class, spec -> spec.withLabel("Category"));
  }

  @SuppressWarnings("unchecked")
  private static ComboBox<Object> kindCombo(CampaignCardExportView view) {
    return (ComboBox<Object>)
        _get(view, ComboBox.class, spec -> spec.withLabel("Custom Content Kind"));
  }

  private static Div printArea(CampaignCardExportView view) {
    return (Div) _get(view, Component.class, spec -> spec.withId("campaign-card-print-area"));
  }

  // ---------------------------------------------------------------------------
  // Initial state
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Defaults to Campaign Chapters with the first chapter preselected and rendered")
  void initialState_rendersDefaultChapter() {
    CampaignCardExportView view = createView();

    ComboBox<Object> category = categoryCombo(view);
    assertThat(category.getValue()).isEqualTo(ExportCategory.CAMPAIGN_CHAPTERS);

    // Cover card + encounter card are in the print area.
    assertThat(printArea(view).getElement().getTextRecursively())
        .contains("Test Campaign")
        .contains("Opening Bell")
        .contains("Docs Wrestler")
        .contains("The arena erupts.")
        .contains("1 / 1");

    // Print is enabled because a chapter is selected.
    Button print = _get(view, Button.class, spec -> spec.withText("Print Cards"));
    assertThat(print.isEnabled()).isTrue();

    // Dynamic title reflects the selected chapter.
    assertThat(view.getPageTitle()).isEqualTo("Test Campaign — Card Export");
  }

  @Test
  @DisplayName("With no chapters available the view renders empty and print stays disabled")
  void noChapters_printDisabled() {
    when(chapterService.getAllChapters()).thenReturn(List.of());

    CampaignCardExportView view = createView();

    Button print = _get(view, Button.class, spec -> spec.withText("Print Cards"));
    assertThat(print.isEnabled()).isFalse();
    assertThat(view.getPageTitle()).isEqualTo("Card Export");
  }

  // ---------------------------------------------------------------------------
  // Category switching
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Switching to Custom Content swaps in the kind combo and clears the grid")
  void switchToCustomContent_showsKindCombo() {
    CampaignCardExportView view = createView();

    categoryCombo(view).setValue(ExportCategory.CUSTOM_CONTENT);

    ComboBox<Object> kind = kindCombo(view);
    assertThat(kind).isNotNull();
    // No kind selected yet → grid cleared, print disabled, default title.
    assertThat(printArea(view).getElement().getChildCount()).isEqualTo(0);
    assertThat(_get(view, Button.class, spec -> spec.withText("Print Cards")).isEnabled())
        .isFalse();
    assertThat(view.getPageTitle()).isEqualTo("Card Export");
  }

  @Test
  @DisplayName("Switching back to Campaign Chapters re-renders the chapter cards")
  void switchBackToChapters_rendersChapter() {
    CampaignCardExportView view = createView();

    categoryCombo(view).setValue(ExportCategory.CUSTOM_CONTENT);
    categoryCombo(view).setValue(ExportCategory.CAMPAIGN_CHAPTERS);

    assertThat(printArea(view).getElement().getTextRecursively()).contains("Test Campaign");
    assertThat(_get(view, Button.class, spec -> spec.withText("Print Cards")).isEnabled()).isTrue();
  }

  // ---------------------------------------------------------------------------
  // Custom content rendering — every kind, populated and empty
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Segment Types kind renders custom segment type cards")
  void customKind_segmentTypes_rendersCards() {
    SegmentType type = new SegmentType();
    type.setName("Gauntlet");
    type.setDescription("One after another.");
    type.setExpansionCode("CUSTOM");
    when(segmentTypeService.findAllByExpansionCode("CUSTOM")).thenReturn(List.of(type));

    CampaignCardExportView view = createView();
    categoryCombo(view).setValue(ExportCategory.CUSTOM_CONTENT);
    kindCombo(view).setValue(CustomKind.SEGMENT_TYPES);

    assertThat(printArea(view).getElement().getTextRecursively())
        .contains("SEGMENT TYPE")
        .contains("Gauntlet")
        .contains("CUSTOM");
    assertThat(_get(view, Button.class, spec -> spec.withText("Print Cards")).isEnabled()).isTrue();
    assertThat(view.getPageTitle()).isEqualTo("Segment Types — Card Export");
  }

  @Test
  @DisplayName("Segment Types kind with no custom content shows the empty state")
  void customKind_segmentTypes_empty() {
    CampaignCardExportView view = createView();
    categoryCombo(view).setValue(ExportCategory.CUSTOM_CONTENT);
    kindCombo(view).setValue(CustomKind.SEGMENT_TYPES);

    assertThat(printArea(view).getElement().getTextRecursively())
        .contains("No custom segment types defined yet.");
    assertThat(_get(view, Button.class, spec -> spec.withText("Print Cards")).isEnabled())
        .isFalse();
  }

  @Test
  @DisplayName("Segment Rules kind renders custom rule cards with badges")
  void customKind_segmentRules_rendersCards() {
    SegmentRule rule = new SegmentRule();
    rule.setName("Cage");
    rule.setDescription("Locked in.");
    rule.setExpansionCode("CUSTOM");
    rule.setNoDq(true);
    rule.setBumpAddition(BumpAddition.ALL);
    when(segmentRuleService.findAllByExpansionCode("CUSTOM")).thenReturn(List.of(rule));

    CampaignCardExportView view = createView();
    categoryCombo(view).setValue(ExportCategory.CUSTOM_CONTENT);
    kindCombo(view).setValue(CustomKind.SEGMENT_RULES);

    assertThat(printArea(view).getElement().getTextRecursively())
        .contains("SEGMENT RULE")
        .contains("Cage")
        .contains("NO DQ")
        .contains("BUMPS: ALL");
  }

  @Test
  @DisplayName("Segment Rules kind with no custom content shows the empty state")
  void customKind_segmentRules_empty() {
    CampaignCardExportView view = createView();
    categoryCombo(view).setValue(ExportCategory.CUSTOM_CONTENT);
    kindCombo(view).setValue(CustomKind.SEGMENT_RULES);

    assertThat(printArea(view).getElement().getTextRecursively())
        .contains("No custom segment rules defined yet.");
    assertThat(_get(view, Button.class, spec -> spec.withText("Print Cards")).isEnabled())
        .isFalse();
  }

  @Test
  @DisplayName("NPCs kind renders only CUSTOM-tagged NPCs, skipping other expansions")
  void customKind_npcs_rendersCustomOnly() {
    Npc custom =
        Npc.builder()
            .name("Samuel Winters")
            .npcType("Referee")
            .gender(Gender.MALE)
            .alignment(AlignmentType.FACE)
            .description("Veteran referee.")
            .imageUrl("/img/samuel.png")
            .expansionCode("CUSTOM")
            .build();
    Npc official =
        Npc.builder()
            .name("Colonel Mustafa")
            .npcType("Manager")
            .gender(Gender.MALE)
            .alignment(AlignmentType.HEEL)
            .expansionCode("RUMBLE")
            .build();
    when(npcService.findAllUnfiltered()).thenReturn(List.of(custom, official));

    CampaignCardExportView view = createView();
    categoryCombo(view).setValue(ExportCategory.CUSTOM_CONTENT);
    kindCombo(view).setValue(CustomKind.NPCS);

    String rendered = printArea(view).getElement().getTextRecursively();
    assertThat(rendered)
        .contains("Samuel Winters")
        .contains("Referee")
        .contains("Veteran referee.");
    assertThat(rendered).doesNotContain("Colonel Mustafa");
  }

  @Test
  @DisplayName("NPCs kind with no custom NPCs shows the empty state")
  void customKind_npcs_empty() {
    CampaignCardExportView view = createView();
    categoryCombo(view).setValue(ExportCategory.CUSTOM_CONTENT);
    kindCombo(view).setValue(CustomKind.NPCS);

    assertThat(printArea(view).getElement().getTextRecursively())
        .contains("No custom NPCs defined yet.");
    assertThat(_get(view, Button.class, spec -> spec.withText("Print Cards")).isEnabled())
        .isFalse();
  }

  @Test
  @DisplayName("Titles kind renders custom championship cards")
  void customKind_titles_rendersCards() {
    Title title = new Title();
    title.setName("Custom Belt");
    title.setDescription("Fan-made prize.");
    title.setTier(WrestlerTier.ICON);
    title.setEffectScript("gainMomentum(2); modifyRoll(1)");
    title.setExpansionCode("CUSTOM");
    when(titleService.findAllByExpansionCode("CUSTOM")).thenReturn(List.of(title));

    CampaignCardExportView view = createView();
    categoryCombo(view).setValue(ExportCategory.CUSTOM_CONTENT);
    kindCombo(view).setValue(CustomKind.TITLES);

    assertThat(printArea(view).getElement().getTextRecursively())
        .contains("CHAMPIONSHIP")
        .contains("Custom Belt")
        .contains("ICON")
        .contains("gainMomentum(2)")
        .contains("modifyRoll(1)");
    assertThat(view.getPageTitle()).isEqualTo("Titles — Card Export");
  }

  @Test
  @DisplayName("Titles kind with no custom titles shows the empty state")
  void customKind_titles_empty() {
    CampaignCardExportView view = createView();
    categoryCombo(view).setValue(ExportCategory.CUSTOM_CONTENT);
    kindCombo(view).setValue(CustomKind.TITLES);

    assertThat(printArea(view).getElement().getTextRecursively())
        .contains("No custom titles defined yet.");
    assertThat(_get(view, Button.class, spec -> spec.withText("Print Cards")).isEnabled())
        .isFalse();
  }

  // ---------------------------------------------------------------------------
  // Printing
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Print button with cards rendered is clickable")
  void printButton_clickableWhenCardsRendered() {
    CampaignCardExportView view = createView();

    Button print = _get(view, Button.class, spec -> spec.withText("Print Cards"));
    assertThat(print.isEnabled()).isTrue();
    print.click();
  }

  @Test
  @DisplayName("Print button stays disabled when nothing is rendered")
  void printButton_disabledWithoutSelection() {
    when(chapterService.getAllChapters()).thenReturn(List.of());

    CampaignCardExportView view = createView();

    assertThat(_get(view, Button.class, spec -> spec.withText("Print Cards")).isEnabled())
        .isFalse();
  }
}
