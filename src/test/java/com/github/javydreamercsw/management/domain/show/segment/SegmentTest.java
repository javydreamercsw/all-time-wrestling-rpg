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
package com.github.javydreamercsw.management.domain.show.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Segment} result and narration-generated state. */
class SegmentTest {

  private Segment segmentWithParticipant() {
    Segment segment = new Segment();
    SegmentType type = new SegmentType();
    type.setName("Match");
    segment.setSegmentType(type);
    Wrestler wrestler = new Wrestler();
    wrestler.setName("Test Wrestler");
    segment.addParticipant(wrestler);
    return segment;
  }

  @Test
  void newSegmentDefaultsToWinnersResultAndUnnarrated() {
    Segment segment = new Segment();
    assertEquals(SegmentResult.WINNERS, segment.getResult());
    assertFalse(segment.isNarrationGenerated());
  }

  @Test
  void declareResultDrawClearsWinnerFlags() {
    Segment segment = segmentWithParticipant();
    segment.getParticipants().forEach(p -> p.setIsWinner(true));
    assertFalse(segment.getWinners().isEmpty());

    segment.declareResult(SegmentResult.DRAW);

    assertEquals(SegmentResult.DRAW, segment.getResult());
    assertTrue(segment.getWinners().isEmpty());
  }

  @Test
  void declareResultNoContestClearsWinnerFlags() {
    Segment segment = segmentWithParticipant();
    segment.getParticipants().forEach(p -> p.setIsWinner(true));

    segment.declareResult(SegmentResult.NO_CONTEST);

    assertEquals(SegmentResult.NO_CONTEST, segment.getResult());
    assertTrue(segment.getWinners().isEmpty());
  }

  @Test
  void declareResultWinnersLeavesWinnerFlagsAlone() {
    Segment segment = segmentWithParticipant();
    segment.getParticipants().forEach(p -> p.setIsWinner(true));

    segment.declareResult(SegmentResult.WINNERS);

    assertEquals(SegmentResult.WINNERS, segment.getResult());
    assertEquals(1, segment.getWinners().size());
  }
}
