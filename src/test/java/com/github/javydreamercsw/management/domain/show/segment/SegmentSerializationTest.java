package com.github.javydreamercsw.management.domain.show.segment;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SegmentSerializationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testSegmentSerialization() throws Exception {
    // Given
    Faction faction = Faction.builder().build();
    faction.setName("Test Faction");

    Title title = new Title();
    title.setName("Test Title");

    TitleReign reign = new TitleReign();
    reign.setTitle(title);
    reign.setStartDate(Instant.now());

    Wrestler wrestler = Wrestler.builder().build();
    wrestler.setName("Test Wrestler");
    wrestler.setFaction(faction);
    wrestler.setStartingHealth(100);
    wrestler.setStartingStamina(100);
    wrestler.setLowHealth(20);
    wrestler.setLowStamina(20);
    wrestler.setDeckSize(50);
    wrestler.setBumps(0);
    wrestler.setTier(WrestlerTier.MIDCARDER);
    wrestler.setGender(Gender.MALE);
    wrestler.setIsPlayer(false);
    wrestler.getReigns().add(reign);

    Injury injury = new Injury();
    injury.setName("Test Injury");
    injury.setWrestler(wrestler);
    injury.setIsActive(true);
    injury.setSeverity(InjurySeverity.MINOR);
    wrestler.getInjuries().add(injury);

    Show show = new Show();
    show.setName("Test Show");

    SegmentType segmentType = new SegmentType();
    segmentType.setName("Test Type");

    SegmentRule segmentRule = new SegmentRule();
    segmentRule.setName("Test Rule");

    Segment segment = new Segment();
    segment.setShow(show);
    segment.setSegmentType(segmentType);
    segment.addParticipant(wrestler);
    segment.setWinners(java.util.List.of(wrestler));
    segment.addSegmentRule(segmentRule);
    segment.setNarration("Test narration");
    segment.setSummary("Test summary");

    // When
    String json = objectMapper.writeValueAsString(segment);

    // Then
    assertThat(json).isNotNull();
    assertThat(json).contains("Test narration");
    // Add more assertions as needed
  }
}
