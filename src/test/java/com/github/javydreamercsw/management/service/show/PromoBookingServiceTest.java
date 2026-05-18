/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.show;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PromoBookingServiceTest {

  @Mock private SegmentRepository segmentRepository;
  @Mock private SegmentTypeRepository segmentTypeRepository;
  @Mock private RivalryService rivalryService;
  @Mock private SegmentRuleService segmentRuleService;
  @Mock private Clock clock;
  @Mock private Random random;

  @InjectMocks private PromoBookingService promoBookingService;

  private Show show;
  private Wrestler wrestler1;
  private Wrestler wrestler2;
  private Wrestler wrestler3;
  private SegmentType promoSegmentType;
  private Segment savedSegment;

  @BeforeEach
  void setUp() {
    show = new Show();
    show.setName("Monday Night Raw");

    wrestler1 = Wrestler.builder().name("Stone Cold").build();
    wrestler2 = Wrestler.builder().name("The Rock").build();
    wrestler3 = Wrestler.builder().name("Triple H").build();

    promoSegmentType = new SegmentType();
    promoSegmentType.setName("Promo");
    promoSegmentType.setDescription("Non-wrestling promo segment for storyline development");

    savedSegment = new Segment();
    savedSegment.setShow(show);
    savedSegment.setSegmentType(promoSegmentType);

    // Default clock setup
    when(clock.instant()).thenReturn(Instant.parse("2025-01-01T00:00:00Z"));
    when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

    // Default random returns deterministic values — first call returns 0.8 (group promo threshold),
    // but most tests need solo promo (< 0.7). Use 0.5 for solo by default.
    when(random.nextDouble()).thenReturn(0.5);
    when(random.nextInt(any(int.class))).thenReturn(0);

    // Default: promo segment type exists
    when(segmentTypeRepository.findByName("Promo")).thenReturn(Optional.of(promoSegmentType));

    // Default: no active rivalries
    when(rivalryService.getActiveRivalries()).thenReturn(Collections.emptyList());

    // Default: segment rules not found (no promo rule applied)
    when(segmentRuleService.findByName(any())).thenReturn(Optional.empty());

    // Default: existsByName returns false (no promo types match)
    when(segmentRuleService.existsByName(any())).thenReturn(false);

    // Default: segmentRepository.save returns the segment
    when(segmentRepository.save(any(Segment.class))).thenReturn(savedSegment);
  }

  @Test
  void bookPromosForShow_emptyWrestlerList_returnsEmptyList() {
    List<Segment> result = promoBookingService.bookPromosForShow(show, Collections.emptyList(), 3);

    assertThat(result).isEmpty();
    verify(segmentRepository, never()).save(any());
  }

  @Test
  void bookPromosForShow_maxPromosZero_returnsEmptyList() {
    List<Segment> result =
        promoBookingService.bookPromosForShow(show, List.of(wrestler1, wrestler2), 0);

    assertThat(result).isEmpty();
    verify(segmentRepository, never()).save(any());
  }

  @Test
  void bookPromosForShow_negativeMaxPromos_returnsEmptyList() {
    List<Segment> result = promoBookingService.bookPromosForShow(show, List.of(wrestler1), -1);

    assertThat(result).isEmpty();
    verify(segmentRepository, never()).save(any());
  }

  @Test
  void bookPromosForShow_noRivalries_booksCharacterPromos() {
    when(rivalryService.getActiveRivalries()).thenReturn(Collections.emptyList());
    // Force solo promo (random < 0.7)
    when(random.nextDouble()).thenReturn(0.5);
    // Solo promo type: existsByName returns true for "Solo Promo"
    when(segmentRuleService.existsByName("Solo Promo")).thenReturn(true);
    when(random.nextInt(1)).thenReturn(0);

    List<Wrestler> wrestlers = new ArrayList<>(List.of(wrestler1));
    List<Segment> result = promoBookingService.bookPromosForShow(show, wrestlers, 1);

    assertThat(result).hasSize(1);
    verify(segmentRepository).save(any(Segment.class));
  }

  @Test
  void bookPromosForShow_withActiveRivalry_booksRivalryPromo() {
    Rivalry rivalry = new Rivalry();
    rivalry.setWrestler1(wrestler1);
    rivalry.setWrestler2(wrestler2);
    rivalry.setHeat(30); // High heat

    when(rivalryService.getActiveRivalries()).thenReturn(List.of(rivalry));
    when(segmentRuleService.existsByName("Confrontation Promo")).thenReturn(true);
    when(segmentRuleService.existsByName("Contract Signing")).thenReturn(true);
    when(segmentRuleService.existsByName("Challenge Issued")).thenReturn(true);
    when(random.nextInt(3)).thenReturn(0);

    List<Wrestler> wrestlers = new ArrayList<>(List.of(wrestler1, wrestler2, wrestler3));
    List<Segment> result = promoBookingService.bookPromosForShow(show, wrestlers, 3);

    // At least one segment should be booked from the rivalry
    assertThat(result).isNotEmpty();
    verify(segmentRepository, atLeastOnce()).save(any(Segment.class));
  }

  @Test
  void bookPromosForShow_rivalryWrestlersNotAvailable_skipsRivalryPromo() {
    Rivalry rivalry = new Rivalry();
    rivalry.setWrestler1(wrestler1);
    rivalry.setWrestler2(wrestler2);
    rivalry.setHeat(30);

    when(rivalryService.getActiveRivalries()).thenReturn(List.of(rivalry));
    // Only wrestler3 is available — rivalry wrestlers are absent
    when(random.nextDouble()).thenReturn(0.5);
    when(segmentRuleService.existsByName("Solo Promo")).thenReturn(true);
    when(random.nextInt(1)).thenReturn(0);

    List<Wrestler> wrestlers = new ArrayList<>(List.of(wrestler3));
    List<Segment> result = promoBookingService.bookPromosForShow(show, wrestlers, 2);

    // Rivalry promo should be skipped; character promo for wrestler3 may be booked
    assertThat(result).hasSize(1);
  }

  @Test
  void bookPromosForShow_promoSegmentTypeNotFound_createsAndSavesFallback() {
    // Simulate promo type not in DB — fallback creation path
    when(segmentTypeRepository.findByName("Promo")).thenReturn(Optional.empty());
    SegmentType fallbackType = new SegmentType();
    fallbackType.setName("Promo");
    when(segmentTypeRepository.save(any(SegmentType.class))).thenReturn(fallbackType);
    when(random.nextDouble()).thenReturn(0.5);
    when(segmentRuleService.existsByName("Solo Promo")).thenReturn(true);
    when(random.nextInt(1)).thenReturn(0);

    List<Wrestler> wrestlers = new ArrayList<>(List.of(wrestler1));
    List<Segment> result = promoBookingService.bookPromosForShow(show, wrestlers, 1);

    assertThat(result).hasSize(1);
    verify(segmentTypeRepository).save(any(SegmentType.class));
  }

  @Test
  void bookPromosForShow_maxPromosLimitsOutput() {
    when(rivalryService.getActiveRivalries()).thenReturn(Collections.emptyList());
    when(random.nextDouble()).thenReturn(0.5); // solo promo
    when(segmentRuleService.existsByName("Solo Promo")).thenReturn(true);
    when(random.nextInt(1)).thenReturn(0);

    List<Wrestler> wrestlers = new ArrayList<>(List.of(wrestler1, wrestler2, wrestler3));
    // Request only 1 promo even though 3 wrestlers are available
    List<Segment> result = promoBookingService.bookPromosForShow(show, wrestlers, 1);

    assertThat(result).hasSize(1);
  }

  @Test
  void isPromoSegment_segmentWithPromoType_returnsTrue() {
    Segment promoSegment = new Segment();
    promoSegment.setSegmentType(promoSegmentType);

    boolean result = promoBookingService.isPromoSegment(promoSegment);

    assertThat(result).isTrue();
  }

  @Test
  void isPromoSegment_segmentWithNonPromoType_returnsFalse() {
    SegmentType matchType = new SegmentType();
    matchType.setName("Match");

    Segment matchSegment = new Segment();
    matchSegment.setSegmentType(matchType);

    boolean result = promoBookingService.isPromoSegment(matchSegment);

    assertThat(result).isFalse();
  }

  @Test
  void isPromoSegment_segmentWithNullType_returnsFalse() {
    Segment segment = new Segment();
    segment.setSegmentType(null);

    boolean result = promoBookingService.isPromoSegment(segment);

    assertThat(result).isFalse();
  }

  @Test
  void getPromosForShow_noSegments_returnsEmptyList() {
    when(segmentRepository.findByShow(show)).thenReturn(Collections.emptyList());

    List<Segment> result = promoBookingService.getPromosForShow(show);

    assertThat(result).isEmpty();
    verify(segmentRepository).findByShow(show);
  }

  @Test
  void getPromosForShow_mixedSegments_returnsOnlyPromos() {
    SegmentType matchType = new SegmentType();
    matchType.setName("Match");

    Segment promoSegment = new Segment();
    promoSegment.setSegmentType(promoSegmentType);

    Segment matchSegment = new Segment();
    matchSegment.setSegmentType(matchType);

    when(segmentRepository.findByShow(show)).thenReturn(List.of(promoSegment, matchSegment));

    List<Segment> result = promoBookingService.getPromosForShow(show);

    assertThat(result).hasSize(1).contains(promoSegment);
  }

  @Test
  void bookPromosForShow_rivalryMediumHeat_booksInterviewOrChallenge() {
    Rivalry rivalry = new Rivalry();
    rivalry.setWrestler1(wrestler1);
    rivalry.setWrestler2(wrestler2);
    rivalry.setHeat(18); // Medium heat: 15 <= heat < 25

    when(rivalryService.getActiveRivalries()).thenReturn(List.of(rivalry));
    when(segmentRuleService.existsByName("Confrontation Promo")).thenReturn(true);
    when(segmentRuleService.existsByName("Interview Segment")).thenReturn(true);
    when(segmentRuleService.existsByName("Challenge Issued")).thenReturn(true);
    when(random.nextInt(3)).thenReturn(1);

    List<Wrestler> wrestlers = new ArrayList<>(List.of(wrestler1, wrestler2));
    List<Segment> result = promoBookingService.bookPromosForShow(show, wrestlers, 2);

    assertThat(result).isNotEmpty();
  }

  @Test
  void bookPromosForShow_rivalryLowHeat_booksBackstageOrInterview() {
    Rivalry rivalry = new Rivalry();
    rivalry.setWrestler1(wrestler1);
    rivalry.setWrestler2(wrestler2);
    rivalry.setHeat(5); // Low heat: < 15

    when(rivalryService.getActiveRivalries()).thenReturn(List.of(rivalry));
    when(segmentRuleService.existsByName("Interview Segment")).thenReturn(true);
    when(segmentRuleService.existsByName("Backstage Segment")).thenReturn(true);
    when(segmentRuleService.existsByName("Challenge Issued")).thenReturn(true);
    when(random.nextInt(3)).thenReturn(0);

    List<Wrestler> wrestlers = new ArrayList<>(List.of(wrestler1, wrestler2));
    List<Segment> result = promoBookingService.bookPromosForShow(show, wrestlers, 2);

    assertThat(result).isNotEmpty();
  }
}
