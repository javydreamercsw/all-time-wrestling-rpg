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

  private MatchDTO matchDTO;

  @BeforeEach
  void setUp() {
    matchDTO = new MatchDTO();
  }

  @Test
  @DisplayName("Should set and get all properties correctly")
  void shouldSetAndGetAllPropertiesCorrectly() {
    // Given
    Instant now = Instant.now();
    List<String> participantNames = Arrays.asList("Wrestler 1", "Wrestler 2");
    List<String> winnerNames = Arrays.asList("Wrestler 1");

    // When
    matchDTO.setExternalId("notion-123");
    matchDTO.setName("Test Match");
    matchDTO.setParticipantNames(participantNames);
    matchDTO.setWinnerNames(winnerNames);
    matchDTO.setMatchTypeName("Singles");
    matchDTO.setShowName("Test Show");
    matchDTO.setMatchDate(now);
    matchDTO.setCreatedTime(now);
    matchDTO.setLastEditedTime(now);

    // Then
    assertThat(matchDTO.getExternalId()).isEqualTo("notion-123");
    assertThat(matchDTO.getName()).isEqualTo("Test Match");
    assertThat(matchDTO.getParticipantNames()).isEqualTo(participantNames);
    assertThat(matchDTO.getWinnerNames()).isEqualTo(winnerNames);
    assertThat(matchDTO.getMatchTypeName()).isEqualTo("Singles");
    assertThat(matchDTO.getShowName()).isEqualTo("Test Show");
    assertThat(matchDTO.getMatchDate()).isEqualTo(now);
    assertThat(matchDTO.getCreatedTime()).isEqualTo(now);
    assertThat(matchDTO.getLastEditedTime()).isEqualTo(now);
  }
}
