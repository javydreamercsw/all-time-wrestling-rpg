package com.github.javydreamercsw.management.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MatchDTO Tests")
class MatchDTOTest {

  private SegmentDTO segmentDTO;

  @BeforeEach
  void setUp() {
    segmentDTO = new SegmentDTO();
  }

  @Test
  @DisplayName("Should set and get all properties correctly")
  void shouldSetAndGetAllPropertiesCorrectly() {
    // Given
    Instant now = Instant.now();
    List<String> participantNames = Arrays.asList("Wrestler 1", "Wrestler 2");
    List<String> winnerNames = List.of("Wrestler 1");

    // When
    segmentDTO.setExternalId("notion-123");
    segmentDTO.setName("Test Match");
    segmentDTO.setParticipantNames(participantNames);
    segmentDTO.setWinnerNames(winnerNames);
    segmentDTO.setSegmentTypeName("Singles");
    segmentDTO.setShowName("Test Show");
    segmentDTO.setSegmentDate(now);
    segmentDTO.setCreatedTime(now);
    segmentDTO.setLastEditedTime(now);

    // Then
    assertThat(segmentDTO.getExternalId()).isEqualTo("notion-123");
    assertThat(segmentDTO.getName()).isEqualTo("Test Match");
    assertThat(segmentDTO.getParticipantNames()).isEqualTo(participantNames);
    assertThat(segmentDTO.getWinnerNames()).isEqualTo(winnerNames);
    assertThat(segmentDTO.getSegmentTypeName()).isEqualTo("Singles");
    assertThat(segmentDTO.getShowName()).isEqualTo("Test Show");
    assertThat(segmentDTO.getSegmentDate()).isEqualTo(now);
    assertThat(segmentDTO.getCreatedTime()).isEqualTo(now);
    assertThat(segmentDTO.getLastEditedTime()).isEqualTo(now);
  }
}
