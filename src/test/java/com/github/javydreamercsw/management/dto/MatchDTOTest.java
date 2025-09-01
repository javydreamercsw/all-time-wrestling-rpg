package com.github.javydreamercsw.management.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
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
  @DisplayName("Should validate DTO with all required fields")
  void shouldValidateDtoWithAllRequiredFields() {
    // Given
    matchDTO.setName("Test Match");
    matchDTO.setParticipants(Arrays.asList("Wrestler 1", "Wrestler 2"));
    matchDTO.setMatchType("Singles");
    matchDTO.setShow("Test Show");

    // When
    boolean isValid = matchDTO.isValid();

    // Then
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Should invalidate DTO with missing name")
  void shouldInvalidateDtoWithMissingName() {
    // Given
    matchDTO.setName(null);
    matchDTO.setParticipants(Arrays.asList("Wrestler 1", "Wrestler 2"));
    matchDTO.setMatchType("Singles");
    matchDTO.setShow("Test Show");

    // When
    boolean isValid = matchDTO.isValid();

    // Then
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("Should invalidate DTO with empty name")
  void shouldInvalidateDtoWithEmptyName() {
    // Given
    matchDTO.setName("   ");
    matchDTO.setParticipants(Arrays.asList("Wrestler 1", "Wrestler 2"));
    matchDTO.setMatchType("Singles");
    matchDTO.setShow("Test Show");

    // When
    boolean isValid = matchDTO.isValid();

    // Then
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("Should invalidate DTO with missing participants")
  void shouldInvalidateDtoWithMissingParticipants() {
    // Given
    matchDTO.setName("Test Match");
    matchDTO.setParticipants(null);
    matchDTO.setMatchType("Singles");
    matchDTO.setShow("Test Show");

    // When
    boolean isValid = matchDTO.isValid();

    // Then
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("Should invalidate DTO with empty participants")
  void shouldInvalidateDtoWithEmptyParticipants() {
    // Given
    matchDTO.setName("Test Match");
    matchDTO.setParticipants(Collections.emptyList());
    matchDTO.setMatchType("Singles");
    matchDTO.setShow("Test Show");

    // When
    boolean isValid = matchDTO.isValid();

    // Then
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("Should invalidate DTO with missing match type")
  void shouldInvalidateDtoWithMissingMatchType() {
    // Given
    matchDTO.setName("Test Match");
    matchDTO.setParticipants(Arrays.asList("Wrestler 1", "Wrestler 2"));
    matchDTO.setMatchType(null);
    matchDTO.setShow("Test Show");

    // When
    boolean isValid = matchDTO.isValid();

    // Then
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("Should invalidate DTO with empty match type")
  void shouldInvalidateDtoWithEmptyMatchType() {
    // Given
    matchDTO.setName("Test Match");
    matchDTO.setParticipants(Arrays.asList("Wrestler 1", "Wrestler 2"));
    matchDTO.setMatchType("   ");
    matchDTO.setShow("Test Show");

    // When
    boolean isValid = matchDTO.isValid();

    // Then
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("Should invalidate DTO with missing show")
  void shouldInvalidateDtoWithMissingShow() {
    // Given
    matchDTO.setName("Test Match");
    matchDTO.setParticipants(Arrays.asList("Wrestler 1", "Wrestler 2"));
    matchDTO.setMatchType("Singles");
    matchDTO.setShow(null);

    // When
    boolean isValid = matchDTO.isValid();

    // Then
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("Should invalidate DTO with empty show")
  void shouldInvalidateDtoWithEmptyShow() {
    // Given
    matchDTO.setName("Test Match");
    matchDTO.setParticipants(Arrays.asList("Wrestler 1", "Wrestler 2"));
    matchDTO.setMatchType("Singles");
    matchDTO.setShow("   ");

    // When
    boolean isValid = matchDTO.isValid();

    // Then
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("Should get correct participant count")
  void shouldGetCorrectParticipantCount() {
    // Given
    matchDTO.setParticipants(Arrays.asList("Wrestler 1", "Wrestler 2", "Wrestler 3"));

    // When
    int count = matchDTO.getParticipantCount();

    // Then
    assertThat(count).isEqualTo(3);
  }

  @Test
  @DisplayName("Should return zero participant count for null participants")
  void shouldReturnZeroParticipantCountForNullParticipants() {
    // Given
    matchDTO.setParticipants(null);

    // When
    int count = matchDTO.getParticipantCount();

    // Then
    assertThat(count).isEqualTo(0);
  }

  @Test
  @DisplayName("Should identify singles match correctly")
  void shouldIdentifySinglesMatchCorrectly() {
    // Given
    matchDTO.setParticipants(Arrays.asList("Wrestler 1", "Wrestler 2"));

    // When
    boolean isSingles = matchDTO.isSinglesMatch();

    // Then
    assertThat(isSingles).isTrue();
  }

  @Test
  @DisplayName("Should not identify multi-person match as singles")
  void shouldNotIdentifyMultiPersonMatchAsSingles() {
    // Given
    matchDTO.setParticipants(Arrays.asList("Wrestler 1", "Wrestler 2", "Wrestler 3"));

    // When
    boolean isSingles = matchDTO.isSinglesMatch();

    // Then
    assertThat(isSingles).isFalse();
  }

  @Test
  @DisplayName("Should identify multi-person match correctly")
  void shouldIdentifyMultiPersonMatchCorrectly() {
    // Given
    matchDTO.setParticipants(Arrays.asList("Wrestler 1", "Wrestler 2", "Wrestler 3"));

    // When
    boolean isMultiPerson = matchDTO.isMultiPersonMatch();

    // Then
    assertThat(isMultiPerson).isTrue();
  }

  @Test
  @DisplayName("Should not identify singles match as multi-person")
  void shouldNotIdentifySinglesMatchAsMultiPerson() {
    // Given
    matchDTO.setParticipants(Arrays.asList("Wrestler 1", "Wrestler 2"));

    // When
    boolean isMultiPerson = matchDTO.isMultiPersonMatch();

    // Then
    assertThat(isMultiPerson).isFalse();
  }

  @Test
  @DisplayName("Should get participants as formatted string")
  void shouldGetParticipantsAsFormattedString() {
    // Given
    matchDTO.setParticipants(Arrays.asList("Wrestler 1", "Wrestler 2", "Wrestler 3"));

    // When
    String participantsString = matchDTO.getParticipantsAsString();

    // Then
    assertThat(participantsString).isEqualTo("Wrestler 1, Wrestler 2, Wrestler 3");
  }

  @Test
  @DisplayName("Should return default message for null participants")
  void shouldReturnDefaultMessageForNullParticipants() {
    // Given
    matchDTO.setParticipants(null);

    // When
    String participantsString = matchDTO.getParticipantsAsString();

    // Then
    assertThat(participantsString).isEqualTo("No participants");
  }

  @Test
  @DisplayName("Should return default message for empty participants")
  void shouldReturnDefaultMessageForEmptyParticipants() {
    // Given
    matchDTO.setParticipants(Collections.emptyList());

    // When
    String participantsString = matchDTO.getParticipantsAsString();

    // Then
    assertThat(participantsString).isEqualTo("No participants");
  }

  @Test
  @DisplayName("Should generate correct summary")
  void shouldGenerateCorrectSummary() {
    // Given
    matchDTO.setName("Test Match");
    matchDTO.setParticipants(Arrays.asList("Wrestler 1", "Wrestler 2"));
    matchDTO.setWinner("Wrestler 1");
    matchDTO.setShow("Test Show");

    // When
    String summary = matchDTO.getSummary();

    // Then
    assertThat(summary)
        .isEqualTo(
            "Match[name='Test Match', participants=2, winner='Wrestler 1', show='Test Show']");
  }

  @Test
  @DisplayName("Should handle null values in summary")
  void shouldHandleNullValuesInSummary() {
    // Given
    matchDTO.setName("Test Match");
    matchDTO.setParticipants(null);
    matchDTO.setWinner(null);
    matchDTO.setShow(null);

    // When
    String summary = matchDTO.getSummary();

    // Then
    assertThat(summary)
        .isEqualTo("Match[name='Test Match', participants=0, winner='null', show='null']");
  }

  @Test
  @DisplayName("Should set and get all properties correctly")
  void shouldSetAndGetAllPropertiesCorrectly() {
    // Given
    Instant now = Instant.now();
    List<String> participants = Arrays.asList("Wrestler 1", "Wrestler 2");

    // When
    matchDTO.setExternalId("notion-123");
    matchDTO.setName("Test Match");
    matchDTO.setDescription("Test Description");
    matchDTO.setParticipants(participants);
    matchDTO.setWinner("Wrestler 1");
    matchDTO.setMatchType("Singles");
    matchDTO.setShow("Test Show");
    matchDTO.setDuration(15);
    matchDTO.setRating(4);
    matchDTO.setStipulation("No DQ");
    matchDTO.setNarration("Great match!");
    matchDTO.setMatchDate(now);
    matchDTO.setIsTitleMatch(true);
    matchDTO.setIsNpcGenerated(false);
    matchDTO.setCreatedTime(now);
    matchDTO.setLastEditedTime(now);
    matchDTO.setCreatedBy("User 1");
    matchDTO.setLastEditedBy("User 2");

    // Then
    assertThat(matchDTO.getExternalId()).isEqualTo("notion-123");
    assertThat(matchDTO.getName()).isEqualTo("Test Match");
    assertThat(matchDTO.getDescription()).isEqualTo("Test Description");
    assertThat(matchDTO.getParticipants()).isEqualTo(participants);
    assertThat(matchDTO.getWinner()).isEqualTo("Wrestler 1");
    assertThat(matchDTO.getMatchType()).isEqualTo("Singles");
    assertThat(matchDTO.getShow()).isEqualTo("Test Show");
    assertThat(matchDTO.getDuration()).isEqualTo(15);
    assertThat(matchDTO.getRating()).isEqualTo(4);
    assertThat(matchDTO.getStipulation()).isEqualTo("No DQ");
    assertThat(matchDTO.getNarration()).isEqualTo("Great match!");
    assertThat(matchDTO.getMatchDate()).isEqualTo(now);
    assertThat(matchDTO.getIsTitleMatch()).isTrue();
    assertThat(matchDTO.getIsNpcGenerated()).isFalse();
    assertThat(matchDTO.getCreatedTime()).isEqualTo(now);
    assertThat(matchDTO.getLastEditedTime()).isEqualTo(now);
    assertThat(matchDTO.getCreatedBy()).isEqualTo("User 1");
    assertThat(matchDTO.getLastEditedBy()).isEqualTo("User 2");
  }
}
