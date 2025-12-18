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
package com.github.javydreamercsw.management.domain.show;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.github.javydreamercsw.management.AbstractJpaTest;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class ShowRepositoryTest extends AbstractJpaTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private ShowRepository showRepository;

  @Test
  @DisplayName(
      "Should fetch Show with its relationships without causing LazyInitializationException")
  void shouldFetchShowWithRelationships() {
    // Given: Create and persist prerequisite entities
    ShowType weeklyType = new ShowType();
    weeklyType.setName("Weekly");
    weeklyType.setDescription("A weekly wrestling show.");
    entityManager.persist(weeklyType);

    ShowType pleType = new ShowType();
    pleType.setName("Premium Live Event (PLE)");
    pleType.setDescription("A major event.");
    entityManager.persist(pleType);

    Season season = new Season();
    season.setName("Season 1");
    season.setDescription("The first season.");
    entityManager.persist(season);

    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setName("Main Event");
    showTemplate.setDescription("A main event template.");
    showTemplate.setShowType(weeklyType); // Associate with the "Weekly" type
    entityManager.persist(showTemplate);

    Show show = new Show();
    show.setName("Monday Night Mayhem");
    show.setDescription("The flagship weekly show.");
    show.setShowDate(LocalDate.now());
    show.setType(weeklyType);
    show.setSeason(season);
    show.setTemplate(showTemplate);
    Long showId = entityManager.persistAndGetId(show, Long.class);

    entityManager.flush();
    // Detach all entities from the persistence context to simulate a closed session
    entityManager.clear();

    // When: Retrieve the Show from the repository again in a new context
    Show foundShow = showRepository.findById(showId).orElse(null);

    // Then: Verify that accessing the relationships does not throw an exception
    assertThat(foundShow).isNotNull();

    assertDoesNotThrow(
        () -> {
          String seasonName = foundShow.getSeason().getName();
          assertThat(seasonName).isEqualTo("Season 1");

          // Check the derived properties on the eagerly loaded template
          boolean isPle = foundShow.getTemplate().isPremiumLiveEvent();
          boolean isWeekly = foundShow.getTemplate().isWeeklyShow();
          assertThat(isPle).isFalse();
          assertThat(isWeekly).isTrue();
        },
        "Accessing related entities should not throw LazyInitializationException");
  }
}
