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

    // Then
    assertThat(segmentDTO.getExternalId()).isEqualTo("notion-123");
    assertThat(segmentDTO.getName()).isEqualTo("Test Match");
    assertThat(segmentDTO.getParticipantNames()).isEqualTo(participantNames);
    assertThat(segmentDTO.getWinnerNames()).isEqualTo(winnerNames);
    assertThat(segmentDTO.getSegmentTypeName()).isEqualTo("Singles");
    assertThat(segmentDTO.getShowName()).isEqualTo("Test Show");
    assertThat(segmentDTO.getSegmentDate()).isEqualTo(now);
  }
}
