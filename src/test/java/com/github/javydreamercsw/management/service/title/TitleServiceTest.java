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
package com.github.javydreamercsw.management.service.title;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.TierBoundary;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.base.image.DefaultImageService;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.service.ranking.TierBoundaryService;
import com.github.javydreamercsw.management.service.title.TitleService.ChallengeResult;
import com.github.javydreamercsw.management.service.title.TitleService.TitleStats;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.universe.UniverseSettingsService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TitleServiceTest {

  @Mock private TierBoundaryService tierBoundaryService;
  @Mock private TitleRepository titleRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private WrestlerService wrestlerService;
  @Mock private UniverseRepository universeRepository;
  @Mock private DefaultImageService imageService;
  @Mock private Clock clock;
  @Mock private ExpansionService expansionService;
  @Mock private UniverseContextService universeContextService;
  @Mock private UniverseSettingsService universeSettingsService;

  @InjectMocks private TitleService titleService;

  private Wrestler wrestler;
  private Title title;
  private Universe universe;
  private WrestlerState wrestlerState;

  @BeforeEach
  void setUp() {
    Instant fixedInstant = Instant.parse("2026-01-01T00:00:00Z");
    when(clock.instant()).thenReturn(fixedInstant);
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);

    universe = new Universe();
    universe.setId(10L);

    wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setName("Test Wrestler");
    wrestler.setGender(Gender.MALE);

    title = new Title();
    title.setId(100L);
    title.setName("World Title");
    title.setDescription("The top prize");
    title.setTier(WrestlerTier.MAIN_EVENTER);
    title.setIsActive(true);
    title.setUniverse(universe);
    title.setCreationDate(fixedInstant);

    wrestlerState = WrestlerState.builder().tier(WrestlerTier.MAIN_EVENTER).fans(200_000L).build();

    when(universeContextService.getCurrentUniverse()).thenReturn(Optional.empty());
    when(expansionService.getEnabledExpansionCodes()).thenReturn(List.of("BASE_GAME"));
    when(titleRepository.save(any(Title.class))).thenAnswer(inv -> inv.getArgument(0));
    when(wrestlerService.getOrCreateState(anyLong(), anyLong())).thenReturn(wrestlerState);
  }

  // =====================================================================
  // isWrestlerEligible
  // =====================================================================

  @Test
  void isWrestlerEligible_wrongGender_returnsFalse() {
    title.setGender(Gender.FEMALE);
    wrestler.setGender(Gender.MALE);

    boolean result = titleService.isWrestlerEligible(wrestler, title);

    assertThat(result).isFalse();
  }

  @Test
  void isWrestlerEligible_nullUniverse_returnsFalse() {
    title.setUniverse(null);
    title.setGender(null);

    boolean result = titleService.isWrestlerEligible(wrestler, title);

    assertThat(result).isFalse();
  }

  @Test
  void isWrestlerEligible_wrestlerTierTooLow_returnsFalse() {
    title.setGender(null);
    title.setTier(WrestlerTier.ICON);
    WrestlerState lowState = WrestlerState.builder().tier(WrestlerTier.ROOKIE).fans(100L).build();
    when(wrestlerService.getOrCreateState(wrestler.getId(), universe.getId())).thenReturn(lowState);

    boolean result = titleService.isWrestlerEligible(wrestler, title);

    assertThat(result).isFalse();
  }

  @Test
  void isWrestlerEligible_wrestlerTierExactMatch_returnsTrue() {
    title.setGender(null);
    title.setTier(WrestlerTier.MAIN_EVENTER);
    // wrestlerState already has MAIN_EVENTER in setUp

    boolean result = titleService.isWrestlerEligible(wrestler, title);

    assertThat(result).isTrue();
  }

  @Test
  void isWrestlerEligible_wrestlerTierHigher_returnsTrue() {
    title.setGender(null);
    title.setTier(WrestlerTier.MIDCARDER); // wrestler is MAIN_EVENTER (higher)

    boolean result = titleService.isWrestlerEligible(wrestler, title);

    assertThat(result).isTrue();
  }

  @Test
  void isWrestlerEligible_genderMatches_returnsTrue() {
    title.setGender(Gender.MALE);
    wrestler.setGender(Gender.MALE);
    title.setTier(WrestlerTier.ROOKIE); // wrestler tier is higher

    boolean result = titleService.isWrestlerEligible(wrestler, title);

    assertThat(result).isTrue();
  }

  // =====================================================================
  // titleNameExists
  // =====================================================================

  @Test
  void titleNameExists_found_returnsTrue() {
    when(titleRepository.findByName("World Title")).thenReturn(Optional.of(title));

    assertThat(titleService.titleNameExists("World Title")).isTrue();
  }

  @Test
  void titleNameExists_notFound_returnsFalse() {
    when(titleRepository.findByName("Unknown")).thenReturn(Optional.empty());

    assertThat(titleService.titleNameExists("Unknown")).isFalse();
  }

  // =====================================================================
  // createTitle
  // =====================================================================

  @Test
  void createTitle_withoutGender_savesTitle() {
    when(universeRepository.findById(10L)).thenReturn(Optional.of(universe));

    Title result =
        titleService.createTitle(
            "World Title", "Top prize", WrestlerTier.MAIN_EVENTER, ChampionshipType.SINGLE, 10L);

    assertThat(result.getName()).isEqualTo("World Title");
    assertThat(result.getDescription()).isEqualTo("Top prize");
    assertThat(result.getTier()).isEqualTo(WrestlerTier.MAIN_EVENTER);
    assertThat(result.getGender()).isNull();
    assertThat(result.getUniverse()).isEqualTo(universe);
    verify(titleRepository).save(any(Title.class));
  }

  @Test
  void createTitle_withGender_savesTitle() {
    when(universeRepository.findById(10L)).thenReturn(Optional.of(universe));

    Title result =
        titleService.createTitle(
            "Women's Title",
            "Women's prize",
            WrestlerTier.MIDCARDER,
            ChampionshipType.SINGLE,
            Gender.FEMALE,
            10L);

    assertThat(result.getName()).isEqualTo("Women's Title");
    assertThat(result.getGender()).isEqualTo(Gender.FEMALE);
    verify(titleRepository).save(any(Title.class));
  }

  @Test
  void createTitle_universeNotFound_throwsException() {
    when(universeRepository.findById(99L)).thenReturn(Optional.empty());

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                titleService.createTitle(
                    "Title", "Desc", WrestlerTier.ROOKIE, ChampionshipType.SINGLE, 99L))
        .isInstanceOf(java.util.NoSuchElementException.class);
  }

  // =====================================================================
  // getTitleById
  // =====================================================================

  @Test
  void getTitleById_found_returnsTitle() {
    when(titleRepository.findById(100L)).thenReturn(Optional.of(title));

    Optional<Title> result = titleService.getTitleById(100L);

    assertThat(result).isPresent().contains(title);
  }

  @Test
  void getTitleById_notFound_returnsEmpty() {
    when(titleRepository.findById(999L)).thenReturn(Optional.empty());

    Optional<Title> result = titleService.getTitleById(999L);

    assertThat(result).isEmpty();
  }

  // =====================================================================
  // findByName
  // =====================================================================

  @Test
  void findByName_found_returnsTitle() {
    when(titleRepository.findByName("World Title")).thenReturn(Optional.of(title));

    Optional<Title> result = titleService.findByName("World Title");

    assertThat(result).isPresent().contains(title);
  }

  @Test
  void findByName_notFound_returnsEmpty() {
    when(titleRepository.findByName("Unknown")).thenReturn(Optional.empty());

    Optional<Title> result = titleService.findByName("Unknown");

    assertThat(result).isEmpty();
  }

  // =====================================================================
  // save / saveAll / findAll / getAllTitles / getActiveTitles / getVacantTitles
  // =====================================================================

  @Test
  void save_delegatesToRepository() {
    titleService.save(title);

    verify(titleRepository).save(title);
  }

  @Test
  void saveAll_delegatesToRepository() {
    List<Title> titles = List.of(title);
    when(titleRepository.saveAll(titles)).thenReturn(titles);

    List<Title> result = titleService.saveAll(titles);

    assertThat(result).containsExactly(title);
    verify(titleRepository).saveAll(titles);
  }

  @Test
  void findAll_returnsAllTitles() {
    when(titleRepository.findAll()).thenReturn(List.of(title));

    List<Title> result = titleService.findAll();

    assertThat(result).containsExactly(title);
  }

  @Test
  void getAllTitles_returnsPage() {
    Page<Title> page = new PageImpl<>(List.of(title));
    when(titleRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
        .thenReturn(page);

    Page<Title> result = titleService.getAllTitles(PageRequest.of(0, 10));

    assertThat(result.getContent()).containsExactly(title);
  }

  @Test
  void getActiveTitles_returnsActiveTitles() {
    when(titleRepository.findByIsActiveTrue()).thenReturn(List.of(title));

    List<Title> result = titleService.getActiveTitles();

    assertThat(result).containsExactly(title);
  }

  @Test
  void getVacantTitles_onlyReturnsVacantActiveTitles() {
    Title vacantTitle = new Title();
    vacantTitle.setId(200L);
    vacantTitle.setName("Vacant Title");
    vacantTitle.setIsActive(true);
    // No champion set — isVacant() returns true

    Title champTitle = new Title();
    champTitle.setId(201L);
    champTitle.setName("Held Title");
    champTitle.setIsActive(true);
    champTitle.getCurrentChampions().add(wrestler);

    when(titleRepository.findByIsActiveTrue()).thenReturn(List.of(vacantTitle, champTitle));

    List<Title> result = titleService.getVacantTitles();

    assertThat(result).containsExactly(vacantTitle);
  }

  // =====================================================================
  // getTitlesByTier
  // =====================================================================

  @Test
  void getTitlesByTier_delegatesToRepository() {
    when(titleRepository.findByIsActiveTrueAndTier(WrestlerTier.MAIN_EVENTER))
        .thenReturn(List.of(title));

    List<Title> result = titleService.getTitlesByTier(WrestlerTier.MAIN_EVENTER);

    assertThat(result).containsExactly(title);
  }

  // =====================================================================
  // awardTitleTo
  // =====================================================================

  @Test
  void awardTitleTo_callsTitleMethodAndSaves() {
    List<Wrestler> champions = List.of(wrestler);

    titleService.awardTitleTo(title, champions);

    assertThat(title.getCurrentChampions()).contains(wrestler);
    verify(titleRepository).save(title);
  }

  // =====================================================================
  // vacateTitle
  // =====================================================================

  @Test
  void vacateTitle_titleFound_vacatesAndSaves() {
    title.getCurrentChampions().add(wrestler);
    when(titleRepository.findById(100L)).thenReturn(Optional.of(title));

    Optional<Title> result = titleService.vacateTitle(100L);

    assertThat(result).isPresent();
    assertThat(result.get().getCurrentChampions()).isEmpty();
    verify(titleRepository).save(title);
  }

  @Test
  void vacateTitle_titleNotFound_returnsEmpty() {
    when(titleRepository.findById(999L)).thenReturn(Optional.empty());

    Optional<Title> result = titleService.vacateTitle(999L);

    assertThat(result).isEmpty();
    verify(titleRepository, never()).save(any());
  }

  // =====================================================================
  // updateTitle
  // =====================================================================

  @Test
  void updateTitle_titleNotFound_returnsEmpty() {
    when(titleRepository.findById(999L)).thenReturn(Optional.empty());

    Optional<Title> result = titleService.updateTitle(999L, "New Name", "New Desc", true);

    assertThat(result).isEmpty();
  }

  @Test
  void updateTitle_nullFields_skipsUpdates() {
    String originalName = title.getName();
    when(titleRepository.findById(100L)).thenReturn(Optional.of(title));

    Optional<Title> result = titleService.updateTitle(100L, null, null, null);

    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo(originalName);
  }

  @Test
  void updateTitle_allFieldsSet_updatesTitle() {
    when(titleRepository.findById(100L)).thenReturn(Optional.of(title));

    Optional<Title> result =
        titleService.updateTitle(100L, "New Name", "New Desc", false, Gender.FEMALE);

    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("New Name");
    assertThat(result.get().getDescription()).isEqualTo("New Desc");
    assertThat(result.get().getIsActive()).isFalse();
    assertThat(result.get().getGender()).isEqualTo(Gender.FEMALE);
    verify(titleRepository).save(title);
  }

  @Test
  void updateTitle_blankName_skipsNameUpdate() {
    String originalName = title.getName();
    when(titleRepository.findById(100L)).thenReturn(Optional.of(title));

    Optional<Title> result = titleService.updateTitle(100L, "   ", "New Desc", null);

    assertThat(result.get().getName()).isEqualTo(originalName);
    assertThat(result.get().getDescription()).isEqualTo("New Desc");
  }

  // =====================================================================
  // deleteTitle
  // =====================================================================

  @Test
  void deleteTitle_titleNotFound_returnsFalse() {
    when(titleRepository.findById(999L)).thenReturn(Optional.empty());

    boolean result = titleService.deleteTitle(999L);

    assertThat(result).isFalse();
    verify(titleRepository, never()).delete((Title) any());
  }

  @Test
  void deleteTitle_activeTitle_returnsFalse() {
    title.setIsActive(true); // active → cannot delete
    when(titleRepository.findById(100L)).thenReturn(Optional.of(title));

    boolean result = titleService.deleteTitle(100L);

    assertThat(result).isFalse();
    verify(titleRepository, never()).delete((Title) any());
  }

  @Test
  void deleteTitle_inactiveButNotVacant_returnsFalse() {
    title.setIsActive(false);
    title.getCurrentChampions().add(wrestler); // not vacant
    when(titleRepository.findById(100L)).thenReturn(Optional.of(title));

    boolean result = titleService.deleteTitle(100L);

    assertThat(result).isFalse();
    verify(titleRepository, never()).delete((Title) any());
  }

  @Test
  void deleteTitle_inactiveAndVacant_deletesAndReturnsTrue() {
    title.setIsActive(false);
    // champion list is empty by default → isVacant() = true
    when(titleRepository.findById(100L)).thenReturn(Optional.of(title));

    boolean result = titleService.deleteTitle(100L);

    assertThat(result).isTrue();
    verify(titleRepository).delete((Title) title);
  }

  // =====================================================================
  // getChallengeCost
  // =====================================================================

  @Test
  void getChallengeCost_withTierBoundary_returnsFromBoundary() {
    TierBoundary boundary = new TierBoundary();
    boundary.setChallengeCost(50_000L);
    when(tierBoundaryService.findByTierAndGender(WrestlerTier.MAIN_EVENTER, Gender.MALE))
        .thenReturn(Optional.of(boundary));

    Long cost = titleService.getChallengeCost(title);

    assertThat(cost).isEqualTo(50_000L);
  }

  @Test
  void getChallengeCost_withoutTierBoundary_fallsBackToEnum() {
    when(tierBoundaryService.findByTierAndGender(any(), any())).thenReturn(Optional.empty());

    Long cost = titleService.getChallengeCost(title);

    assertThat(cost).isEqualTo(WrestlerTier.MAIN_EVENTER.getChallengeCost());
  }

  @Test
  void getChallengeCost_nullGender_usesMaleAsDefault() {
    title.setGender(null);
    TierBoundary boundary = new TierBoundary();
    boundary.setChallengeCost(75_000L);
    when(tierBoundaryService.findByTierAndGender(WrestlerTier.MAIN_EVENTER, Gender.MALE))
        .thenReturn(Optional.of(boundary));

    Long cost = titleService.getChallengeCost(title);

    assertThat(cost).isEqualTo(75_000L);
  }

  // =====================================================================
  // getContenderEntryFee
  // =====================================================================

  @Test
  void getContenderEntryFee_withTierBoundary_returnsFromBoundary() {
    TierBoundary boundary = new TierBoundary();
    boundary.setContenderEntryFee(20_000L);
    when(tierBoundaryService.findByTierAndGender(WrestlerTier.MAIN_EVENTER, Gender.MALE))
        .thenReturn(Optional.of(boundary));

    Long fee = titleService.getContenderEntryFee(title);

    assertThat(fee).isEqualTo(20_000L);
  }

  @Test
  void getContenderEntryFee_withoutTierBoundary_fallsBackToEnum() {
    when(tierBoundaryService.findByTierAndGender(any(), any())).thenReturn(Optional.empty());

    Long fee = titleService.getContenderEntryFee(title);

    assertThat(fee).isEqualTo(WrestlerTier.MAIN_EVENTER.getContenderEntryFee());
  }

  // =====================================================================
  // challengeForTitle
  // =====================================================================

  @Test
  void challengeForTitle_wrestlerNotFound_returnsFailure() {
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.empty());
    when(titleRepository.findById(100L)).thenReturn(Optional.of(title));

    ChallengeResult result = titleService.challengeForTitle(1L, 100L, 10L);

    assertThat(result.success()).isFalse();
    assertThat(result.message()).containsIgnoringCase("challenger not found");
  }

  @Test
  void challengeForTitle_titleNotFound_returnsFailure() {
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(titleRepository.findById(100L)).thenReturn(Optional.empty());

    ChallengeResult result = titleService.challengeForTitle(1L, 100L, 10L);

    assertThat(result.success()).isFalse();
    assertThat(result.message()).containsIgnoringCase("title not found");
  }

  @Test
  void challengeForTitle_inactiveTitle_returnsFailure() {
    title.setIsActive(false);
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(titleRepository.findById(100L)).thenReturn(Optional.of(title));

    ChallengeResult result = titleService.challengeForTitle(1L, 100L, 10L);

    assertThat(result.success()).isFalse();
    assertThat(result.message()).containsIgnoringCase("not active");
  }

  @Test
  void challengeForTitle_wrestlerAlreadyChampion_returnsFailure() {
    title.getCurrentChampions().add(wrestler);
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(titleRepository.findById(100L)).thenReturn(Optional.of(title));

    ChallengeResult result = titleService.challengeForTitle(1L, 100L, 10L);

    assertThat(result.success()).isFalse();
    assertThat(result.message()).containsIgnoringCase("already a champion");
  }

  @Test
  void challengeForTitle_notEligible_returnsFailure() {
    title.setTier(WrestlerTier.ICON);
    WrestlerState lowState = WrestlerState.builder().tier(WrestlerTier.ROOKIE).fans(100L).build();
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(titleRepository.findById(100L)).thenReturn(Optional.of(title));
    when(wrestlerService.getOrCreateState(wrestler.getId(), universe.getId())).thenReturn(lowState);

    ChallengeResult result = titleService.challengeForTitle(1L, 100L, universe.getId());

    assertThat(result.success()).isFalse();
    assertThat(result.message()).containsIgnoringCase("not eligible");
  }

  @Test
  void challengeForTitle_cannotAffordFee_returnsFailure() {
    WrestlerState brokeState =
        WrestlerState.builder().tier(WrestlerTier.MAIN_EVENTER).fans(0L).build();
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(titleRepository.findById(100L)).thenReturn(Optional.of(title));
    when(wrestlerService.getOrCreateState(wrestler.getId(), universe.getId()))
        .thenReturn(brokeState);
    when(tierBoundaryService.findByTierAndGender(any(), any())).thenReturn(Optional.empty());

    ChallengeResult result = titleService.challengeForTitle(1L, 100L, universe.getId());

    assertThat(result.success()).isFalse();
    assertThat(result.message()).containsIgnoringCase("cannot afford");
  }

  @Test
  void challengeForTitle_allChecksPass_addsChallengerAndReturnsSuccess() {
    wrestlerState = WrestlerState.builder().tier(WrestlerTier.MAIN_EVENTER).fans(500_000L).build();
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(titleRepository.findById(100L)).thenReturn(Optional.of(title));
    when(wrestlerService.getOrCreateState(wrestler.getId(), universe.getId()))
        .thenReturn(wrestlerState);
    when(tierBoundaryService.findByTierAndGender(any(), any())).thenReturn(Optional.empty());

    ChallengeResult result = titleService.challengeForTitle(1L, 100L, universe.getId());

    assertThat(result.success()).isTrue();
    assertThat(result.message()).containsIgnoringCase("challenge successful");
    assertThat(title.getChallengers()).contains(wrestler);
    verify(wrestlerService).spendFans(eq(1L), eq(universe.getId()), anyLong());
    verify(titleRepository).save(title);
  }

  // =====================================================================
  // addChallengerToTitle
  // =====================================================================

  @Test
  void addChallengerToTitle_titleNotFound_returnsFailure() {
    when(titleRepository.findById(100L)).thenReturn(Optional.empty());
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));

    ChallengeResult result = titleService.addChallengerToTitle(100L, 1L);

    assertThat(result.success()).isFalse();
    assertThat(result.message()).containsIgnoringCase("title not found");
  }

  @Test
  void addChallengerToTitle_wrestlerNotFound_returnsFailure() {
    when(titleRepository.findById(100L)).thenReturn(Optional.of(title));
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.empty());

    ChallengeResult result = titleService.addChallengerToTitle(100L, 1L);

    assertThat(result.success()).isFalse();
    assertThat(result.message()).containsIgnoringCase("wrestler not found");
  }

  @Test
  void addChallengerToTitle_notEligible_returnsFailure() {
    title.setTier(WrestlerTier.ICON);
    WrestlerState lowState = WrestlerState.builder().tier(WrestlerTier.ROOKIE).fans(0L).build();
    when(titleRepository.findById(100L)).thenReturn(Optional.of(title));
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(wrestlerService.getOrCreateState(wrestler.getId(), universe.getId())).thenReturn(lowState);

    ChallengeResult result = titleService.addChallengerToTitle(100L, 1L);

    assertThat(result.success()).isFalse();
    assertThat(result.message()).containsIgnoringCase("not eligible");
  }

  @Test
  void addChallengerToTitle_eligible_addsChallengerAndReturnsSuccess() {
    when(titleRepository.findById(100L)).thenReturn(Optional.of(title));
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    // wrestlerState in setUp already has MAIN_EVENTER

    ChallengeResult result = titleService.addChallengerToTitle(100L, 1L);

    assertThat(result.success()).isTrue();
    assertThat(title.getChallengers()).contains(wrestler);
    verify(titleRepository).save(title);
  }

  // =====================================================================
  // removeChallengerFromTitle
  // =====================================================================

  @Test
  void removeChallengerFromTitle_titleNotFound_returnsFailure() {
    when(titleRepository.findById(100L)).thenReturn(Optional.empty());
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));

    ChallengeResult result = titleService.removeChallengerFromTitle(100L, 1L);

    assertThat(result.success()).isFalse();
    assertThat(result.message()).containsIgnoringCase("title not found");
  }

  @Test
  void removeChallengerFromTitle_wrestlerNotFound_returnsFailure() {
    when(titleRepository.findById(100L)).thenReturn(Optional.of(title));
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.empty());

    ChallengeResult result = titleService.removeChallengerFromTitle(100L, 1L);

    assertThat(result.success()).isFalse();
    assertThat(result.message()).containsIgnoringCase("wrestler not found");
  }

  @Test
  void removeChallengerFromTitle_notAChallenger_returnsFailure() {
    when(titleRepository.findById(100L)).thenReturn(Optional.of(title));
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));

    ChallengeResult result = titleService.removeChallengerFromTitle(100L, 1L);

    assertThat(result.success()).isFalse();
    assertThat(result.message()).containsIgnoringCase("not a challenger");
  }

  @Test
  void removeChallengerFromTitle_isChallenger_removesAndReturnsSuccess() {
    title.getChallengers().add(wrestler);
    when(titleRepository.findById(100L)).thenReturn(Optional.of(title));
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));

    ChallengeResult result = titleService.removeChallengerFromTitle(100L, 1L);

    assertThat(result.success()).isTrue();
    assertThat(title.getChallengers()).doesNotContain(wrestler);
    verify(titleRepository).save(title);
  }

  // =====================================================================
  // getTitlesHeldBy
  // =====================================================================

  @Test
  void getTitlesHeldBy_wrestlerNotFound_returnsEmptyList() {
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.empty());

    List<Title> result = titleService.getTitlesHeldBy(1L);

    assertThat(result).isEmpty();
  }

  @Test
  void getTitlesHeldBy_wrestlerFound_delegatesToRepository() {
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(titleRepository.findTitlesHeldByWrestler(wrestler)).thenReturn(List.of(title));

    List<Title> result = titleService.getTitlesHeldBy(1L);

    assertThat(result).containsExactly(title);
  }

  // =====================================================================
  // getTitleStats
  // =====================================================================

  @Test
  void getTitleStats_titleNotFound_returnsNull() {
    when(titleRepository.findById(999L)).thenReturn(Optional.empty());

    TitleStats result = titleService.getTitleStats(999L);

    assertThat(result).isNull();
  }

  @Test
  void getTitleStats_titleFound_returnsCorrectStats() {
    when(titleRepository.findById(100L)).thenReturn(Optional.of(title));

    TitleStats result = titleService.getTitleStats(100L);

    assertThat(result).isNotNull();
    assertThat(result.titleName()).isEqualTo("World Title");
    assertThat(result.totalReigns()).isEqualTo(0);
    assertThat(result.currentChampionsCount()).isEqualTo(0);
  }

  // =====================================================================
  // count
  // =====================================================================

  @Test
  void count_delegatesToRepository() {
    when(titleRepository.count()).thenReturn(7L);

    assertThat(titleService.count()).isEqualTo(7L);
  }

  // =====================================================================
  // helpers
  // =====================================================================

  /**
   * Convenience wrapper to keep verify calls tidy with argument matchers that need static import.
   */
  private static <T> T eq(final T value) {
    return org.mockito.ArgumentMatchers.eq(value);
  }
}
