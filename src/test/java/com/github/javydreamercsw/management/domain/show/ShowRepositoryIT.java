/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

class ShowRepositoryIT extends ManagementIntegrationTest {

  @Autowired private ShowRepository showRepository;
  @Autowired private UniverseRepository universeRepository;
  @Autowired private ShowTypeRepository showTypeRepository;

  @Test
  @Transactional
  void testFindByUniverse() {
    Universe universe1 = new Universe();
    universe1.setName("Universe 1");
    universeRepository.save(universe1);

    Universe universe2 = new Universe();
    universe2.setName("Universe 2");
    universeRepository.save(universe2);

    ShowType type = new ShowType();
    type.setName("Test Type");
    type.setDescription("Test Description");
    showTypeRepository.save(type);

    Show show1 = new Show();
    show1.setName("Show 1");
    show1.setDescription("Description 1");
    show1.setType(type);
    show1.setUniverse(universe1);
    show1.setShowDate(LocalDate.now());
    showRepository.save(show1);

    Show show2 = new Show();
    show2.setName("Show 2");
    show2.setDescription("Description 2");
    show2.setType(type);
    show2.setUniverse(universe1);
    show2.setShowDate(LocalDate.now().plusDays(1));
    showRepository.save(show2);

    Show show3 = new Show();
    show3.setName("Show 3");
    show3.setDescription("Description 3");
    show3.setType(type);
    show3.setUniverse(universe2);
    show3.setShowDate(LocalDate.now());
    showRepository.save(show3);

    List<Show> universe1Shows = showRepository.findByUniverse(universe1);
    assertThat(universe1Shows).hasSize(2);
    assertThat(universe1Shows)
        .extracting(Show::getName)
        .containsExactlyInAnyOrder("Show 1", "Show 2");

    List<Show> universe2Shows = showRepository.findByUniverse(universe2);
    assertThat(universe2Shows).hasSize(1);
    assertThat(universe2Shows.get(0).getName()).isEqualTo("Show 3");
  }
}
