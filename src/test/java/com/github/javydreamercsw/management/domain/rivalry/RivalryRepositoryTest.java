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
package com.github.javydreamercsw.management.domain.rivalry;

import static org.junit.jupiter.api.Assertions.*;

import com.github.javydreamercsw.TestUtils;
import com.github.javydreamercsw.management.AbstractJpaTest;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class RivalryRepositoryTest extends AbstractJpaTest {

  @Autowired private RivalryRepository rivalryRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private TestEntityManager entityManager;

  @AfterEach
  void tearDown() {
    entityManager.clear();
  }

  @Test
  void findActiveRivalriesBetween() {
    // Given
    Wrestler wrestler1 = wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Wrestler 1"));
    Wrestler wrestler2 = wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Wrestler 2"));
    Wrestler wrestler3 = wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Wrestler 3"));
    Wrestler wrestler4 = wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Wrestler 4"));

    Instant now = Instant.now();
    Instant lastMonth = now.minus(30, ChronoUnit.DAYS);

    Rivalry activeRivalry = new Rivalry();
    activeRivalry.setWrestler1(wrestler1);
    activeRivalry.setWrestler2(wrestler2);
    activeRivalry.setStartedDate(lastMonth.minus(1, ChronoUnit.DAYS));
    activeRivalry.setIsActive(true);
    rivalryRepository.save(activeRivalry);

    Rivalry activeRivalry2 = new Rivalry();
    activeRivalry2.setWrestler1(wrestler3);
    activeRivalry2.setWrestler2(wrestler4);
    activeRivalry2.setStartedDate(lastMonth.plus(1, ChronoUnit.DAYS));
    activeRivalry2.setIsActive(true);
    rivalryRepository.save(activeRivalry2);

    Rivalry endedRivalry = new Rivalry();
    endedRivalry.setWrestler1(wrestler1);
    endedRivalry.setWrestler2(wrestler3); // Different wrestlers
    endedRivalry.setStartedDate(lastMonth.minus(10, ChronoUnit.DAYS));
    endedRivalry.setEndedDate(lastMonth.minus(1, ChronoUnit.DAYS));
    endedRivalry.setIsActive(false);
    rivalryRepository.save(endedRivalry);

    Rivalry futureRivalry = new Rivalry();
    futureRivalry.setWrestler1(wrestler2);
    futureRivalry.setWrestler2(wrestler4); // Different wrestlers
    futureRivalry.setStartedDate(now.plus(1, ChronoUnit.DAYS));
    futureRivalry.setIsActive(true);
    rivalryRepository.save(futureRivalry);

    // When
    List<Rivalry> result = rivalryRepository.findActiveRivalriesBetween(lastMonth, now);

    // Then
    assertEquals(2, result.size());
  }
}
