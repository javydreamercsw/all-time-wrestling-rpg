package com.github.javydreamercsw.management.service.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("SegmentService Tests")
class SegmentServiceTest {

  @Mock private SegmentRepository matchRepository;

  @InjectMocks private SegmentService segmentService;

  private Show testShow;
  private SegmentType testSegmentType;
  private Wrestler testWinner;
  private Segment testSegment;
  private Instant testDate;

  @BeforeEach
  void setUp() {
    testDate = Instant.now();

    testShow = new Show();
    testShow.setId(1L);
    testShow.setName("Test Show");

    testSegmentType = new SegmentType();
    testSegmentType.setName("Singles");

    testWinner = new Wrestler();
    testWinner.setId(1L);
    testWinner.setName("Test Wrestler");

    testSegment = new Segment();
    testSegment.setId(1L);
    testSegment.setShow(testShow);
    testSegment.setSegmentType(testSegmentType);
    testSegment.addParticipant(testWinner);
    testSegment.setWinners(java.util.List.of(testWinner));
    testSegment.setSegmentDate(testDate);
    testSegment.setNarration("Great segment!");
    testSegment.setIsTitleSegment(false);
    testSegment.setIsNpcGenerated(false);
    testSegment.setExternalId("notion-123");
  }

  @Test
  @DisplayName("Should create segment successfully")
  void shouldCreateSegmentSuccessfully() {
    // Given
    when(matchRepository.save(any(Segment.class))).thenReturn(testSegment);

    // When
    Segment result = segmentService.createSegment(testShow, testSegmentType, testDate, false);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getShow()).isEqualTo(testShow);
    assertThat(result.getSegmentType()).isEqualTo(testSegmentType);
    assertThat(result.getSegmentDate()).isEqualTo(testDate);
    assertThat(result.getIsTitleSegment()).isFalse();

    verify(matchRepository).save(any(Segment.class));
  }

  @Test
  @DisplayName("Should update segment successfully")
  void shouldUpdateSegmentSuccessfully() {
    // Given
    when(matchRepository.findById(1L)).thenReturn(Optional.of(testSegment));
    when(matchRepository.save(any(Segment.class))).thenReturn(testSegment);

    // When
    Segment result = segmentService.updateSegment(testSegment);

    // Then
    assertThat(result).isEqualTo(testSegment);
    verify(matchRepository).save(testSegment);
  }

  @Test
  @DisplayName("Should find segment by ID")
  void shouldFindSegmentById() {
    // Given
    when(matchRepository.findById(1L)).thenReturn(Optional.of(testSegment));

    // When
    Optional<Segment> result = segmentService.findById(1L);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(testSegment);
    verify(matchRepository).findById(1L);
  }

  @Test
  @DisplayName("Should return empty when segment not found by ID")
  void shouldReturnEmptyWhenSegmentNotFoundById() {
    // Given
    when(matchRepository.findById(999L)).thenReturn(Optional.empty());

    // When
    Optional<Segment> result = segmentService.findById(999L);

    // Then
    assertThat(result).isEmpty();
    verify(matchRepository).findById(999L);
  }

  @Test
  @DisplayName("Should get all matches with pagination")
  void shouldGetAllSegmentsWithPagination() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);
    Page<Segment> page = new PageImpl<>(Arrays.asList(testSegment));
    when(matchRepository.findAllBy(pageable)).thenReturn(page);

    // When
    Page<Segment> result = segmentService.getAllSegments(pageable);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0)).isEqualTo(testSegment);
    verify(matchRepository).findAllBy(pageable);
  }

  @Test
  @DisplayName("Should get matches by show")
  void shouldGetSegmentsByShow() {
    // Given
    List<Segment> matches = Arrays.asList(testSegment);
    when(matchRepository.findByShow(testShow)).thenReturn(matches);

    // When
    List<Segment> result = segmentService.getSegmentsByShow(testShow);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testSegment);
    verify(matchRepository).findByShow(testShow);
  }

  @Test
  @DisplayName("Should get matches by wrestler participation")
  void shouldGetSegmentesByWrestlerParticipation() {
    // Given
    List<Segment> matches = Arrays.asList(testSegment);
    when(matchRepository.findByWrestlerParticipation(testWinner)).thenReturn(matches);

    // When
    List<Segment> result = segmentService.getSegmentsByWrestlerParticipation(testWinner);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testSegment);
    verify(matchRepository).findByWrestlerParticipation(testWinner);
  }

  @Test
  @DisplayName("Should get matches between two wrestlers")
  void shouldGetSegmentesBetweenTwoWrestlers() {
    // Given
    Wrestler wrestler2 = new Wrestler();
    wrestler2.setId(2L);
    wrestler2.setName("Test Wrestler 2");

    List<Segment> matches = Collections.singletonList(testSegment);
    when(matchRepository.findSegmentsBetween(testWinner, wrestler2)).thenReturn(matches);

    // When
    List<Segment> result = segmentService.getSegmentsBetween(testWinner, wrestler2);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testSegment);
    verify(matchRepository).findSegmentsBetween(testWinner, wrestler2);
  }

  @Test
  @DisplayName("Should get NPC generated matches")
  void shouldGetNpcGeneratedSegmentes() {
    // Given
    testSegment.setIsNpcGenerated(true);
    List<Segment> matches = Arrays.asList(testSegment);
    when(matchRepository.findByIsNpcGeneratedTrue()).thenReturn(matches);

    // When
    List<Segment> result = segmentService.getNpcGeneratedSegments();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testSegment);
    verify(matchRepository).findByIsNpcGeneratedTrue();
  }

  @Test
  @DisplayName("Should get title matches")
  void shouldGetTitleSegmentes() {
    // Given
    testSegment.setIsTitleSegment(true);
    List<Segment> matches = Arrays.asList(testSegment);
    when(matchRepository.findByIsTitleSegmentTrue()).thenReturn(matches);

    // When
    List<Segment> result = segmentService.getTitleSegments();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testSegment);
    verify(matchRepository).findByIsTitleSegmentTrue();
  }

  @Test
  @DisplayName("Should get matches after specific date")
  void shouldGetSegmentesAfterSpecificDate() {
    // Given
    Instant afterDate = testDate.minusSeconds(3600);
    List<Segment> matches = Arrays.asList(testSegment);
    when(matchRepository.findBySegmentDateAfter(afterDate)).thenReturn(matches);

    // When
    List<Segment> result = segmentService.getSegmentsAfter(afterDate);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testSegment);
    verify(matchRepository).findBySegmentDateAfter(afterDate);
  }

  @Test
  @DisplayName("Should count wins by wrestler")
  void shouldCountWinsByWrestler() {
    // Given
    when(matchRepository.countWinsByWrestler(testWinner)).thenReturn(5L);

    // When
    long result = segmentService.countWinsByWrestler(testWinner);

    // Then
    assertThat(result).isEqualTo(5L);
    verify(matchRepository).countWinsByWrestler(testWinner);
  }

  @Test
  @DisplayName("Should count total matches by wrestler")
  void shouldCountTotalSegmentsByWrestler() {
    // Given
    when(matchRepository.countSegmentsByWrestler(testWinner)).thenReturn(10L);

    // When
    long result = segmentService.countSegmentsByWrestler(testWinner);

    // Then
    assertThat(result).isEqualTo(10L);
    verify(matchRepository).countSegmentsByWrestler(testWinner);
  }

  @Test
  @DisplayName("Should check if segment exists by external ID")
  void shouldCheckIfSegmentExistsByExternalId() {
    // Given
    when(matchRepository.existsByExternalId("notion-123")).thenReturn(true);

    // When
    boolean result = segmentService.existsByExternalId("notion-123");

    // Then
    assertThat(result).isTrue();
    verify(matchRepository).existsByExternalId("notion-123");
  }

  @Test
  @DisplayName("Should find segment by external ID")
  void shouldFindSegmentByExternalId() {
    // Given
    when(matchRepository.findByExternalId("notion-123")).thenReturn(Optional.of(testSegment));

    // When
    Optional<Segment> result = segmentService.findByExternalId("notion-123");

    // Then
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(testSegment);
    verify(matchRepository).findByExternalId("notion-123");
  }

  @Test
  @DisplayName("Should delete segment by ID")
  void shouldDeleteSegmentById() {
    // When
    segmentService.deleteSegment(1L);

    // Then
    verify(matchRepository).deleteById(1L);
  }
}
